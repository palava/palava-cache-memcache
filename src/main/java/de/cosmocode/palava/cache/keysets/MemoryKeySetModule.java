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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Binds the memory key set implementation to be constructed by the KeySetFactory.
 * This implementation saves the keys in a set in the memory of the jvm.
 * When the palava environment shuts down normally then it persists the keys to the disk.
 * Once the environment starts again the keys are read again from the disk.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
public class MemoryKeySetModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(KeySetFactory.class).toProvider(
                FactoryProvider.newFactory(KeySetFactory.class, MemoryKeySet.class));
    }

}
