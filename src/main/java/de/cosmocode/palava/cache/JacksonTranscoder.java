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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import de.cosmocode.commons.reflect.Reflection;

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
        
        final JsonGenerator generator;
        
        try {
            generator = factory.createJsonGenerator(byteStream, JsonEncoding.UTF8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        try {
            dataStream.writeUTF(value.getClass().getName());
            generator.writeObject(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(generator);
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
        final DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data.getData()));
        final JsonParser parser;
        
        try {
            parser = factory.createJsonParser(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        try {
            final Class<?> valueType = Reflection.forName(stream.readUTF());
            LOG.trace("Read class {}", valueType);
            return parser.readValueAs(valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(parser);
            Closeables.closeQuietly(stream);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
