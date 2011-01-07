package de.cosmocode.palava.cache;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p></p>
 * <p>
 * Created on: 07.01.11
 * </p>
 *
 * @author Oliver Lorenz
 */
public enum JacksonTranscoder implements Transcoder<Object> {

    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(JacksonTranscoder.class);

    private final JsonFactory factory = new MappingJsonFactory();

    @Override
    public boolean asyncDecode(CachedData d) {
        return false;
    }

    @Override
    public CachedData encode(Object o) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream classOut = new DataOutputStream(out);
        try {
            classOut.writeUTF(o.getClass().getName());
            final JsonGenerator generator = factory.createJsonGenerator(out, JsonEncoding.UTF8);
            generator.writeObject(o);
            generator.close();
            out.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        final byte[] bytes = out.toByteArray();
        LOG.trace("Writing {}", new String(bytes));
        return new CachedData(0, bytes, getMaxSize());
    }

    @Override
    public Object decode(CachedData d) {
        final ByteArrayInputStream in = new ByteArrayInputStream(d.getData());
        final DataInputStream classIn = new DataInputStream(in);
        try {
            final Class<?> valueType = Class.forName(classIn.readUTF());
            valueType.getConstructor().setAccessible(true);
            LOG.trace("Read class {}", valueType);
            final JsonParser parser = factory.createJsonParser(in);
            return parser.readValueAs(valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
