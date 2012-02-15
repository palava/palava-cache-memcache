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
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * A wrapper around the value that should be stored in memcached.
 * This wrapper stores all relevant information to model a idle-timeout-based storage.
 *
 * @since 1.0
 */
final class MetaValue implements Serializable {

    private long idleTimeInSeconds;
    private long lifeTimeInSeconds;
    private Date storedAt;
    private Date lastAccessedAt;
    private Serializable key;
    private Object value;

    public long getIdleTimeInSeconds() {
        return idleTimeInSeconds;
    }

    public void setIdleTimeInSeconds(long idleTimeInSeconds) {
        this.idleTimeInSeconds = idleTimeInSeconds;
    }

    public long getLifeTimeInSeconds() {
        return lifeTimeInSeconds;
    }

    public void setLifeTimeInSeconds(long lifeTimeInSeconds) {
        this.lifeTimeInSeconds = lifeTimeInSeconds;
    }

    public Date getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(Date storedAt) {
        this.storedAt = storedAt;
    }

    public Date getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Date lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Serializable getKey() {
        return key;
    }

    public void setKey(Serializable key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getValueClassName() {
        return value.getClass().getName();
    }

    /**
     * Returns true if the idle time is set and item was not accessed for {@link #getIdleTimeInSeconds()}.
     * @return true if idle time is set and item has idled out
     */
    @JsonIgnore
    public boolean isExpired() {
        if (getIdleTimeInSeconds() == 0) {
            return false;
        } else {
            final Date currentDate = new Date();
            final long msSinceLastAccess = currentDate.getTime() - getLastAccessedAt().getTime();
            return msSinceLastAccess > getIdleTimeInSeconds() * 1000;
        }
    }

    /**
     * Calculate a new timeout based on the time that this entry was initially stored and
     * the lifetime in seconds.
     * @return the new timeout for a set in memcached
     */
    @JsonIgnore
    public int calculateNewTimeout() {
        final Date currentDate = new Date();
        final long msSinceStored = currentDate.getTime() - getStoredAt().getTime();
        return (int) (getLifeTimeInSeconds() - msSinceStored * 1000);
    }
}
