package hu.blackbelt.judo.framework.lang.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    User build();
}
