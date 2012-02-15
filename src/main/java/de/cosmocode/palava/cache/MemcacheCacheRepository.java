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

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.cosmocode.palava.cache.keysets.KeySetFactory;
import de.cosmocode.palava.ipc.Current;
import net.spy.memcached.MemcachedClientIF;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

/**
 * Memcache implementation of the cache repository.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
class MemcacheCacheRepository implements CacheRepository {

    private final KeySetFactory keySetFactory;
    private final Provider<MemcachedClientIF> currentClient;
    private KeyMarshaller keyMarshaller = KeyMarshallers.HASHED_JSON;
    private Marshaller marshaller = JacksonMarshaller.INSTANCE;

    private final ConcurrentMap<String, CacheRegion<?, ?>> cacheRegionLookup = new MapMaker().makeMap();

    @Inject
    MemcacheCacheRepository(
            final KeySetFactory keySetFactory,
            @Current final Provider<MemcachedClientIF> currentClient) {
        this.keySetFactory = keySetFactory;
        this.currentClient = currentClient;
    }

    @Inject(optional = true)
    public void setKeyMarshaller(final KeyMarshaller keyMarshaller) {
        this.keyMarshaller = keyMarshaller;
    }

    @Inject(optional = true)
    public void setMarshaller(final Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V> CacheRegion<K, V> getRegion(final String name) {
        if (cacheRegionLookup.containsKey(name)) {
            return (CacheRegion<K, V>) cacheRegionLookup.get(name);
        } else {
            final CacheRegion<K, V> newCacheRegion = new MemcacheCacheRegion<K, V>(keySetFactory, currentClient,
                    keyMarshaller, marshaller, name);
            final CacheRegion<?, ?> previousCacheRegion = cacheRegionLookup.putIfAbsent(name, newCacheRegion);
            if (previousCacheRegion == null) {
                return newCacheRegion;
            } else {
                return (CacheRegion<K, V>) previousCacheRegion;
            }
        }
    }

}
