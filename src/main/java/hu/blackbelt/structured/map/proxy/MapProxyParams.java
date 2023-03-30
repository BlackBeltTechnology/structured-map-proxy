package hu.blackbelt.structured.map.proxy;

/*-
 * #%L
 * Structured map proxy
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
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

public class MapProxyParams {
    private boolean immutable = false;
    private boolean nullSafeCollection = false;
    private String identifierField;
    private String enumMappingMethod = MapProxy.DEFAULT_ENUM_MAPPING_METHOD;
    private boolean mapNullToOptionalAbsent = false;

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public boolean isNullSafeCollection() {
        return nullSafeCollection;
    }

    public void setNullSafeCollection(boolean nullSafeCollection) {
        this.nullSafeCollection = nullSafeCollection;
    }

    public String getEnumMappingMethod() {
        return enumMappingMethod;
    }

    public void setEnumMappingMethod(String enumMappingMethod) {
        this.enumMappingMethod = enumMappingMethod;
    }

    public boolean isMapNullToOptionalAbsent() {
        return mapNullToOptionalAbsent;
    }

    public void setMapNullToOptionalAbsent(boolean mapNullToOptionalAbsent) {
        this.mapNullToOptionalAbsent = mapNullToOptionalAbsent;
    }
}
