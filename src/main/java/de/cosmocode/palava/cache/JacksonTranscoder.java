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

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import de.cosmocode.commons.reflect.Reflection;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Jackson based transcoder that uses the {@link MappingJsonFactory} to transcode any POJO.
 *
 * @author Oliver Lorenz
 */
enum JacksonTranscoder implements Transcoder<IdleTimeAwareValue> {

    INSTANCE;
    
    private static final Logger LOG = LoggerFactory.getLogger(JacksonTranscoder.class);

    private final JsonFactory factory = new MappingJsonFactory();

    @Override
    public boolean asyncDecode(CachedData data) {
        return false;
    }

    @Override
    public CachedData encode(IdleTimeAwareValue metaValue) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;

        try {
            objectOutputStream = new ObjectOutputStream(byteStream);

            // write metadata
            objectOutputStream.writeUTF(metaValue.getValueClassName());
            objectOutputStream.writeObject(metaValue.getKey());
            objectOutputStream.writeLong(metaValue.getIdleTimeInSeconds());
            objectOutputStream.writeLong(metaValue.getLifeTimeInSeconds());
            if (metaValue.getIdleTimeInSeconds() > 0) {
                objectOutputStream.writeLong(metaValue.getStoredAt().getTime());
                objectOutputStream.writeLong(metaValue.getLastAccessedAt().getTime());
            }

            factory.createJsonGenerator(objectOutputStream, JsonEncoding.UTF8).writeObject(metaValue.getValue());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(objectOutputStream);
        }

        final byte[] bytes = byteStream.toByteArray();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing {}", new String(bytes, Charsets.UTF_8));
        }
        
        return new CachedData(0, bytes, getMaxSize());
    }

    @Override
    public IdleTimeAwareValue decode(CachedData data) {
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(data.getData());
        ObjectInputStream inputStream = null;

        try {
            inputStream = new ObjectInputStream(new BufferedInputStream(byteStream));
            final IdleTimeAwareValue metaValue = new IdleTimeAwareValue();

            // read metadata
            final String className = inputStream.readUTF();
            metaValue.setKey(Serializable.class.cast(inputStream.readObject()));
            metaValue.setIdleTimeInSeconds(inputStream.readLong());
            metaValue.setLifeTimeInSeconds(inputStream.readLong());
            if (metaValue.getIdleTimeInSeconds() > 0) {
                metaValue.setStoredAt(new Date(inputStream.readLong()));
                metaValue.setLastAccessedAt(new Date(inputStream.readLong()));
            }

            // read real value
            final Class<?> valueType = Reflection.forName(className);
            LOG.trace("Read class {}", valueType);
            final JsonParser jsonInputStreamParser = factory.createJsonParser(inputStream);
            final Object value = jsonInputStreamParser.readValueAs(valueType);
            LOG.trace("Read value: {} of type {}", value, valueType);
            metaValue.setValue(value);

            return metaValue;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(inputStream);
        }
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
