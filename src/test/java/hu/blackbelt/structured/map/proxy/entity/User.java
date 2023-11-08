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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface User extends Entity {
    Boolean getActive();
    void setActive(Boolean active);
    String getCredential();
    void setCredential(String credential);
    String getEmail();
    void setEmail(String email);
    Optional<String> getFirstName();
    void setFirstName(String firstName);
    LocalDateTime getLastLoginTime();
    void setLastLoginTime(LocalDateTime lastLoginTime);
    Optional<String> getLastName();
    void setLastName(String lastName);
    Optional<String> getLoginName();
    void setLoginName(String loginName);
    Boolean getNotificationEmail();
    void setNotificationEmail(Boolean notificationEmail);
    Boolean getNotificationSms();
    void setNotificationSms(Boolean notificationSms);
    String getSms();
    void setSms(String sms);

    UserDetail getSingleUserDetail();
    void setSingleUserDetail(UserDetail userDetail);

    Collection<UserDetail> getUserDetails();
    void setUserDetails(Collection<UserDetail> userDetails);

    void addToUserDetails(UserDetail first, UserDetail... userDetails);

    void removeFromUserDetails(UserDetail first, UserDetail... userDetails);

    Collection getCollectionWithoutType();
    void setCollectionWithoutType(Collection collectionWithoutType);

    Collection<Map<String, Object>> getCollectionWithMapType();
    void setCollectionWithMapType(Collection<Map<String, Object>> collectionWithMapType);

    Map getMapWithoutType();
    void setMapWithoutType(Map map);

    Map<String, UserDetail> getMapWithValueType();
    void setMapWithValueType(Map<String, UserDetail> stringMapWithValueType);

    Map<UserDetail, UserDetail> getMapWithValueTypeAndKeyType();
    void setMapWithValueTypeAndKeyType(Map<UserDetail, UserDetail> mapWithValueTypeAndKeyType);

    Country getCountry();

    void setCountry(Country country);

    Optional<Country> getBirthCountry();

    void setBirthCountry(Country country);

    <T> T adaptTo(Class<T> clazz);

}
