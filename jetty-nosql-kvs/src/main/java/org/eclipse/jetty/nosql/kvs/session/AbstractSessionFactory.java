package org.eclipse.jetty.nosql.kvs.session;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public abstract class AbstractSessionFactory
{
    protected final static Logger log = Log.getLogger("org.eclipse.jetty.nosql.kvs.session.AbstractSessionFactory");

    private ISerializationTranscoder transcoder;

    public abstract ISerializableSession create();

    public AbstractSessionFactory(final ISerializationTranscoder t)
    {
        transcoder = t;
    }

    public void setCompressionEnabled(final boolean enabled)
    {
        transcoder.setCompressionEnabled(enabled);
    }

    public ISerializableSession create(final String sessionId)
    {
        ISerializableSession s = create();
        s.setId(sessionId);
        return s;
    }

    public ISerializableSession create(final String sessionId, final long created)
    {
        ISerializableSession s = create(sessionId);
        s.setCreationTime(created);
        return s;
    }

    public ISerializableSession create(final String sessionId, final long created, final long accessed)
    {
        ISerializableSession s = create(sessionId, created);
        s.setAccessed(accessed);
        return s;
    }

    public ISerializableSession create(final AbstractSession session)
    {
        synchronized (session)
        {
            ISerializableSession s = create(session.getId(), session.getCreationTime(), session.getAccessed());
            if (session.isValid())
            {
                for (String key : session.getNames())
                {
                    s.setAttribute(key, session.getAttribute(key));
                }
            }
            else
            {
                // we do not need to retrieve attributes of invalidated sessions since
                // they have been cleared on AbstractSession.invalidate().
                s.setValid(false);
            }
            return s;
        }
    }

    public ISerializationTranscoder getTranscoder()
    {
        return transcoder;
    }

    protected void setTranscoder(final ISerializationTranscoder transcoder)
    {
        boolean ce = this.transcoder.isCompressionEnabled();
        this.transcoder = transcoder;
        this.transcoder.setCompressionEnabled(ce);
    }

    public byte[] pack(final ISerializableSession session)
    {
        return pack(session, getTranscoder());
    }

    public abstract byte[] pack(ISerializableSession session, ISerializationTranscoder tc) throws TranscoderException;

    public ISerializableSession unpack(final byte[] raw)
    {
        return unpack(raw, getTranscoder());
    }

    public abstract ISerializableSession unpack(byte[] raw, ISerializationTranscoder tc) throws TranscoderException;

    public abstract void setClassLoader(ClassLoader cl);
}
