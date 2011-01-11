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

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.Aspects;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

import de.cosmocode.palava.core.DefaultRegistryModule;
import de.cosmocode.palava.core.lifecycle.LifecycleModule;
import de.cosmocode.palava.ipc.ConnectionAwareUnitOfWorkScopeModule;
import de.cosmocode.palava.ipc.IpcScopeModule;
import de.cosmocode.palava.memcache.MemcacheClientModule;
import de.cosmocode.palava.scope.UnitOfWorkScopeAspect;

/**
 * Tests the {@link MemcacheCacheService}.
 *
 * @author Oliver Lorenz
 */
public final class MemcacheCacheServiceTest extends CacheServiceTest {
    
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
        final CacheService unit = Guice.createInjector(
                new LifecycleModule(),
                new DefaultRegistryModule(),
                new IpcScopeModule(),
                new ConnectionAwareUnitOfWorkScopeModule(),
                new AbstractModule() {
                    
                    @Override
                    protected void configure() {
                        bindConstant().annotatedWith(Names.named("memcache.addresses")).to("192.168.0.12:11211");
                    }
                    
                },
                new MemcacheClientModule(),
                new MemcacheCacheServiceModule()).
            getInstance(CacheService.class);
        return unit;
    }

}
