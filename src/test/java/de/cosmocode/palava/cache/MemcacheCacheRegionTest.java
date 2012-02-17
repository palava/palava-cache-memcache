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

import com.google.common.base.Predicate;
import de.cosmocode.junit.LoggingRunner;
import de.cosmocode.palava.core.Framework;
import de.cosmocode.palava.core.Palava;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Properties;

/**
 * Tests {@link MemcacheCacheRegion}.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
@RunWith(LoggingRunner.class)
public class MemcacheCacheRegionTest {

    private final Framework framework = Palava.newFramework(new MemcacheTestModule(), new Properties());

    @Before
    public void start() throws LifecycleException {
        framework.start();
    }

    @After
    public void stop() throws LifecycleException {
        framework.stop();
    }

    public <K extends Serializable, V> CacheRegion<K, V> getCacheRegion(final String name) {
        final CacheRepository cacheRepository = framework.getInstance(CacheRepository.class);
        return cacheRepository.getRegion(name);
    }

    @Test
    public void putItem() {
        final CacheRegion<String, Object> cacheRegion = getCacheRegion("test");
        final TestObject testValue = new TestObject("John", "Mal", 12);
        cacheRegion.put("testItem", testValue);
        Assert.assertEquals(testValue, cacheRegion.get("testItem"));
    }

    @Test
    public void removeIf() {
        final CacheRegion<Integer, String> cacheRegion = getCacheRegion("test");
        cacheRegion.put(5, "abc");
        cacheRegion.put(10, "def");
        Assert.assertTrue(cacheRegion.containsKey(10));
        final boolean removedAnything = cacheRegion.removeIf(new Predicate<Integer>() {
            @Override
            public boolean apply(@Nullable Integer input) {
                return input != null && input == 10;
            }
        });
        Assert.assertTrue(removedAnything);
        Assert.assertFalse(cacheRegion.containsKey(10));
        Assert.assertTrue(cacheRegion.containsKey(5));
    }

    @Test
    public void putItemWithBigKey() {
        final CacheRegion<Serializable, TestObject> cacheRegion = getCacheRegion("test");
        // we have to use HashMap as the type, because we need some serializable type as the key and Map alone is not enough
        final HashMap<String, String> bigKey = new HashMap<String, String>(10);
        bigKey.put("asdfasdfasdfasdfasdfa sdfasdf asdf asdf asdfasdf asdf asd asdf asdf", "asd fasdf asdf asdf asdf");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpoiejw qpoinkvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpokvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpoasdf asdfa sdkvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fas{[¹²³¼[]¹²{¼ æſðđ{asdödfjpqio bng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←[ „“¹}[“↓ }¼[] bbwe");
        bigKey.put("asd fasdf asdfas dfassödfjpokvnpyisdupqio bng", "apvo iahpoeianpwasdf afsdpabwe");
        bigKey.put("asd fasdf asdf asdfasdf asöasd dfa sdkvnpyisdupqio bng", "apvo iahpoeasd fdsafabwe");
        bigKey.put("asd fasdf asdupqio bng", "apvo iahpoeianpwiubvipupab] æ ſ[“¹[]“ [{ ſ“æ][ſ{] ſðđwe");
        bigKey.put("asd fas{[¹²³¼[]¹²{¼ æſðđ{asdödfng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←[ „“¹}[“↓ }¼[] bbwe");
        bigKey.put("asd fas{[ng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←] bbwe");
        final TestObject value = new TestObject("bla", "blubb", 1);
        cacheRegion.put(bigKey, value);
        Assert.assertEquals(value, cacheRegion.get(bigKey));
    }

    @Test
    public void putItemWithBigKeyUsingHashedSerialize() {
        final MemcacheCacheRepository cacheRepository = framework.getInstance(MemcacheCacheRepository.class);
        cacheRepository.setKeyMarshaller(KeyMarshallers.HASHED_SERIALIZE);
        final CacheRegion<Serializable, TestObject> cacheRegion = cacheRepository.getRegion("test");
        // we have to use HashMap as the type, because we need some serializable type as the key and Map alone is not enough
        final HashMap<String, String> bigKey = new HashMap<String, String>(10);
        bigKey.put("asdfasdfasdfasdfasdfa sdfasdf asdf asdf asdfasdf asdf asd asdf asdf", "asd fasdf asdf asdf asdf");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpoiejw qpoinkvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpokvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fasdf asdf asdfasdf asödfjpoasdf asdfa sdkvnpyisdupqio bng", "apvo iahpoeianpwiubvipupabwe");
        bigKey.put("asd fas{[¹²³¼[]¹²{¼ æſðđ{asdödfjpqio bng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←[ „“¹}[“↓ }¼[] bbwe");
        bigKey.put("asd fasdf asdfas dfassödfjpokvnpyisdupqio bng", "apvo iahpoeianpwasdf afsdpabwe");
        bigKey.put("asd fasdf asdf asdfasdf asöasd dfa sdkvnpyisdupqio bng", "apvo iahpoeasd fdsafabwe");
        bigKey.put("asd fasdf asdupqio bng", "apvo iahpoeianpwiubvipupab] æ ſ[“¹[]“ [{ ſ“æ][ſ{] ſðđwe");
        bigKey.put("asd fas{[¹²³¼[]¹²{¼ æſðđ{asdödfng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←[ „“¹}[“↓ }¼[] bbwe");
        bigKey.put("asd fas{[ng", "apvo iahpoeianpwiubvipupa⅞ Æ]} ð[←] bbwe");
        final TestObject value = new TestObject("bla", "blubb", 1);
        cacheRegion.put(bigKey, value);
        Assert.assertEquals(value, cacheRegion.get(bigKey));
    }
}
