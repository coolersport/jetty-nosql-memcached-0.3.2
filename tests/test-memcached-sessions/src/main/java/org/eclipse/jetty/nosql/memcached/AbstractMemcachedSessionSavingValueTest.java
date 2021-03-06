package org.eclipse.jetty.nosql.memcached;

//========================================================================
//Copyright (c) 2011 Intalio, Inc.
//Copyright (c) 2012 Geisha Tokyo Entertainment, Inc.
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//The Eclipse Public License is available at
//http://www.eclipse.org/legal/epl-v10.html
//The Apache License v2.0 is available at
//http://www.opensource.org/licenses/apache2.0.php
//You may elect to redistribute this code under either of these licenses.
//========================================================================

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.server.session.AbstractSessionValueSavingTest;
import org.eclipse.jetty.server.session.AbstractTestServer;
import org.junit.Test;

public abstract class AbstractMemcachedSessionSavingValueTest extends AbstractSessionValueSavingTest
{

    
    
    public AbstractTestServer createServer(int port, int max, int scavenge)
    {
//        ConnectorServer srv = null;
        try
        {
//            srv = new ConnectorServer(
//                    new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:0/jettytest"),
//                    "org.eclipse.jetty:name=rmiconnectorserver");
//            srv.start();
            
            MemcachedTestServer server = new MemcachedTestServer(port,max,scavenge,true);

//            MBeanContainer mbean = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
//           
//            server.getServer().getContainer().addEventListener(mbean);
//            server.getServer().addBean(mbean);
//
//            mbean.start();
                    
            return server;

        }
//        catch (MalformedURLException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    @Test
    public void testSessionValueSaving() throws Exception
    {
        String contextPath = "";
        String servletMapping = "/server";
        int maxInactivePeriod = 10000;
        int scavengePeriod = 20000;
        AbstractTestServer server1 = createServer(0,maxInactivePeriod,scavengePeriod);
        server1.addContext(contextPath).addServlet(TestServlet.class,servletMapping);
        server1.start();
        int port1 = server1.getPort();
        try
        {

            HttpClient client = new HttpClient();
            client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
            client.start();
            try
            {
                String[] sessionTestValue = new String[]
                { "0", "null" };

                // Perform one request to server1 to create a session
                ContentExchange exchange1 = new ContentExchange(true);
                exchange1.setMethod(HttpMethods.GET);
                exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping + "?action=init");
                client.send(exchange1);
                exchange1.waitForDone();
                assertEquals(HttpServletResponse.SC_OK,exchange1.getResponseStatus());

                String[] sessionTestResponse = exchange1.getResponseContent().split("/");
                assertTrue(Long.parseLong(sessionTestValue[0]) < Long.parseLong(sessionTestResponse[0]));

                sessionTestValue = sessionTestResponse;

                String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                assertTrue(sessionCookie != null);
                // Mangle the cookie, replacing Path with $Path, etc.
                sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=","$1\\$Path=");

                // Perform some request to server2 using the session cookie from the previous request
                // This should migrate the session from server1 to server2, and leave server1's
                // session in a very stale state, while server2 has a very fresh session.
                // We want to test that optimizations done to the saving of the shared lastAccessTime
                // do not break the correct working
                int requestInterval = 500;

                for (int i = 0; i < 10; ++i)
                {
                    ContentExchange exchange2 = new ContentExchange(true);
                    exchange2.setMethod(HttpMethods.GET);
                    exchange2.setURL("http://localhost:" + port1 + contextPath + servletMapping);
                    exchange2.getRequestFields().add("Cookie",sessionCookie);
                    client.send(exchange2);
                    exchange2.waitForDone();
                    assertEquals(HttpServletResponse.SC_OK,exchange2.getResponseStatus());

                    sessionTestResponse = exchange2.getResponseContent().split("/");

                    assertTrue(Long.parseLong(sessionTestValue[0]) < Long.parseLong(sessionTestResponse[0]));
                    assertTrue(Long.parseLong(sessionTestValue[1]) < Long.parseLong(sessionTestResponse[1]));

                    sessionTestValue = sessionTestResponse;

                    String setCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    if (setCookie != null)
                        sessionCookie = setCookie.replaceFirst("(\\W)(P|p)ath=","$1\\$Path=");

                    Thread.sleep(requestInterval);
                }

 //               Thread.sleep(320000);
            }
            finally
            {
                client.stop();
            }
        }
        finally
        {
            server1.stop();
        }
    }

    public static class TestServlet extends HttpServlet
    {
		private static final long serialVersionUID = -225790042588139275L;

		@Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
        {
            String action = request.getParameter("action");
            if ("init".equals(action))
            {
                NoSqlSession session = (NoSqlSession)request.getSession(true);
                session.setAttribute("test",System.currentTimeMillis());
                session.setAttribute("objectTest", new Pojo("foo","bar"));
                
                sendResult(session,httpServletResponse.getWriter());
                
            }
            else
            {
                NoSqlSession session = (NoSqlSession)request.getSession(false);
                if (session != null)
                {
                    long value = System.currentTimeMillis();
                    session.setAttribute("test",value);

                }

                sendResult(session,httpServletResponse.getWriter());

                Pojo p = (Pojo)session.getAttribute("objectTest");
                
                //System.out.println(p.getName() + " / " + p.getValue() );
                assertEquals(p.getName(), "foo");
                assertEquals(p.getValue(), "bar");
            }

        }

        private void sendResult(NoSqlSession session, PrintWriter writer)
        {
            if (session != null)
            {
                if (session.getVersion() == null)
                {
                    writer.print(session.getAttribute("test") + "/-1");
                }
                else
                {
                    writer.print(session.getAttribute("test") + "/" + session.getVersion());
                }
            }
            else
            {
                writer.print("0/-1");
            }
        }
        
        public static class Pojo implements Serializable
        {
			private static final long serialVersionUID = -5149700272943378810L;
			private String _name;
            private String _value;
            
            public Pojo( )
            {
                _name = "name";
                _value = "value";
            }

            public Pojo( String name, String value )
            {
                _name = name;
                _value = value;
            }
            
            public String getName()
            {
                return _name;
            }
            
            public String getValue()
            {
                return _value;
            }
        }
        
    }

 
}
