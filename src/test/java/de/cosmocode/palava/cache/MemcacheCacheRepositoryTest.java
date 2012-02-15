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

import de.cosmocode.palava.core.Framework;
import de.cosmocode.palava.core.Palava;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

/**
 * Tests {@link MemcacheCacheRepository}.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
public class MemcacheCacheRepositoryTest {

    private final Framework framework = Palava.newFramework(new MemcacheTestModule(), new Properties());

    @Before
    public void start() throws LifecycleException {
        framework.start();
    }

    @After
    public void stop() throws LifecycleException {
        framework.stop();
    }

    @Test
    public void memcacheRepositoryBound() {
        final CacheRepository cacheRepository = framework.getInstance(CacheRepository.class);
        Assert.assertTrue(cacheRepository instanceof MemcacheCacheRepository);
    }

    @Test
    public void createOneRegion() {
        final CacheRepository cacheRepository = framework.getInstance(CacheRepository.class);
        final CacheRegion<Integer, String> cacheRegion = cacheRepository.getRegion("test");
        Assert.assertNotNull(cacheRegion);
        Assert.assertEquals("test", cacheRegion.getName());
    }

    @Test
    public void createTwoRegions() {
        final CacheRepository cacheRepository = framework.getInstance(CacheRepository.class);
        final CacheRegion<Integer, String> cacheRegion1 = cacheRepository.getRegion("region1");
        Assert.assertEquals("region1", cacheRegion1.getName());
        final CacheRegion<String, Object> cacheRegion2 = cacheRepository.getRegion("region2");
        Assert.assertEquals("region2", cacheRegion2.getName());
    }

}
