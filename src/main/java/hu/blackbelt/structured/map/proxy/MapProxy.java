package hu.blackbelt.structured.map.proxy;

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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import hu.blackbelt.structured.map.proxy.annotation.Embedded;
import hu.blackbelt.structured.map.proxy.annotation.Key;
import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
public final class MapProxy implements InvocationHandler {

    public static final String METHOD_SET = "set";
    public static final String METHOD_GET = "get";
    public static final String METHOD_IS = "is";
    public static final String METHOD_TO_MAP = "toMap";
    public static final String METHOD_GET_ORIGINAL_MAP = "getOriginalMap";
    public static final String METHOD_TO_STRING = "toString";
    public static final String METHOD_HASH_CODE = "hashCode";
    public static final String METHOD_EQUALS = "equals";
    public static final String METHOD_PARSE = "parse";
    public static final String METHOD_ADAPT_TO = "adaptTo";
    public static final String DEFAULT_ENUM_MAPPING_METHOD = "name";

    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
            = new ImmutableMap.Builder<Class<?>, Class<?>>()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .put(void.class, Void.class)
            .build();

    private Map<String, ?> original;
    private Map<String, Object> internal;

    Class clazz;
    MapProxyParams params;

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static <T> Builder<T> builder(MapProxy proxy) {
        return new Builder(proxy.clazz)
                .withParams(proxy.params)
                .withMap((Map<String, ?>) proxy.invokeToMap());
    }

    public static class Builder<T> {
        private MapProxyParams params = new MapProxyParams();
        private final Class<T> clazz;
        private Map<String, ?> map = Collections.emptyMap();

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Builder<T> withParams(MapProxyParams params) {
            this.params.setImmutable(params.isImmutable());
            this.params.setNullSafeCollection(params.isNullSafeCollection());
            this.params.setIdentifierField(params.getIdentifierField());
            this.params.setEnumMappingMethod(params.getEnumMappingMethod());
            this.params.setMapNullToOptionalAbsent(params.isMapNullToOptionalAbsent());
            return this;
        }

        public Builder<T> withImmutable(boolean immutable) {
            this.params.setImmutable(immutable);
            return this;
        }

        public Builder<T> withNullSafeCollection(boolean nullSafeCollection) {
            this.params.setNullSafeCollection(nullSafeCollection);
            return this;
        }

        public Builder<T> withIdentifierField(String identifierField) {
            this.params.setIdentifierField(identifierField);
            return this;
        }

        public Builder<T> withMap(Map<String, ?> map) {
            this.map = map;
            return this;
        }

        public Builder<T> withBean(Object object) {
            this.map = beanToProxyMap(clazz, params, object);
            return this;
        }

        public Builder<T> withEnumMappingMethod(String enumMappingMethod) {
            this.params.setEnumMappingMethod(enumMappingMethod);
            return this;
        }

        public Builder<T> withMapNullToOptionalAbsent(boolean mapNullToOptionalAbsent) {
            this.params.setMapNullToOptionalAbsent(mapNullToOptionalAbsent);
            return this;
        }

        public T newInstance() {
            return MapProxy.newInstance(map, clazz, params);
        }

    }

