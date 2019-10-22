package hu.blackbelt.structured.map.proxy.entity;

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
    UserBuilder collectionWithoutType(Collection collectionWithoutType);
    UserBuilder collectionWithMapType(Collection<Map<String, Object>> collectionWithMapType);
    UserBuilder mapWithoutType(Map withoutType);
    UserBuilder mapWithValueType(Map<String, UserDetail> mapWithValueType);
    UserBuilder mapWithValueTypeAndKeyType(Map<UserDetail, UserDetail> mapWithValueTypeAndKeyType);

    User build();
}
