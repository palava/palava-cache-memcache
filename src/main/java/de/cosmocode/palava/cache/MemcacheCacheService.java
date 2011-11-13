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

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import de.cosmocode.jackson.JacksonRenderer;
import de.cosmocode.palava.ipc.Current;
import de.cosmocode.rendering.Renderer;
import net.spy.memcached.MemcachedClientIF;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memcache based implementation of {@link CacheService}.
 *
 * @author Oliver Lorenz
 */
final class MemcacheCacheService extends AbstractCacheService {

    private static final Logger LOG = LoggerFactory.getLogger(MemcacheCacheService.class);

    private final Provider<MemcachedClientIF> currentClient;

    @Inject
    public MemcacheCacheService(@Current Provider<MemcachedClientIF> currentClient) {
        this.currentClient = Preconditions.checkNotNull(currentClient, "CurrentClient");
    }

    private String encode(Serializable key) {
        final Renderer r = new JacksonRenderer();
        final String renderedKey = r.value(key).build().toString();
        // hash the key, so that it doesn't exceed the 250 character limit
        return DigestUtils.shaHex(renderedKey);
    }

    @Override
    public void store(Serializable key, Object value, CacheExpiration expiration) {
        Preconditions.checkNotNull(key, "Key");
        Preconditions.checkNotNull(expiration, "Expiration");

        final int timeout = (int) expiration.getLifeTimeIn(TimeUnit.SECONDS);
        final MemcachedClientIF client = currentClient.get();
        LOG.trace("Storing {} => {}..", key, value);

        final IdleTimeAwareValue idleTimeAwareValue = new IdleTimeAwareValue();
        idleTimeAwareValue.setValue(value);
        idleTimeAwareValue.setIdleTimeInSeconds(expiration.getIdleTimeIn(TimeUnit.SECONDS));
        idleTimeAwareValue.setLifeTimeInSeconds(expiration.getLifeTimeIn(TimeUnit.SECONDS));
        if (idleTimeAwareValue.getIdleTimeInSeconds() > 0) {
            idleTimeAwareValue.setStoredAt(new Date());
            idleTimeAwareValue.setLastAccessedAt(new Date());
        }
        client.set(encode(key), timeout, idleTimeAwareValue, JacksonTranscoder.INSTANCE);
    }

    @Override
    public <V> V read(Serializable key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF client = currentClient.get();

        final Object rawValue = client.get(encode(key), JacksonTranscoder.INSTANCE);
        final IdleTimeAwareValue idleTimeAwareValue = (IdleTimeAwareValue) rawValue;
        if (idleTimeAwareValue == null) {
            return null;
        } else if (idleTimeAwareValue.isExpired()) {
            client.delete(encode(key));
            return null;
        } else {
            if (idleTimeAwareValue.getIdleTimeInSeconds() > 0) {
                // update the value in the cache with last accessed: now; deliberately unsafe use of set
                idleTimeAwareValue.setLastAccessedAt(new Date());
                final int timeout = idleTimeAwareValue.calculateNewTimeout();
                client.set(encode(key), timeout, idleTimeAwareValue, JacksonTranscoder.INSTANCE);
            }

            @SuppressWarnings("unchecked")
            final V value = (V) idleTimeAwareValue.getValue();
            LOG.trace("Read value {} for key '{}'", value, key);
            return value;
        }
    }

    @Override
    public <V> V remove(Serializable key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF client = currentClient.get();
        final String encodedKey = encode(key);

        final V item = read(key);
        client.delete(encodedKey);
        return item;
    }

    @Override
    public void clear() {
        currentClient.get().flush();
    }
}
