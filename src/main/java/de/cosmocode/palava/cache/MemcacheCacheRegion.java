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
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import de.cosmocode.palava.cache.keysets.KeySetFactory;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.transcoders.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Memcache based implementation of the cache region.
 * Since memcache does not provide native support for lists and there are several different ways to fix this,
 * the user can configure the factory that creates these key sets for the cache region.
 *
 * @param <K> the type of the keys, must be serializable
 * @param <V> the type of the values, which should either extend serializable or conform to the bean standard
 * @since 1.0
 */
class MemcacheCacheRegion<K extends Serializable, V> extends AbstractMap<K, V> implements CacheRegion<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(MemcacheCacheRegion.class);

    private final Set<String> keySet;
    private final Provider<MemcachedClientIF> currentClient;
    private final KeyMarshaller keyMarshaller;
    private final Transcoder<Object> transcoder;
    private final String name;

    private final Set<Entry<K, V>> entrySet = new EntrySet();

    MemcacheCacheRegion(
            final KeySetFactory keySetFactory,
            final Provider<MemcachedClientIF> currentClient,
            final KeyMarshaller keyMarshaller,
            final Marshaller marshaller,
            final String name) {
        this.keySet = keySetFactory.create(name);
        this.currentClient = currentClient;
        this.keyMarshaller = keyMarshaller;
        this.transcoder = new MarshallerTranscoder(marshaller);
        this.name = name;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    @Override
    public int size() {
        return keySet.size();
    }

    @Override
    public V get(Object key) {
        Preconditions.checkNotNull(key, "Key");
        final MetaValue metaValue = getInternal(keyMarshaller.encode(Serializable.class.cast(key)));

        if (metaValue == null) {
            return null;
        } else {
            @SuppressWarnings("unchecked")
            final V value = (V) metaValue.getValue();
            LOG.trace("Read value {} for key '{}'", value, key);
            return value;
        }
    }

    private MetaValue getInternal(String key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF client = currentClient.get();

        final Object rawValue = client.get(key, transcoder);
        @SuppressWarnings("unchecked")
        final MetaValue metaValue = (MetaValue) rawValue;
        if (metaValue == null) {
            return null;
        } else if (metaValue.isExpired()) {
            client.delete(key);
            return null;
        } else {
            if (metaValue.getIdleTimeInSeconds() > 0) {
                // update the value in the cache with last accessed: now; deliberately unsafe use of set
                metaValue.setLastAccessedAt(new Date());
                final int timeout = metaValue.calculateNewTimeout();
                client.set(key, timeout, metaValue, transcoder);
            }

            return metaValue;
        }
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, CacheExpirations.ETERNAL);
    }

    @Override
    public V put(K key, V value, CacheExpiration expiration) {
        Preconditions.checkNotNull(key, "Key");
        Preconditions.checkNotNull(expiration, "Expiration");

        final int timeout = (int) expiration.getLifeTimeIn(TimeUnit.SECONDS);
        final MemcachedClientIF client = currentClient.get();
        LOG.trace("Storing {} => {}..", key, value);

        final MetaValue metaValue = new MetaValue();
        metaValue.setKey(key);
        metaValue.setValue(value);
        metaValue.setIdleTimeInSeconds((int) expiration.getIdleTimeIn(TimeUnit.SECONDS));
        metaValue.setLifeTimeInSeconds((int) expiration.getLifeTimeIn(TimeUnit.SECONDS));
        if (metaValue.getIdleTimeInSeconds() > 0) {
            metaValue.setStoredAt(new Date());
            metaValue.setLastAccessedAt(new Date());
        }

        final V previousValue = get(key);
        final String encodedKey = keyMarshaller.encode(key);
        client.set(encodedKey, timeout, metaValue, transcoder);
        keySet.add(encodedKey);
        return previousValue;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, CacheExpirations.ETERNAL);
    }

    @Override
    public V putIfAbsent(K key, V value, CacheExpiration expiration) {
        return null;
    }

    @Override
    public V remove(Object key) {
        Preconditions.checkNotNull(key, "Key");
        final MemcachedClientIF client = currentClient.get();
        final String encodedKey = keyMarshaller.encode(Serializable.class.cast(key));

        final V item = get(key);
        client.delete(encodedKey);
        keySet.remove(encodedKey);
        return item;
    }

    @Override
    public boolean removeIf(Predicate<? super K> predicate) {
        boolean removedAnything = false;
        for (final Entry<K, V> entry : entrySet()) {
            if (predicate.apply(entry.getKey())) {
                remove(entry.getKey());
                removedAnything = true;
            }
        }
        return removedAnything;
    }

    @Override
    public void clear() {
        currentClient.get().flush();
        keySet.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public V replace(K key, V value) {
        return null;
    }

    /**
     * Memcache specific implementation of the entry set.
     */
    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        @Override
        public int size() {
            return keySet.size();
        }

    }

    /**
     * Memcache specific implementation of the iterator over the entry set.
     */
    private final class EntrySetIterator extends AbstractIterator<Entry<K, V>> implements Iterator<Entry<K, V>> {

        private final Iterator<String> keyIterator = keySet.iterator();

        @Override
        @SuppressWarnings("unchecked")
        protected Entry<K, V> computeNext() {
            if (keyIterator.hasNext()) {
                MetaValue metaValue = null;
                while (metaValue == null && keyIterator.hasNext()) {
                    metaValue = getInternal(keyIterator.next());
                }
                if (metaValue == null) {
                    return endOfData();
                } else {
                    return Maps.immutableEntry((K) metaValue.getKey(), (V) metaValue.getValue());
                }
            } else {
                return endOfData();
            }
        }

    }

}
