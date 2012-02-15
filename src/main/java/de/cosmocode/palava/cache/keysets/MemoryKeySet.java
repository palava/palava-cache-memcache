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

import com.google.common.collect.ForwardingSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.cosmocode.palava.core.lifecycle.Disposable;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * KeySet that saves the keys in the memory, in CopyOnWriteArraySet.
 * It also persists the list to the hard disk on palava shutdown and reads it from the disk on palava startup.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
final class MemoryKeySet extends ForwardingSet<String> implements Set<String>, Initializable, Disposable {

    private final String name;
    private final Set<String> keys = new CopyOnWriteArraySet<String>();

    @Inject
    MemoryKeySet(@Assisted final String name) {
        this.name = name;
    }

    @Override
    protected Set<String> delegate() {
        return keys;
    }

    @Override
    public void initialize() throws LifecycleException {
        // attempt to read list from hard disk
    }

    @Override
    public void dispose() throws LifecycleException {
        // write list to hard disk
    }

    @Override
    public String toString() {
        return "MemoryKeySet{" +
                "name='" + name + '\'' +
                ", keys=" + keys +
                '}';
    }
}
