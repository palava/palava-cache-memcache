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

import com.google.common.base.Preconditions;

/**
 * Simple test object to test memcache serialization.
 *
 * @author Oliver Lorenz
 * @since 1.0
 */
@SuppressWarnings("UnusedDeclaration")
final class TestObject {

    private String forename;
    private String surname;
    private int counter;

    TestObject() {
    }

    TestObject(String forename, String surname, int counter) {
        this.forename = Preconditions.checkNotNull(forename, "Forename");
        this.surname = Preconditions.checkNotNull(surname, "Surname");
        this.counter = counter;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TestObject that = (TestObject) o;

        return counter == that.counter && forename.equals(that.forename) && surname.equals(that.surname);

    }

    @Override
    public int hashCode() {
        int result = forename.hashCode();
        result = 31 * result + surname.hashCode();
        result = 31 * result + counter;
        return result;
    }

    @Override
    public String toString() {
        return "TestObject{" +
                "forename='" + forename + '\'' +
                ", surname='" + surname + '\'' +
                ", counter=" + counter +
                '}';
    }
}