    private static <T> T newInstance(Map<String, ?> map, Class clazz, MapProxyParams params) {
        try {
            Set<Class> interfaces = getWithSuperClasses(clazz, MapHolder.class);
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                    interfaces.toArray(new Class[interfaces.size()]),
                    new MapProxy(clazz, map, params));
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Could not create instance", e);
        }
    }

    private static Set<Class> getWithSuperClasses(Class ...classes) {
        Set<Class> out = new HashSet<>();
        for (Class o : classes) {
            Class subclass = o;
            out.add(subclass);
            out.addAll(getWithSuperClasses(subclass.getInterfaces()));
        }
        return out;
    }
    @AllArgsConstructor
    @Getter
    private static class AttributeInfo {
        String mapKey;
        Class propertyType;
        ParameterizedType parameterType;
        PropertyDescriptor propertyDescriptor;
        boolean composite = false;
    }

    private static CacheLoader<Class, Map<String, AttributeInfo>> typeInfoCacheLoader = new CacheLoader<Class, Map<String, AttributeInfo>>() {
        @Override
        public Map<String, AttributeInfo> load(Class sourceClass) throws Exception {
        Map<String, AttributeInfo> targetTypes = new ConcurrentHashMap<>();
        Set<Class> classesToIntrospect = getWithSuperClasses(sourceClass);
        Set<PropertyDescriptor> propertyDescriptors = new HashSet<>();
        for (Class c : classesToIntrospect) {
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(c).getPropertyDescriptors()) {
                propertyDescriptors.add(propertyDescriptor);
            }
        }

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String attrName = propertyDescriptor.getName();
            final Class propertyType = propertyDescriptor.getPropertyType();
            Optional<ParameterizedType> parametrizedType = getGetterOrSetterParameterizedType(propertyDescriptor);

            String mapKey = attrName;
            boolean composite = false;
            if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getReadMethod().getDeclaredAnnotation(Embedded.class) != null) {
                composite = true;
            }

            if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getReadMethod().getDeclaredAnnotation(Key.class) != null) {
                mapKey = propertyDescriptor.getReadMethod().getDeclaredAnnotation(Key.class).name();
            }

            targetTypes.put(attrName, new AttributeInfo(mapKey, propertyType, parametrizedType.orElse(null), propertyDescriptor, composite));
        }
        return targetTypes;
        }
    };

    private static LoadingCache<Class, Map<String, AttributeInfo>> typeInfoCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(Long.parseLong(System.getProperty("structuredMapProxyCacheExpireInSecond", "60")), TimeUnit.SECONDS)
            .build(typeInfoCacheLoader);

    private <T> MapProxy(Class clazz, Map<String, ?> map, MapProxyParams params) throws IntrospectionException {
        original = map;

        this.clazz = clazz;
        this.params = params;

        internal = toProxyMap(clazz, params, (Map<String, Object>) map);
    }

    private static Map<String, Object> toProxyMap(Class proxyClass, MapProxyParams params, Map<String, Object> map) {
        Map<String, AttributeInfo> typeInfo = null;
        try {
            typeInfo = typeInfoCache.get(proxyClass);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> proxyMap = new HashMap<>();

        typeInfo.forEach((attrName, attrInfo) -> {
            final String mapKey = attrInfo.mapKey;
            final Class proxyPropertyType = attrInfo.getPropertyType();
            Object proxyValue = null;
            if (map.containsKey(mapKey)) {
                Object value = map.get(mapKey);
                if (value instanceof Optional) {
                    value = ((Optional) value).orElse(null);
                }
                Optional<ParameterizedType> parametrizedType = Optional.ofNullable(attrInfo.getParameterType());
                if (value == null) {
                    proxyValue = null;
                } else if (Collection.class.isAssignableFrom(proxyPropertyType)) {
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException(String.format("The attribute %s in %s must be collection.", attrName, proxyClass.getName()));
                    }
                    proxyValue = toProxyCollection(proxyClass, (Collection) value, proxyPropertyType, parametrizedType.orElse(null), params);
                } else if (Optional.class.isAssignableFrom(proxyPropertyType) && parametrizedType.isPresent()) {
                    Class optionalType = getRawType(parametrizedType.orElseThrow(() ->
                            new IllegalStateException(String.format("Optional type attribute %s in %s class does not have generic type.", attrName, proxyClass.getName()))), 0);
                    if (value instanceof Map) {
                        if (optionalType.isInterface()) {
                            proxyValue = MapProxy.builder(optionalType)
                                    .withParams(params)
                                    .withMap((Map) value)
                                    .newInstance();
                        } else {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is Optional. The Optional's generic type have to be interface.", attrName, proxyClass.getName()));
                        }
                    } else if (optionalType.isAssignableFrom(value.getClass())) {
                        proxyValue = value;
                    } else if (optionalType.isEnum()) {
                        proxyValue = toEnum(params, value, optionalType);
                    }
                } else if (value instanceof Map) {
                    if (Map.class.isAssignableFrom(proxyPropertyType)) {
                        proxyValue = toProxyMap(proxyClass, (Map) value, proxyPropertyType, parametrizedType.orElse(null), params);
                    } else if (proxyPropertyType.isInterface()) {
                        proxyValue = MapProxy.builder(proxyPropertyType)
                                .withParams(params)
                                .withMap((Map) value)
                                .newInstance();
                    }
                } else if (proxyPropertyType.isEnum() && !proxyPropertyType.isAssignableFrom(value.getClass())) {
                    proxyValue = toEnum(params, value, proxyPropertyType);
                } else if (proxyPropertyType.isInterface() && !proxyPropertyType.isAssignableFrom(value.getClass())) {
                    proxyValue = MapProxy.builder(proxyPropertyType)
                            .withParams(params)
                            .withMap(beanToProxyMap(proxyPropertyType, params, value))
                            .newInstance();
                } else if (proxyPropertyType.isAssignableFrom(value.getClass())) {
                    proxyValue = value;
                } else {
                    proxyValue = getValueAs(value, proxyPropertyType, "Could not assign " + value.getClass()
                            + " to " + proxyClass.getName() + "." + attrName + " as %s");
                }
                proxyMap.put(mapKey, proxyValue);
            } else if (attrInfo != null && attrInfo.composite) {
                if (!proxyPropertyType.isInterface()) {
                    throw new IllegalArgumentException(String.format("The attribute %s in %s is not interface. The @Embedded attributes type have to be interface.", attrName, proxyClass.getName()));
                }
                proxyMap.putAll(((MapHolder) MapProxy.builder(proxyPropertyType)
                        .withParams(params)
                        .withMap(map)
                        .newInstance()).toMap());
            }
        });

        return proxyMap;
    }

    private static Map beanToProxyMap(Class clazz, MapProxyParams params, Object bean) {
        if (bean == null) {
            return null;
        }
        Map<String, AttributeInfo> targetInfos = null;
        Map<String, AttributeInfo> beanInfos = null;

        try {
            targetInfos = typeInfoCache.get(clazz);
            beanInfos = typeInfoCache.get(bean.getClass());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> map = new HashMap<>();
        Map<String, AttributeInfo> finalBeanInfos = beanInfos;

        targetInfos.forEach((attrName, attrInfo) -> {
            Object value = null;
            try {
                value = finalBeanInfos.get(attrName).propertyDescriptor.getReadMethod().invoke(bean);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (finalBeanInfos.containsKey(attrName)) {
                map.put(getKeyName(clazz, attrName), value);
            }
        });
        toProxyMap(clazz, params, map);
        return map;
    }

    private static <T> T toBeanFromObject(Object object, Class proxyClass, Class<T> targetClass, MapProxyParams params) {
        if (object == null) {
            return null;
        } else if (Map.class.isAssignableFrom(targetClass) && targetClass.isAssignableFrom(object.getClass())) {
            return (T) object;
        } else if (object instanceof MapHolder) {
            return toBeanFromMap(((MapHolder) object).toMap(), proxyClass, targetClass, params);
        } else if (object instanceof Map) {
            return toBeanFromMap((Map) object, proxyClass, targetClass, params);
        } else if (targetClass.isAssignableFrom(object.getClass())) {
            return (T) object;
        }
        return null;
    }


    private static <T> T toBeanFromMap(Map proxyMap, Class proxyClass, Class<T> target, MapProxyParams params) {
        Map<String, AttributeInfo> targetInfos = null;
        Map<String, AttributeInfo> beanInfos = null;

        try {
            targetInfos = typeInfoCache.get(proxyClass);
            beanInfos = typeInfoCache.get(target);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        Map<String, AttributeInfo> finalBeanInfos = beanInfos;

        T bean = createNewInstance(target);
        targetInfos.forEach((attrName, attrInfo) -> {
            if (proxyMap.containsKey(attrInfo.mapKey) && finalBeanInfos.containsKey(attrName) && finalBeanInfos.get(attrName).propertyDescriptor.getWriteMethod() != null) {
                Object value = proxyMap.get(attrInfo.mapKey);
                Method m = finalBeanInfos.get(attrName).propertyDescriptor.getWriteMethod();
                AttributeInfo beanInfo = finalBeanInfos.get(attrName);
                final String mapKey = attrInfo.mapKey;
                Object valueToSet = null;
                Optional<ParameterizedType> proxyParametrizedType = Optional.ofNullable(attrInfo.getParameterType());
                Class proxyPropertyType = attrInfo.getPropertyType();
                if (Optional.class.isAssignableFrom(proxyPropertyType) && proxyParametrizedType.isPresent()) {
                    getRawType(proxyParametrizedType.orElseThrow(() ->
                            new IllegalStateException(String.format("Optional type attribute %s in %s class does not have generic type.", attrName, proxyClass.getName()))), 0);
                }
                Class beanPropertyType = beanInfo.getPropertyType();
                Optional<ParameterizedType> beanParametrizedType = Optional.ofNullable(beanInfo.getParameterType());

                if (value != null) {
                    if (Collection.class.isAssignableFrom(beanInfo.getPropertyType())) {
                        if (!Collection.class.isAssignableFrom(attrInfo.getPropertyType())) {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is collection, but not in in proxy %s type.",
                                    attrName, beanInfo.getPropertyType().getName(), attrInfo.getPropertyType().getName()));
                        } else if (proxyParametrizedType.isPresent() && !beanParametrizedType.isPresent()) {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is collection without generic type, but not in in proxy %s type the generic type is %s.",
                                    attrName, beanInfo.getPropertyType().getName(), attrInfo.getPropertyType().getName(), proxyParametrizedType.get().getTypeName()));
                        } else if (!proxyParametrizedType.isPresent() && beanParametrizedType.isPresent()) {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is collection generic type is %s, but not in in proxy %s type is without generic type.",
                                    attrName, beanInfo.getPropertyType().getName(), beanParametrizedType.get().getTypeName(), attrInfo.getPropertyType().getName()));
                        } else if (proxyParametrizedType.isPresent() && beanParametrizedType.isPresent()) {
                            valueToSet = toBeanCollection(proxyClass,
                                    (Collection) value,
                                    proxyPropertyType,
                                    proxyParametrizedType.get(),
                                    beanPropertyType,
                                    beanParametrizedType.get(),
                                    params);
                        } else {
                            valueToSet = value;
                        }
                    } else if (value instanceof MapHolder) {
                        valueToSet = toBeanFromMap(((MapHolder) value).toMap(), proxyPropertyType, beanPropertyType, params);
                    } else if (value instanceof Map) {
                        if (!Map.class.isAssignableFrom(beanInfo.getPropertyType())) {
                            valueToSet = toBeanFromObject(value, proxyPropertyType, beanPropertyType, params);
                        } else {
                            if (proxyParametrizedType.isPresent() && !beanParametrizedType.isPresent()) {
                                throw new IllegalArgumentException(String.format("The attribute %s in %s is map without generic type, but not in in proxy %s type the generic type is %s.",
                                        attrName, beanInfo.getPropertyType().getName(), attrInfo.getPropertyType().getName(), proxyParametrizedType.get().getTypeName()));
                            } else if (!proxyParametrizedType.isPresent() && beanParametrizedType.isPresent()) {
                                throw new IllegalArgumentException(String.format("The attribute %s in %s is map generic type is %s, but not in in proxy %s type is without generic type.",
                                        attrName, beanInfo.getPropertyType().getName(), beanParametrizedType.get().getTypeName(), attrInfo.getPropertyType().getName()));
                            } else if (proxyParametrizedType.isPresent() && beanParametrizedType.isPresent()) {
                                valueToSet = toMapForBeanFromMapWithGenerics((Map) value, proxyParametrizedType.get(), beanParametrizedType.get(), params);
                            } else if (Map.class.isAssignableFrom(beanPropertyType)) {
                                valueToSet = (Map) value;
                            } else {
                                throw new IllegalArgumentException("sdsd");
                            }
                        }
                    } else if (beanPropertyType.isEnum()) {
                        valueToSet = toEnum(params, value, beanPropertyType);
                    } else if (beanPropertyType.isAssignableFrom(value.getClass())) {
                        valueToSet = value;
                    }

                    if (valueToSet != null) {
                        try {
                            beanInfo.propertyDescriptor.getWriteMethod().invoke(bean, valueToSet);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
        return bean;
    }

    private static Optional<ParameterizedType> getGetterOrSetterParameterizedType(PropertyDescriptor propertyDescriptor) {
        Type genericType = null;
        if (propertyDescriptor.getReadMethod() != null) {
            genericType = propertyDescriptor.getReadMethod().getGenericReturnType();
        }
        if (genericType == null && propertyDescriptor.getWriteMethod() != null && propertyDescriptor.getWriteMethod().getGenericParameterTypes().length > 0) {
            genericType = propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0];
        }
        if (genericType != null && genericType instanceof ParameterizedType) {
            return Optional.of((ParameterizedType) genericType);
        } else {
            return Optional.empty();
        }
    }

    private static Object toEnum(MapProxyParams params, Object value, Class returnType) {
        Enum enumValue = null;
        if (params.getEnumMappingMethod().equals("name")) {
            enumValue = Enum.valueOf(returnType, (String) value);
        } else {
            for (Object enumConstant : returnType.getEnumConstants()) {
                try {
                    Object enumAttributeValue = returnType.getMethod(params.getEnumMappingMethod()).invoke(enumConstant);
                    if (value.equals(enumAttributeValue)) {
                        enumValue = (Enum) enumConstant;
                        break;
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    enumValue = null;
                }
            }
            if (enumValue == null) {
                throw new IllegalArgumentException(String.format("Enumeration couldn't be resolved: %s.%s via method %s()", returnType, value, params.getEnumMappingMethod()));
            }
        }
        return enumValue;
    }

    private static Map toProxyMap(Class clazz, Map value, Class propertyType, ParameterizedType parameterizedType, MapProxyParams params) {
        Map transformedValue;

        if (parameterizedType != null) {
            final Class mapKeyType = getRawType(parameterizedType, 0);
            final Class mapValueType = getRawType(parameterizedType, 1);
            Function<Map.Entry, ?> keyMapper = mapEntryKey();
            Function<Map.Entry, ?> valueMapper = mapEntryValue();
            if (mapKeyType.isInterface()) {
                keyMapper = keyMapper.andThen(toValueFunction(clazz, params).andThen(valueToMapProxyFunction(mapKeyType, params)));
            }
            if (mapValueType.isInterface()) {
                valueMapper = valueMapper.andThen(toValueFunction(clazz, params).andThen(valueToMapProxyFunction(mapValueType, params)));
            }
            transformedValue = (Map) value.entrySet().stream().collect(Collectors.toMap(keyMapper, valueMapper));
            if (!params.isImmutable()) {
                transformedValue = new HashMap<>(transformedValue);
            }
        } else {
            transformedValue = value;
        }
        return transformedValue;
    }

    private static Map toMapForBeanFromMapWithGenerics(Map value, ParameterizedType proxyParameterizedType, ParameterizedType beanParameterizedType, MapProxyParams params) {
        Map transformedValue;

        if (proxyParameterizedType != null) {
            final Class proxyMapKeyType = getRawType(proxyParameterizedType, 0);
            final Class proxyMapValueType = getRawType(proxyParameterizedType, 1);

            final Class beanMapKeyType = getRawType(beanParameterizedType, 0);
            final Class beanMapValueType = getRawType(beanParameterizedType, 1);

            Function<Map.Entry, ?> keyMapper = mapEntryKey();
            Function<Map.Entry, ?> valueMapper = mapEntryValue();
            if (proxyMapKeyType.isInterface()) {
                keyMapper = keyMapper.andThen(toBeanFunction(proxyMapKeyType, beanMapKeyType, params));
            }
            if (proxyMapValueType.isInterface()) {
                valueMapper = valueMapper.andThen(toBeanFunction(proxyMapValueType, beanMapValueType, params));
            }
            transformedValue = (Map) value.entrySet().stream().collect(Collectors.toMap(keyMapper, valueMapper));
            if (!params.isImmutable()) {
                transformedValue = new HashMap<>(transformedValue);
            }
        } else {
            transformedValue = value;
        }
        return transformedValue;
    }

    private static Collection toProxyCollection(Class clazz, Collection value, Class propertyType, ParameterizedType parameterizedType, MapProxyParams params) {
        Collection transformedValue = value;
        if (parameterizedType != null) {
            final Class collectionType = getRawType(parameterizedType, 0);
            if (collectionType.isInterface()
                    && !Map.class.isAssignableFrom(collectionType)) {
                transformedValue =  (Collection) value.stream()
                        .map(toValueFunction(clazz, params))
                        .map(valueToMapProxyFunction(collectionType, params))
                        .collect(toCollectorForType(propertyType));
                if (!params.isImmutable()) {
                    transformedValue = mutableCollection(propertyType, transformedValue);
                }
            }
        }
        return transformedValue;
    }

     private static Collection toBeanCollection(Class proxyClass, Collection value, Class proxyPropertyType, ParameterizedType proxyParameterizedType, Class beanPropertyType, ParameterizedType beanParameterizedType, MapProxyParams params) {
        Collection transformedValue = value;
        final Class beanCollectionType = getRawType(beanParameterizedType, 0);
        final Class proxyCollectionType = getRawType(proxyParameterizedType, 0);
        if (beanCollectionType.isInterface() && !beanCollectionType.isAssignableFrom(proxyCollectionType)) {
            throw new IllegalArgumentException(String.format("Could not map bean type %s to %s", proxyCollectionType.getName(), beanCollectionType.getName()));
        } else if (beanCollectionType.isAssignableFrom(proxyCollectionType)) {
            transformedValue = value;
        } else {
            transformedValue =  (Collection) value.stream()
                    .map(v -> toBeanFromObject(v, proxyCollectionType, beanCollectionType, params))
                    .collect(toCollectorForType(beanPropertyType));
        }
         if (!params.isImmutable()) {
             transformedValue = mutableCollection(beanPropertyType, transformedValue);
         }
         return transformedValue;
    }

    private static Collection mutableCollection(Class returnType, Collection valueTransformed) {
        Collection valueRet = null;
        if (valueTransformed == null) {
            return null;
        }
        try {
            valueRet = (Collection) createNewInstance(returnType);
            valueRet.addAll(valueTransformed);
        } catch (Exception ex) {
            if (List.class.isAssignableFrom(returnType)) {
                valueRet = new ArrayList<>(valueTransformed);
            } else if (Set.class.isAssignableFrom(returnType)) {
                valueRet = new HashSet<>(valueTransformed);
            } else {
                valueRet = new ArrayList<>(valueTransformed);
            }
        }
        return valueRet;
    }

    private int invokeHashCode(Object proxy) {
        return proxy.toString().hashCode();
    }

    private boolean invokeEquals(Object proxy, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Object obj = args[0];
        if (obj == null) {
            return false;
        } else if (obj.getClass().isAssignableFrom(clazz) || clazz.isAssignableFrom(obj.getClass())) {
            if (params.getIdentifierField() == null) {
                return obj.toString().equals(proxy.toString());
            }
            Method getId = ReflectionUtil.findGetter(obj.getClass(), params.getIdentifierField());
            Object thisId = internal.get(params.getIdentifierField());
            Object thatId = getId.invoke(obj);
            return thisId != null && thisId.equals(thatId);
        }
        return false;
    }

    private void invokeSet(Method m, Object[] args) throws ExecutionException {
        if (params.isImmutable()) {
            throw new IllegalStateException("Could not call set on immutable object");
        }
        String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
        final Object value = args[0];
        AttributeInfo attributeInfo = typeInfoCache.get(clazz).get(attrName);
        if (attributeInfo == null || !attributeInfo.isComposite()) {
            internal.put(getKeyName(clazz, attrName), value);
        } else if (attributeInfo != null && attributeInfo.isComposite() && value instanceof MapHolder) {
            if (attributeInfo.getPropertyType().isInterface()) {
                MapHolder proxy = (MapHolder) MapProxy.builder(attributeInfo.getPropertyType())
                        .withParams(params)
                        .withMap(((MapHolder) value).toMap())
                        .newInstance();
                proxy.toMap().entrySet().forEach(e -> {
                    internal.put(e.getKey(), e.getValue());
                });
            } else {
                throw new IllegalArgumentException(String.format("The attribute %s in %s is not interface. The @Embedded attributes type have to be interface.", attrName, clazz.getName()));
            }
        } else {
            internal.put(getKeyName(clazz, attrName), value);
        }
    }

    private static String getKeyName(Class clazz, String attrName) {
        String mapKey = attrName;
        AttributeInfo attributeInfo = null;
        try {
            attributeInfo = typeInfoCache.get(clazz).get(attrName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (attributeInfo != null) {
            mapKey = attributeInfo.getMapKey();
        }
        return mapKey;
    }
    private Object invokeGet(Method m) {
        String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
        AttributeInfo attributeInfo = null;
        try {
            attributeInfo = typeInfoCache.get(clazz).get(attrName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (attributeInfo == null || !attributeInfo.isComposite()) {
            Object value = internal.get(getKeyName(clazz, attrName));

            if (params.isNullSafeCollection() && value == null && Collection.class.isAssignableFrom(m.getReturnType())) {
                if (params.isImmutable()) {
                    value = Collections.EMPTY_LIST;
                } else {
                    value = new ArrayList<>();
                }
            }

            if (attributeInfo != null) {
                if (Optional.class.isAssignableFrom(attributeInfo.getPropertyType())) {
                    if (value instanceof Optional) {
                        return value;
                    } else {
                        if (internal.containsKey(getKeyName(clazz, attrName)) || params.isMapNullToOptionalAbsent()) {
                            return Optional.ofNullable(getValueAs(value, getRawType(attributeInfo.getParameterType(), 0), "Unable to get " + attrName + " attribute as %s"));
                        } else {
                            return null;
                        }
                    }
                }
            }
            return getValueAs(value, m.getReturnType(), "Unable to get " + attrName + " attribute as %s");
        } else if (attributeInfo != null && attributeInfo.isComposite()) {
            if (attributeInfo.getPropertyType().isInterface()) {
                Object proxy = MapProxy.builder(attributeInfo.getPropertyType())
                        .withParams(params)
                        .withMap((Map) internal)
                        .newInstance();
                return proxy;
            } else {
                throw new IllegalArgumentException(String.format("The attribute %s in %s is not interface. The @Embedded attributes type have to be interface.", attrName, clazz.getName()));
            }
        } else {
            Object value = internal.get(getKeyName(clazz, attrName));
            return getValueAs(value, m.getReturnType(), "Unable to get " + attrName + " attribute as %s");
        }
    }

    private boolean invokeIs(Method m) throws ExecutionException {
        String attrName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
        final Object value = internal.get(getKeyName(clazz, attrName));
        return (Boolean) getValueAs(value, boolean.class, "Unable to get " + attrName + " attribute as %s");
    }

    private Object invokeToMap() {
        Map<Object, Object> map = new HashMap<>();
        for (Map.Entry entry : internal.entrySet()) {
            map.put(
                    mapEntryKey().andThen(keyName(clazz)).andThen(toValueFunction(clazz, params)).apply(entry),
                    mapEntryValue().andThen(toValueFunction(clazz, params)).apply(entry));
        }
        return map;
    }

    private <T> T invokeAdaptTo(Object proxy, Object args[]) {
        if (args[0] == null || !(args[0] instanceof Class)) {
            throw new IllegalArgumentException("Adapter target is mandatory and have to be a class");
        }

        Class<T> targetClazz = (Class<T>) args[0];
        if (targetClazz.equals(Map.class)) {
            return (T) invokeToMap();
        } else if (targetClazz.isInterface()) {
            return MapProxy.builder(targetClazz)
                    .withMap((Map<String, ?>) invokeToMap())
                    .withParams(params)
                    .newInstance();
        } else {
            return toBeanFromObject(proxy, clazz, targetClazz, params);
        }
    }

    private String invokeToString() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (Map.Entry entry : internal.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            map.put(entry.getKey(), entry.getValue());
        }
        return "PROXY" + map;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        if (METHOD_HASH_CODE.equals(m.getName())) {
            return invokeHashCode(proxy);
        } else if (METHOD_EQUALS.equals(m.getName())) {
            return invokeEquals(proxy, args);
        } else if (!METHOD_SET.equals(m.getName()) && m.getName().startsWith(METHOD_SET)) {
            invokeSet(m, args);
        } else if (!METHOD_GET.equals(m.getName()) && m.getName().startsWith(METHOD_GET)) {
            return invokeGet(m);
        } else if (!METHOD_IS.equals(m.getName()) && m.getName().startsWith(METHOD_IS)) {
            return invokeIs(m);
        } else if (METHOD_TO_MAP.equals(m.getName())) {
            return invokeToMap();
        } else if (METHOD_GET_ORIGINAL_MAP.equals(m.getName())) {
            return original;
        } else if (METHOD_TO_STRING.equals(m.getName())) {
            return invokeToString();
        } else if (METHOD_ADAPT_TO.equals(m.getName())) {
            return invokeAdaptTo(proxy, args);
        }
        return null;
    }

    private static Object getValueAs(Object value, Class clazz, String errorPattern) {
        final Class valueClass = value != null ? value.getClass() : Void.class;
        final Optional<Class> valuePrimitiveClass = PRIMITIVES_TO_WRAPPERS.entrySet().stream()
                .filter(e -> Objects.equals(valueClass, e.getValue()))
                .map(e -> (Class) e.getKey())
                .findAny();

        if (value == null || clazz.isAssignableFrom(value.getClass()) || valuePrimitiveClass.isPresent() && clazz.isAssignableFrom(valuePrimitiveClass.get())) {
            return value;
        }

        try {
            return clazz.getConstructor(valueClass).newInstance(value);
        } catch (Exception ex) {
            log.debug("Constructor not found to convert value");
        }
        if (valuePrimitiveClass.isPresent()) {
            try {
                return clazz.getConstructor(valuePrimitiveClass.get()).newInstance(value);
            } catch (Exception ex) {
                log.debug("Constructor not found to convert primitive value");
            }
        }
        try {
            return clazz.getMethod(METHOD_PARSE, valueClass).invoke(null, value);
        } catch (Exception ex) {
            log.debug("Parse method not found to convert value");
        }
        if (valuePrimitiveClass.isPresent()) {
            try {
                return clazz.getMethod(METHOD_PARSE, valuePrimitiveClass.get()).invoke(null, value);
            } catch (Exception ex) {
                log.debug("Parse method not found to convert primitive value");
            }
        }

        throw new IllegalStateException(MessageFormat.format(errorPattern, clazz.getName()));
    }

    private static Function valueToMapProxyFunction(Class type, MapProxyParams params) {
        return (o) -> {
            if (o instanceof Map) {
                return MapProxy.builder(type).withParams(params).withMap((Map) o).newInstance();
            } else {
                return MapProxy.builder(type).withParams(params).withBean(o).newInstance();
            }
        };
    }

    private static Collector toCollectorForType(Class<?> type) {
        Collector collector;
        if (Set.class.isAssignableFrom(type)) {
            collector = Collectors.toSet();
        } else {
            collector = Collectors.toList();
        }
        return collector;
    }

    private static Class getRawType(ParameterizedType parameterizedType, int argnum) {
        Type collectionGenericType = parameterizedType.getActualTypeArguments()[argnum];
        if (collectionGenericType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) collectionGenericType).getRawType();
        } else {
            return (Class) parameterizedType.getActualTypeArguments()[argnum];
        }
    }

    private static Function<Object, Object> keyName(Class clazz) {
        return (o) -> {
            if (o instanceof String) {
                return getKeyName(clazz, (String) o);
            }
            return o;
        };
    }

    private static Function<Object, Object> toValueFunction(Class proxyClass, MapProxyParams params) {
        return (o) -> toValue(proxyClass, o, params);
    }

    private static Object toValue(Class proxyClass, Object value, MapProxyParams params) {
        if (value instanceof MapHolder) {
            return ((MapHolder) value).toMap();
        } else if (value instanceof Map) {
            return ((Map) value).entrySet().stream().collect(
                    Collectors.toMap(
                            mapEntryKey().andThen(keyName(proxyClass)).andThen(toValueFunction(proxyClass, params)),
                            mapEntryValue().andThen(toValueFunction(proxyClass, params))
                    ));
        } else if (value instanceof Collection) {
            return ((Collection) value).stream().map(v -> {
                if (v instanceof MapHolder) {
                    return ((MapHolder) v).toMap();
                } else {
                    return v;
                }
            }).collect(Collectors.toList());
        } else if (value instanceof Enum) {
            try {
                return ((Enum) value).getClass().getMethod(params.getEnumMappingMethod()).invoke(value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            return value;
        }
    }

    private static Function<Object, Object> toBeanFunction(Class proxyClazz, Class targetClazz, MapProxyParams params) {
        return (o) -> toBeanFromObject(o, proxyClazz, targetClazz, params);
    }

    private static Function<Map.Entry, ?> mapEntryKey() {
        return Map.Entry::getKey;
    }

    private static Function<Map.Entry, ?> mapEntryValue() {
        return Map.Entry::getValue;
    }

    private static <T> T createNewInstance(Class<T> clazz, Object...values) {
        T bean = null;
        try {
            bean = (T) clazz.getConstructor().newInstance(values);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Default constructor for " + clazz.getName() + " not found");
        }
        return bean;
    }

}
