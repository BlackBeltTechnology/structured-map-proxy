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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

public interface UserBuilderWithPrefix {
    UserBuilderWithPrefix withXmiid(String par);
    UserBuilderWithPrefix withSms(String par);
    UserBuilderWithPrefix withNotificationSms(Boolean par);
    UserBuilderWithPrefix withNotificationEmail(Boolean par);
    UserBuilderWithPrefix withLoginName(String par);
    UserBuilderWithPrefix withLastName(String par);
    UserBuilderWithPrefix withLastLoginTime(LocalDateTime par);
    UserBuilderWithPrefix withGuid(String par);
    UserBuilderWithPrefix withFirstName(String par);
    UserBuilderWithPrefix withEmail(String par);
    UserBuilderWithPrefix withCredential(String par);
    UserBuilderWithPrefix withAuditUpdateUser(String par);
    UserBuilderWithPrefix withAuditUpdateTime(LocalDateTime par);
    UserBuilderWithPrefix withAuditCreateUser(String par);
    UserBuilderWithPrefix withAuditCreateTime(LocalDateTime par);
    UserBuilderWithPrefix withActive(Boolean par);
    UserBuilderWithPrefix withId(Serializable id);
    UserBuilderWithPrefix withEmptyConfigs();
    UserBuilderWithPrefix withUserInfoId(Serializable id);
    UserBuilderWithPrefix withUserDetails(Collection<UserDetail> userDetails);
    UserBuilderWithPrefix withSingleUserDetail(UserDetail userDetail);
    UserBuilderWithPrefix withCollectionWithoutType(Collection collectionWithoutType);
    UserBuilderWithPrefix withCollectionWithMapType(Collection<Map<String, Object>> collectionWithMapType);
    UserBuilderWithPrefix withMapWithoutType(Map withoutType);
    UserBuilderWithPrefix withMapWithValueType(Map<String, UserDetail> mapWithValueType);
    UserBuilderWithPrefix withMapWithValueTypeAndKeyType(Map<UserDetail, UserDetail> mapWithValueTypeAndKeyType);

    User build();
}
