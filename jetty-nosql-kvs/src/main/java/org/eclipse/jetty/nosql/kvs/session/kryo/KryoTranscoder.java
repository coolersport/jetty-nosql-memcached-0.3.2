package org.eclipse.jetty.nosql.kvs.session.kryo;

import org.eclipse.jetty.nosql.kvs.session.AbstractSerializationTranscoder;
import org.eclipse.jetty.nosql.kvs.session.TranscoderException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;

public class KryoTranscoder extends AbstractSerializationTranscoder
{
    private Kryo kryo = null;

    public KryoTranscoder()
    {
        this(Thread.currentThread().getContextClassLoader());
    }

    public KryoTranscoder(final ClassLoader cl)
    {
        kryo = new Kryo();
        kryo.setRegistrationOptional(true);
        kryo.setClassLoader(cl);
    }

    public byte[] encode(final Object obj) throws TranscoderException
    {
        byte[] raw = null;
        try
        {
            ObjectBuffer buf = new ObjectBuffer(kryo);
            raw = buf.writeObject(obj);
        }
        catch (Exception error)
        {
            throw (new TranscoderException(error));
        }
        return raw;
    }

    public <T> T decode(final byte[] raw, final Class<T> klass) throws TranscoderException
    {
        T obj = null;
        try
        {
            ObjectBuffer buf = new ObjectBuffer(kryo);
            obj = buf.readObject(raw, klass);
        }
        catch (Exception error)
        {
            throw (new TranscoderException(error));
        }
        return obj;
    }
}
