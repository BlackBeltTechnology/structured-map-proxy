package hu.blackbelt.structured.map.proxy.entity;

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
    UserBuilderWithPrefix withCollectionWithoutType(Collection collectionWithoutType);
    UserBuilderWithPrefix withCollectionWithMapType(Collection<Map<String, Object>> collectionWithMapType);
    UserBuilderWithPrefix withMapWithoutType(Map withoutType);
    UserBuilderWithPrefix withMapWithValueType(Map<String, UserDetail> mapWithValueType);
    UserBuilderWithPrefix withMapWithValueTypeAndKeyType(Map<UserDetail, UserDetail> mapWithValueTypeAndKeyType);

    User build();
}
