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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import de.cosmocode.jackson.JacksonRenderer;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.ipc.Current;
import de.cosmocode.rendering.Renderer;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.transcoders.BaseSerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Memcache based implementation of {@link CacheService}.
 * </p>
 * <p>
 * Created on: 07.01.11
 * </p>
 *
 * @author Oliver Lorenz
 */
final class MemcacheService implements CacheService, Initializable, Provider<MemcachedClientIF> {

    private static final String MAX_AGE_NEGATIVE = "Max age must not be negative, but was %s";

    private static final Logger LOG = LoggerFactory.getLogger(MemcacheService.class);

    private final List<InetSocketAddress> addresses;
    private final Provider<MemcachedClientIF> memcachedClientProvider;

    private boolean binary;
    private long defaultTimeout;
    private TimeUnit defaultTimeoutUnit = TimeUnit.SECONDS;
    private int compressionThreshold = -1;
    private HashAlgorithm hashAlgorithm = HashAlgorithm.NATIVE_HASH;
    private ConnectionFactory cf;

    @Inject
    public MemcacheService(
            @Named(MemcacheServiceConfig.ADRESSES) String addresses,
            @Current Provider<MemcachedClientIF> memcachedClientProvider) {
        this.memcachedClientProvider = memcachedClientProvider;
        Preconditions.checkNotNull(addresses, "Addresses");
        this.addresses = AddrUtil.getAddresses(addresses);
    }

    @Inject(optional = true)
    public void setBinary(@Named(MemcacheServiceConfig.BINARY) boolean binary) {
        this.binary = binary;
    }

    @Inject(optional = true)
    public void setDefaultTimeout(@Named(MemcacheServiceConfig.DEFAULT_TIMEOUT) long defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    @Inject(optional = true)
    public void setDefaultTimeoutUnit(@Named(MemcacheServiceConfig.DEFAULT_TIMEOUT_UNIT) TimeUnit defaultTimeoutUnit) {
        this.defaultTimeoutUnit = defaultTimeoutUnit;
    }

    @Inject(optional = true)
    public void setCompressionThreshold(@Named(MemcacheServiceConfig.COMPRESSION_THRESHOLD) int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Inject(optional = true)
    public void setHashAlgorithm(@Named(MemcacheServiceConfig.HASH_ALGORITHM) HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @Override
    public void initialize() throws LifecycleException {
        if (binary) {
            cf = new BinaryConnectionFactory(
                BinaryConnectionFactory.DEFAULT_OP_QUEUE_LEN,
                BinaryConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
                hashAlgorithm
            );
        } else {
            cf = new DefaultConnectionFactory(
                DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN,
                DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
                hashAlgorithm
            );
        }
    }

    @Override
    public MemcachedClientIF get() {
        try {
            final MemcachedClient client = new MemcachedClient(cf, addresses);

            if (compressionThreshold >= 0) {
                if (client.getTranscoder() instanceof BaseSerializingTranscoder) {
                    final BaseSerializingTranscoder bst = (BaseSerializingTranscoder) client.getTranscoder();
                    bst.setCompressionThreshold(compressionThreshold);
                } else {
                    throw new UnsupportedOperationException(
                        "cannot set compression threshold; transcoder does not extend BaseSerializingTranscoder");
                }
            }

            return client;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toStringKey(Serializable key) {
        // render key as json
        final Renderer rKey = new JacksonRenderer();
        return rKey.value(key).build().toString();
    }

    @Override
    public void store(Serializable key, Object value) {
        store(key, value, defaultTimeout, defaultTimeoutUnit);
    }

    @Override
    public void store(Serializable key, Object value, long maxAge, TimeUnit maxAgeUnit) {
        Preconditions.checkNotNull(key, "Key");
        Preconditions.checkArgument(maxAge >= 0, MAX_AGE_NEGATIVE, maxAge);
        Preconditions.checkNotNull(maxAgeUnit, "MaxAge TimeUnit");

        final int timeout = (int) maxAgeUnit.toSeconds(maxAge);

        // get the memcache connection
        final MemcachedClientIF memcache = memcachedClientProvider.get();

        // store it
        LOG.trace("Storing {} => {}..", key, value);
        memcache.set(toStringKey(key), timeout, value, JacksonTranscoder.INSTANCE);
    }

    @Override
    public void store(Serializable key, Object value, CacheExpiration expiration) {
        Preconditions.checkNotNull(key, "Key");
        Preconditions.checkNotNull(expiration, "Expiration");
        // just redirect to store with max age, because memcache does not support idle time
        store(key, value, expiration.getLifeTime(), expiration.getLifeTimeUnit());
    }

    @Override
    public <V> V read(Serializable key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF memcache = memcachedClientProvider.get();
        @SuppressWarnings("unchecked")
        final V value = (V) memcache.get(toStringKey(key), JacksonTranscoder.INSTANCE);
        LOG.trace("Read value {} for key {}", value, key);
        return value;
    }

    @Override
    public <V> V remove(Serializable key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF memcache = memcachedClientProvider.get();
        @SuppressWarnings("unchecked")
        final V item = (V) memcache.get(toStringKey(key), JacksonTranscoder.INSTANCE);
        memcache.delete(toStringKey(key));
        return item;
    }

    @Override
    public void clear() {
        memcachedClientProvider.get().flush();
    }
}
