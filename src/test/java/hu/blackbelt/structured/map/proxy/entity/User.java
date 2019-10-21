package hu.blackbelt.judo.framework.lang.entity;

import java.time.LocalDateTime;

public interface User extends Entity {
    Boolean getActive();
    void setActive(Boolean active);
    LocalDateTime getAuditCreateTime();
    void setAuditCreateTime(LocalDateTime auditCreateTime);
    String getAuditCreateUser();
    void setAuditCreateUser(String auditCreateUser);
    LocalDateTime getAuditUpdateTime();
    void setAuditUpdateTime(LocalDateTime auditUpdateTime);
    String getAuditUpdateUser();
    void setAuditUpdateUser(String auditUpdateUser);
    String getCredential();
    void setCredential(String credential);
    String getEmail();
    void setEmail(String email);
    String getFirstName();
    void setFirstName(String firstName);
    String getGuid();
    void setGuid(String guid);
    LocalDateTime getLastLoginTime();
    void setLastLoginTime(LocalDateTime lastLoginTime);
    String getLastName();
    void setLastName(String lastName);
    String getLoginName();
    void setLoginName(String loginName);
    Boolean getNotificationEmail();
    void setNotificationEmail(Boolean notificationEmail);
    Boolean getNotificationSms();
    void setNotificationSms(Boolean notificationSms);
    String getSms();
    void setSms(String sms);
    String getXmiid();
    void setXmiid(String xmiid);
}
