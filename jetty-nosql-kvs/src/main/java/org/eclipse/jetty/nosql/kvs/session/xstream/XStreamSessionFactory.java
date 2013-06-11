package org.eclipse.jetty.nosql.kvs.session.xstream;

import org.eclipse.jetty.nosql.kvs.session.AbstractSessionFactory;
import org.eclipse.jetty.nosql.kvs.session.ISerializableSession;
import org.eclipse.jetty.nosql.kvs.session.ISerializationTranscoder;
import org.eclipse.jetty.nosql.kvs.session.TranscoderException;

public class XStreamSessionFactory extends AbstractSessionFactory
{
    public XStreamSessionFactory()
    {
        this(Thread.currentThread().getContextClassLoader());
    }

    public XStreamSessionFactory(final ClassLoader cl)
    {
        super(new XStreamTranscoder(cl));
    }

    @Override
    public ISerializableSession create()
    {
        return new XStreamSession();
    }

    @Override
    public byte[] pack(final ISerializableSession session, final ISerializationTranscoder tc)
        throws TranscoderException
    {
        byte[] raw = null;
        try
        {
            raw = tc.encode(session);
        }
        catch (Exception error)
        {
            throw (new TranscoderException(error));
        }
        return raw;
    }

    @Override
    public ISerializableSession unpack(final byte[] raw, final ISerializationTranscoder tc) throws TranscoderException
    {
        ISerializableSession session = null;
        try
        {
            session = tc.decode(raw, XStreamSession.class);
        }
        catch (Exception error)
        {
            throw (new TranscoderException(error));
        }
        return session;
    }

    @Override
    public void setClassLoader(final ClassLoader cl)
    {
        XStreamTranscoder tc = new XStreamTranscoder(cl);
        setTranscoder(tc);
    }
}
