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

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserBean extends EntityBean {
    Boolean active;
    String credential;
    String email;
    String firstName;
    LocalDateTime lastLoginTime;
    String lastName;
    String loginName;
    Boolean notificationEmail;
    Boolean notificationSms;
    String sms;
    UserDetailBean singleUserDetail;
    Collection<UserDetailBean> userDetails;
    Collection collectionWithoutType;
    Collection<Map<String, Object>> collectionWithMapType;
    Map mapWithoutType;
    Map<String, UserDetailBean> mapWithValueType;
    Map<UserDetailBean, UserDetailBean> mapWithValueTypeAndKeyType;
    Country country;
    Country birthCountry;


    @Builder(builderMethodName = "userBeanBuilder")
    public UserBean(IdentifierBean compositeIdentifier,
                    Serializable id,
                    String guid,
                    String xmiid,
                    String auditCreateUser,
                    LocalDateTime auditCreateTime,
                    String auditUpdateUser,
                    LocalDateTime auditUpdateTime,
                    Boolean active,
                    String credential,
                    String email,
                    String firstName,
                    LocalDateTime lastLoginTime,
                    String lastName,
                    String loginName,
                    Boolean notificationEmail,
                    Boolean notificationSms,
                    String sms,
                    UserDetailBean singleUserDetail,
                    Collection<UserDetailBean> userDetails,
                    Collection collectionWithoutType,
                    Collection<Map<String, Object>> collectionWithMapType,
                    Map mapWithoutType,
                    Map<String, UserDetailBean> mapWithValueType,
                    Map<UserDetailBean, UserDetailBean> mapWithValueTypeAndKeyType,
                    Country country,
                    Country birthCountry

                    ) {
        super(compositeIdentifier,id,guid,xmiid,auditCreateUser,auditCreateTime,auditUpdateUser,auditUpdateTime);
        this.active = active;
        this.credential = credential;
        this.email = email;
        this.firstName = firstName;
        this.lastLoginTime = lastLoginTime;
        this.lastName = lastName;
        this.loginName = loginName;
        this.notificationEmail = notificationEmail;
        this.notificationSms = notificationSms;
        this.sms = sms;
        this.singleUserDetail = singleUserDetail;
        this.userDetails = userDetails;
        this.collectionWithoutType = collectionWithoutType;
        this.collectionWithMapType = collectionWithMapType;
        this.mapWithoutType = mapWithoutType;
        this.mapWithValueType = mapWithValueType;
        this.mapWithValueTypeAndKeyType = mapWithValueTypeAndKeyType;
        this.country = country;
        this.birthCountry = birthCountry;

    }

}
