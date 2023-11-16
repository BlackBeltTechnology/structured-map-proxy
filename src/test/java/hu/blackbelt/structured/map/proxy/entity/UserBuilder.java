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

public interface UserBuilder {
    UserBuilder xmiid(String par);
    UserBuilder sms(String par);
    UserBuilder notificationSms(Boolean par);
    UserBuilder notificationEmail(Boolean par);
    UserBuilder loginName(String par);
    UserBuilder lastName(String par);
    UserBuilder lastLoginTime(LocalDateTime par);
    UserBuilder guid(String par);
    UserBuilder firstName(String par);
    UserBuilder email(String par);
    UserBuilder credential(String par);
    UserBuilder auditUpdateUser(String par);
    UserBuilder auditUpdateTime(LocalDateTime par);
    UserBuilder auditCreateUser(String par);
    UserBuilder auditCreateTime(LocalDateTime par);
    UserBuilder active(Boolean par);
    UserBuilder id(Serializable id);
    UserBuilder emptyConfigs();
    UserBuilder userInfoId(Serializable id);
    UserBuilder userDetails(Collection<UserDetail> userDetails);
    UserBuilder addToUserDetails(UserDetail first, UserDetail... rest);
    UserBuilder singleUserDetail(UserDetail userDetail);
    UserBuilder collectionWithoutType(Collection collectionWithoutType);
    UserBuilder collectionWithMapType(Collection<Map<String, Object>> collectionWithMapType);
    UserBuilder mapWithoutType(Map withoutType);
    UserBuilder mapWithValueType(Map<String, UserDetail> mapWithValueType);
    UserBuilder mapWithValueTypeAndKeyType(Map<UserDetail, UserDetail> mapWithValueTypeAndKeyType);

    User build();
}
