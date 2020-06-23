package hu.blackbelt.structured.map.proxy.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

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

    UserDetail getSingleUserDetail();
    void setSingleUserDetail(UserDetail userDetail);

    Collection<UserDetail> getUserDetails();
    void setUserDetails(Collection<UserDetail> userDetails);

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

}
