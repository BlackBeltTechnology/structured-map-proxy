package hu.blackbelt.structured.map.proxy.entity;

/*-
 * #%L
 * Structured map proxy
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.EqualsAndHashCode;

import java.util.Date;

public interface Event {

    String getTitle();
    void setTitle(String title);

    Date getDate();
    void setDate(Date date);

    boolean isPrivate();
    void setPrivate(boolean _private);

    UpperCaseString getRoom();
    void setRoom(UpperCaseString room);

    String getNotes();
    void setNotes(String notes);

    @EqualsAndHashCode
    class UpperCaseString {

        private String _internal;

        private UpperCaseString() {
        }

        public static UpperCaseString parse(String str) {
            UpperCaseString instance = new UpperCaseString();
            instance._internal = str != null ? str.toUpperCase() : null;
            return instance;
        }

        @Override
        public String toString() {
            return String.valueOf(_internal);
        }
    }
}
