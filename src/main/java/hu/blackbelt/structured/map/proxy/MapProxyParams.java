package hu.blackbelt.structured.map.proxy;

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

    public String getIdentifierField() {
        return identifierField;
    }

    public void setIdentifierField(String identifierField) {
        this.identifierField = identifierField;
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
