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
