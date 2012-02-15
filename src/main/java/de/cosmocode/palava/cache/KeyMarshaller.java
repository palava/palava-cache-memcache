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

import java.io.Serializable;

/**
 * A key Marshaller converts the Serializable key of the CacheRegion into a String key for memcached.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
public interface KeyMarshaller {

    /**
     * Converts the CacheRegion key into a memcached key.
     *
     * @param key the CacheRegion key
     * @return the memcached key
     */
    String encode(final Serializable key);

    /**
     * <p>
     *     Decodes a previously encoded key into a Serializable.
     * </p>
     * <p>
     *     This method may throw an UnsupportedOperationException if the implementation is a one-way-encoding.
     *     In this case the method {@link #isDecodable()} returns false.
     * </p>
     *
     * @param encoded the encoded key that should be decoded
     * @return the decoded Serializable
     * @throws UnsupportedOperationException if the implementation is a one-way-encoding
     */
    Serializable decode(final String encoded);

    /**
     * If this KeyMarshaller supports decoding.
     * @return true if {@link #decode(String)} can be safely called, false otherwise
     */
    boolean isDecodable();

}
