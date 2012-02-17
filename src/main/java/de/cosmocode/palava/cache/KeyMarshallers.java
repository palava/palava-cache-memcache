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

import de.cosmocode.jackson.JacksonRenderer;
import de.cosmocode.rendering.Renderer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;

/**
 * Default key marshallers. {@link #HASHED_JSON} is the default for the MemcacheCacheRegion.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
public enum KeyMarshallers implements KeyMarshaller {

    JSON {
        @Override
        public String encode(Serializable key) {
            final Renderer r = new JacksonRenderer();
            return r.value(key).build().toString();
        }
    },
    HASHED_JSON {
        @Override
        public String encode(Serializable key) {
            // hash the json encoded key, so that it doesn't exceed the 250 character limit
            return DigestUtils.shaHex(JSON.encode(key));
        }
    },
    SERIALIZE {
        @Override
        public String encode(Serializable key) {
            return new String(SerializationUtils.serialize(key));
        }

        @Override
        public Serializable decode(String encoded) {
            return Serializable.class.cast(SerializationUtils.deserialize(encoded.getBytes()));
        }

        @Override
        public boolean isDecodable() {
            return true;
        }
    },
    HASHED_SERIALIZE {
        @Override
        public String encode(Serializable key) {
            return DigestUtils.shaHex(SerializationUtils.serialize(key));
        }
    };


    @Override
    public Serializable decode(String encoded) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDecodable() {
        return false;
    }
}
