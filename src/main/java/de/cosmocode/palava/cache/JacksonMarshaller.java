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
import de.cosmocode.commons.Bijection;
import de.cosmocode.commons.reflect.Reflection;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Jackson based marshaller that uses the {@link MappingJsonFactory} to encode any POJO.
 *
 * @author Oliver Lorenz
 */
enum JacksonMarshaller implements Marshaller {

    INSTANCE;
    
    private static final Logger LOG = LoggerFactory.getLogger(JacksonMarshaller.class);

    private final JsonFactory factory = new MappingJsonFactory();

    @Override
    public Bijection<Serializable, Object> inverse() {
        return JacksonInverseMarshaller.INSTANCE;
    }

    @Override
    public Serializable apply(@Nullable Object input) {
        if (input == null) {
            return null;
        }

        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;

        try {
            objectOutputStream = new ObjectOutputStream(byteStream);

            // write metadata
            final Object value;
            if (input instanceof MetaValue) {
                final MetaValue metaValue = MetaValue.class.cast(input);
                objectOutputStream.writeBoolean(true);
                objectOutputStream.writeUTF(metaValue.getValueClassName());

                objectOutputStream.writeObject(metaValue.getKey());
                objectOutputStream.writeLong(metaValue.getIdleTimeInSeconds());
                objectOutputStream.writeLong(metaValue.getLifeTimeInSeconds());
                if (metaValue.getIdleTimeInSeconds() > 0) {
                    objectOutputStream.writeLong(metaValue.getStoredAt().getTime());
                    objectOutputStream.writeLong(metaValue.getLastAccessedAt().getTime());
                }
                value = metaValue.getValue();
            } else {
                objectOutputStream.writeBoolean(false);
                objectOutputStream.writeUTF(input.getClass().getName());
                value = input;
            }

            factory.createJsonGenerator(objectOutputStream, JsonEncoding.UTF8).writeObject(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Closeables.closeQuietly(objectOutputStream);
        }

        final byte[] bytes = byteStream.toByteArray();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing {}", new String(bytes, Charsets.UTF_8));
        }
        
        return bytes;
    }

    /**
     * Decoding part of the Jackson marshaller implementation.
     */
    private enum JacksonInverseMarshaller implements Bijection<Serializable, Object> {

        INSTANCE;

        private final JsonFactory factory = new MappingJsonFactory();

        @Override
        public Bijection<Object, Serializable> inverse() {
            return JacksonMarshaller.INSTANCE;
        }

        @Override
        public Object apply(@Nullable Serializable input) {
            if (input == null) {
                return null;
            }

            final ByteArrayInputStream byteStream = new ByteArrayInputStream(byte[].class.cast(input));
            ObjectInputStream inputStream = null;

            try {
                inputStream = new ObjectInputStream(new BufferedInputStream(byteStream));

                // read metadata
                final MetaValue metaValue = new MetaValue();
                final boolean hasMetadata = inputStream.readBoolean();
                final String className = inputStream.readUTF();

                if (hasMetadata) {
                    metaValue.setKey(Serializable.class.cast(inputStream.readObject()));
                    metaValue.setIdleTimeInSeconds(inputStream.readLong());
                    metaValue.setLifeTimeInSeconds(inputStream.readLong());
                    if (metaValue.getIdleTimeInSeconds() > 0) {
                        metaValue.setStoredAt(new Date(inputStream.readLong()));
                        metaValue.setLastAccessedAt(new Date(inputStream.readLong()));
                    }
                }

                // read real value
                final Class<?> valueType = Reflection.forName(className);
                LOG.trace("Read class {}", valueType);
                final JsonParser jsonInputStreamParser = factory.createJsonParser(inputStream);
                final Object value = jsonInputStreamParser.readValueAs(valueType);
                LOG.trace("Read value: {} of type {}", value, valueType);

                if (hasMetadata) {
                    metaValue.setValue(value);
                    return metaValue;
                } else {
                    return value;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } finally {
                Closeables.closeQuietly(inputStream);
            }
        }
    }
}
