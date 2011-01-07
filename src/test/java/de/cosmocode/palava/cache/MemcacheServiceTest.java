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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import de.cosmocode.palava.core.DefaultRegistryModule;
import de.cosmocode.palava.core.inject.TypeConverterModule;
import net.spy.memcached.MemcachedClientIF;
import org.junit.After;

import java.util.concurrent.TimeUnit;

/**
 * <p></p>
 * <p>
 * Created on: 07.01.11
 * </p>
 *
 * @author Oliver Lorenz
 */
public class MemcacheServiceTest extends CacheServiceTest {

    @Override
    protected long lifeTime() {
        return 2;
    }

    @Override
    protected long idleTime() {
        return 2;
    }

    @Override
    protected long sleepTimeBeforeIdleTimeout() {
        return 1;
    }

    @Override
    protected long sleepTimeUntilExpired() {
        return 4;
    }

    @Override
    protected TimeUnit timeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public CacheService unit() {
        final MemcacheProviderProxy provider = new MemcacheProviderProxy();
        final MemcacheService service = new MemcacheService("192.168.0.12:11211", provider);
        provider.setOriginal(service);
        service.initialize();
        return service;
    }

    private static class MemcacheProviderProxy implements Provider<MemcachedClientIF> {

        private Provider<MemcachedClientIF> original;

        public void setOriginal(Provider<MemcachedClientIF> original) {
            this.original = original;
        }

        @Override
        public MemcachedClientIF get() {
            return original.get();
        }

    }

}
