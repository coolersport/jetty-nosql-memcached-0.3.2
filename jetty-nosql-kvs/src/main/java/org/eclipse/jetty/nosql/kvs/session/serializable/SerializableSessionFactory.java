package org.eclipse.jetty.nosql.kvs.session.serializable;

import org.eclipse.jetty.nosql.kvs.session.AbstractSessionFactory;
import org.eclipse.jetty.nosql.kvs.session.ISerializableSession;
import org.eclipse.jetty.nosql.kvs.session.ISerializationTranscoder;
import org.eclipse.jetty.nosql.kvs.session.TranscoderException;

public class SerializableSessionFactory extends AbstractSessionFactory
{
    public SerializableSessionFactory()
    {
        super(new SerializableTranscoder());
    }

    @Override
    public ISerializableSession create()
    {
        return new SerializableSession();
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
    public ISerializableSession unpack(final byte[] raw, final ISerializationTranscoder tc)
    {
        ISerializableSession session = null;
        try
        {
            session = tc.decode(raw, SerializableSession.class);
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
        // do nothing
    }
}
