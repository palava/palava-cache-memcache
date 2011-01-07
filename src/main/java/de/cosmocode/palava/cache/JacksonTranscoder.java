/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.io.Serializable;

/**
 * <p>
 * Jackson based transcoder that uses the {@link MappingJsonFactory} to transcode
 * any POJO.
 * </p>
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
            LOG.trace("Read class {}", valueType);
            return factory.createJsonParser(in).readValueAs(valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
