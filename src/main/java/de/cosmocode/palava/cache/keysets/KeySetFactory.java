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

package de.cosmocode.palava.cache.keysets;

import java.util.Set;

/**
 * A factory that produces Sets that hold the keys as they appear in memcache for a cache region.
 * The implementations are free to store them whereever and in whichever way they choose.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
public interface KeySetFactory {

    /**
     * Constructs the new key set with the given unqiue name.
     *
     * @param name the name of the key set, the same as the cache region; can be used for persistence
     * @return a new Set of Strings that usually handles persistence over application restart
     */
    Set<String> create(final String name);

}
