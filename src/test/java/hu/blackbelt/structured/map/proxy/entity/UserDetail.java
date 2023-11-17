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

import hu.blackbelt.structured.map.proxy.annotation.Key;

import java.util.Objects;

public interface UserDetail {
    @Key(name = "__id")
    String getId();

    void setId(String id, Object... args);

    String getNote();

    void setNote(String note, Object... args);

    static boolean equals(UserDetail o1, Object o2) {
        if (o2 == null) {
            return false;
        }
        if (UserDetail.class.isAssignableFrom(o2.getClass())) {
            return o1.getId().equals(((UserDetail) o2).getId());
        }
        return false;
    }

    static String toString(UserDetail o1) {
        return String.format("{ id: %s, note: %s }",
                Objects.toString(o1.getId(), "null"),
                Objects.toString(o1.getNote(), "null")
        );
    }

}
