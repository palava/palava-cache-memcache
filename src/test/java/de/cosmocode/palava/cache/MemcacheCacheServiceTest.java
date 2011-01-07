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

/**
 * <p>
 * Tests the {@link MemcacheCacheService}.
 * </p>
 * <p>
 * Created on: 07.01.11
 * </p>
 *
 * @author Oliver Lorenz
 */
public class MemcacheCacheServiceTest extends CacheServiceTest {

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
        // TODO set the address to something external, so that the test always succeeds
        final MemcacheCacheService service = new MemcacheCacheService("192.168.0.12:11211");
        service.initialize();
        return service;
    }

}
