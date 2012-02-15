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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Memcache based implementation of {@link CacheService}.
 *
 * @author Oliver Lorenz
 * @since 1.0
 * @deprecated replaced with {@link MemcacheCacheRegion}
 */
@Deprecated
final class MemcacheCacheService extends AbstractCacheService {

    private final CacheRegion<Serializable, Object> cacheRegion;

    @Inject
    MemcacheCacheService(final MemcacheCacheRepository memcacheCacheRepository) {
        cacheRegion = memcacheCacheRepository.getRegion("MemcacheCacheService");
    }

    @Override
    public void store(Serializable key, Object value, CacheExpiration expiration) {
        cacheRegion.put(key, value, expiration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V read(Serializable key) {
        return (V) cacheRegion.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V remove(Serializable key) {
        return (V) cacheRegion.remove(key);
    }

    @Override
    public void clear() {
        cacheRegion.clear();
    }
}
