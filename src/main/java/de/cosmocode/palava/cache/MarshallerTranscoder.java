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
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;

/**
 * Wraps a marshaller with a transcoder, so that the transcoder can be given to the memcache spy library.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
final class MarshallerTranscoder implements Transcoder<Object> {

    private final Marshaller marshaller;

    MarshallerTranscoder(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean asyncDecode(CachedData cachedData) {
        return false;
    }

    @Override
    public CachedData encode(Object o) {
        final Serializable encoded = marshaller.apply(o);
        if (encoded instanceof byte[]) {
            return new CachedData(0, byte[].class.cast(encoded), getMaxSize());
        } else {
            return new CachedData(0, SerializationUtils.serialize(encoded), getMaxSize());
        }
    }

    @Override
    public Object decode(CachedData cachedData) {
        return marshaller.inverse().apply(cachedData.getData());
    }

    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

}
