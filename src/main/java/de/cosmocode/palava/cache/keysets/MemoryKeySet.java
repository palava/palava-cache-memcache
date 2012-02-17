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
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.cosmocode.palava.core.lifecycle.Disposable;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
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

    private static final Logger LOG = LoggerFactory.getLogger(MemoryKeySet.class);

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
        try {
            final File serializationFile = getSerializationFile();
            if (serializationFile.exists()) {
                final Object deserialized = SerializationUtils.deserialize(
                        Files.newInputStreamSupplier(serializationFile).getInput()
                );
                @SuppressWarnings("unchecked")
                final Collection<String> deserializedKeys = (Collection<String>) deserialized;
                keys.addAll(deserializedKeys);
                LOG.info("Loaded {} keys from hard disk", keys.size());
            }
        } catch (IOException e) {
            throw new LifecycleException(e);
        }
    }

    @Override
    public void dispose() throws LifecycleException {
        // write list to hard disk
        try {
            SerializationUtils.serialize(
                    Serializable.class.cast(keys),
                    Files.newOutputStreamSupplier(getSerializationFile()).getOutput()
            );
        } catch (IOException e) {
            throw new LifecycleException(e);
        }
    }

    private File getSerializationFile() {
        final File parentDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "memoryKeySets");
        if (parentDir.mkdirs()) {
            LOG.info("Created parent serialization directory {}", parentDir);
        }
        return new File(parentDir, name + ".ser");
    }

    @Override
    public String toString() {
        return "MemoryKeySet{" +
                "name='" + name + '\'' +
                ", keys=" + keys +
                '}';
    }
}
