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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import de.cosmocode.palava.core.DefaultRegistryModule;
import de.cosmocode.palava.core.Framework;
import de.cosmocode.palava.core.Palava;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.core.lifecycle.LifecycleModule;
import de.cosmocode.palava.memcache.MemcacheClientModule;
import de.cosmocode.palava.memcache.MemcacheLocalServerModule;
import de.cosmocode.palava.scope.SingletonUnitOfWorkScopeModule;
import org.junit.After;
import org.junit.Before;

/**
 * Tests the {@link MemcacheCacheService}.
 *
 * @author Oliver Lorenz
 */
public final class MemcacheCacheServiceTest extends CacheServiceTest {

    private final Framework framework = Palava.newFramework(new AbstractModule() {

        @Override
        protected void configure() {
            install(new LifecycleModule());
            install(new DefaultRegistryModule());
            install(new SingletonUnitOfWorkScopeModule());

            final String address = "127.0.0.1";
            final int port = 11211;

            // configure local JVM memcache server
            bindConstant().annotatedWith(Names.named("local.memcache.server.verbose")).to(true);
            bindConstant().annotatedWith(Names.named("local.memcache.server.address")).to(address);
            bindConstant().annotatedWith(Names.named("local.memcache.server.port")).to(port);
            install(new MemcacheLocalServerModule());

            // configure memcache client
            bindConstant().annotatedWith(Names.named("memcache.addresses")).to(address + ":" + port);
            install(new MemcacheClientModule());

            install(new MemcacheCacheServiceModule());
        }

    }, new Properties());

    @Before
    public void start() throws LifecycleException {
        framework.start();
    }

    @After
    public void stop() throws LifecycleException {
        framework.stop();
    }

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
        return framework.getInstance(CacheService.class);
    }

}
