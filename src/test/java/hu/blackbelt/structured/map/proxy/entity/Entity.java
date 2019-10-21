package hu.blackbelt.judo.framework.lang.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public interface Entity extends Serializable {
    Serializable getId();
    void setId(Serializable id);
    String getGuid();
    void setGuid(String guid);
    String getXmiid();
    void setXmiid(String xmiid);
    String getAuditCreateUser();
    void setAuditCreateUser(String auditCreateUser);
    LocalDateTime getAuditCreateTime();
    void setAuditCreateTime(LocalDateTime auditCreateTime);
    String getAuditUpdateUser();
    void setAuditUpdateUser(String auditUpdateUser);
    LocalDateTime getAuditUpdateTime();
    void setAuditUpdateTime(LocalDateTime auditUpdateTime);

}
