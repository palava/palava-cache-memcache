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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import de.cosmocode.commons.reflect.Reflection;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jackson based transcoder that uses the {@link MappingJsonFactory} to transcode
 * any POJO.
 *
 * @author Oliver Lorenz
 */
enum JacksonTranscoder implements Transcoder<Object> {

    INSTANCE;
    
    private static final Logger LOG = LoggerFactory.getLogger(JacksonTranscoder.class);

    private final JsonFactory factory = new MappingJsonFactory();

    @Override
    public boolean asyncDecode(CachedData data) {
        return false;
    }

    @Override
    public CachedData encode(Object value) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final DataOutputStream dataStream = new DataOutputStream(byteStream);
        
        try {
            dataStream.writeUTF(value.getClass().getName());
            factory.createJsonGenerator(dataStream, JsonEncoding.UTF8).writeObject(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(dataStream);
        }

        final byte[] bytes = byteStream.toByteArray();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing {}", new String(bytes, Charsets.UTF_8));
        }
        
        return new CachedData(0, bytes, getMaxSize());
    }

    @Override
    public Object decode(CachedData data) {
        final DataInputStream dataInputStream = new DataInputStream(
            new BufferedInputStream(new ByteArrayInputStream(data.getData())));
        
        try {
            final String className = dataInputStream.readUTF();
            final Class<?> valueType = Reflection.forName(className);
            LOG.trace("Read class {}", valueType);
            return factory.createJsonParser(dataInputStream).readValueAs(valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(dataInputStream);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
