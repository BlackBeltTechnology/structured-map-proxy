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
import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
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

    public static final String SET = "set";
    public static final String GET = "get";
    public static final String IS = "is";
    public static final String TO_MAP = "toMap";
    public static final String GET_ORIGINAL_MAP = "getOriginalMap";
    public static final String TO_STRING = "toString";
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
    public static final String HASH_CODE = "hashCode";
    public static final String EQUALS = "equals";


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
                .withMap(proxy.original);
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
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                    new Class[]{clazz, MapHolder.class},
                    new MapProxy(clazz, map, params));
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Could not create instance", e);
        }
    }

    @AllArgsConstructor
    @Getter
    private static class AttributeInfo {
        Class propertyType;
        ParameterizedType parameterType;
        PropertyDescriptor propertyDescriptor;
    }

    private static CacheLoader<Class, Map<String, AttributeInfo>> typeInfoCacheLoader = new CacheLoader<Class, Map<String, AttributeInfo>>() {
        @Override
        public Map<String, AttributeInfo> load(Class sourceClass) throws Exception {
            Map<String, AttributeInfo> targetTypes = new ConcurrentHashMap<>();
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(sourceClass).getPropertyDescriptors()) {
                String attrName = propertyDescriptor.getName();
                final Class propertyType = propertyDescriptor.getPropertyType();
                Optional<ParameterizedType> parametrizedType = getGetterOrSetterParameterizedType(propertyDescriptor);
                targetTypes.put(attrName, new AttributeInfo(propertyType, parametrizedType.orElse(null), propertyDescriptor));
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
        internal = new HashMap<>(map);

        this.clazz = clazz;
        this.params = params;

        Map<String, AttributeInfo> typeInfo = null;
        try {
            typeInfo = typeInfoCache.get(clazz);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        typeInfo.forEach((attrName, attrInfo) -> {
            if (internal.containsKey(attrName)) {
                Object value = internal.get(attrName);
                if (value instanceof Optional) {
                    value = ((Optional) value).orElse(null);
                }
                final Class propertyType = attrInfo.getPropertyType();
                Optional<ParameterizedType> parametrizedType = Optional.ofNullable(attrInfo.getParameterType());
                if (value == null) {
                    internal.put(attrName, null);
                } else if (Collection.class.isAssignableFrom(propertyType)) {
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException(String.format("The attribute %s in %s must be collection.", attrName, clazz.getName()));
                    }
                    internal.put(attrName, createCollectionValue((Collection) value, propertyType, parametrizedType.orElse(null), params));
                } else if (Optional.class.isAssignableFrom(propertyType) && parametrizedType.isPresent()) {
                    Class optionalType = getRawType(parametrizedType.orElseThrow(() ->
                            new IllegalStateException(String.format("Optional type attribute %s in %s class does not have generic type.", attrName, clazz.getName()))), 0);
                    if (value instanceof Map) {
                        if (optionalType.isInterface()) {
                            internal.put(attrName, MapProxy.builder(optionalType)
                                    .withParams(params)
                                    .withMap((Map) value)
                                    .newInstance());
                        } else {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is Optional. The Optional's generic type have to be interface.", attrName, clazz.getName()));
                        }
                    } else if (optionalType.isEnum()) {
                        internal.put(attrName, createEnumValue(value, optionalType));
                    } else if (optionalType.isAssignableFrom(value.getClass())) {
                        internal.put(attrName, value);
                    }
                } else if (value instanceof Map) {
                    if (Map.class.isAssignableFrom(propertyType)) {
                        internal.put(attrName, createMapValue((Map) value, propertyType, parametrizedType.orElse(null), params));
                    } else if (propertyType.isInterface()) {
                        internal.put(attrName, MapProxy.builder(propertyType)
                                .withParams(params)
                                .withMap((Map) value)
                                .newInstance());
                    }
                } else if (propertyType.isAssignableFrom(value.getClass())) {
                    internal.put(attrName, value);
                } else if (propertyType.isEnum()) {
                    internal.put(attrName, createEnumValue(value, propertyType));
                } else {
                    internal.put(attrName, getValueAs(value, propertyType, "Could not assign " + value.getClass()
                            + " to " + clazz.getName() + "." + attrName + " as %s"));
                }
            }
        });
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

    private Object createEnumValue(Object value, Class returnType) {
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

    private Map createMapValue(Map value, Class propertyType, ParameterizedType parameterizedType, MapProxyParams params) {
        Map transformedValue;

        if (parameterizedType != null) {
            final Class mapKeyType = getRawType(parameterizedType, 0);
            final Class mapValueType = getRawType(parameterizedType, 1);
            Function<Map.Entry, ?> keyMapper = mapEntryKey();
            Function<Map.Entry, ?> valueMapper = mapEntryValue();
            if (mapKeyType.isInterface()) {
                keyMapper = keyMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapKeyType, params)));
            }
            if (mapValueType.isInterface()) {
                valueMapper = valueMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapValueType, params)));
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

    private Collection createCollectionValue(Collection value, Class propertyType, ParameterizedType parameterizedType, MapProxyParams params) {
        Collection transformedValue = value;
        if (parameterizedType != null) {
            final Class collectionType = getRawType(parameterizedType, 0);
            if (collectionType.isInterface()
                    && !Map.class.isAssignableFrom(collectionType)) {
                transformedValue =  (Collection) value.stream()
                        .map(objectToMap)
                        .map(objectToMapProxyFunction(collectionType, params))
                        .collect(toCollectorForType(propertyType));
                if (!params.isImmutable()) {
                    transformedValue = createMutableCollection(propertyType, transformedValue);
                }
            }
        }
        return transformedValue;
    }

    private Collection createMutableCollection(Class returnType, Collection valueTransformed) {
        if (valueTransformed == null) {
            return null;
        }
        try {
            Constructor constructor = returnType.getConstructor();
            valueTransformed = (Collection) constructor.newInstance();
            valueTransformed.addAll(valueTransformed);
        } catch (Exception ex) {
            if (List.class.isAssignableFrom(returnType)) {
                valueTransformed = new ArrayList<>(valueTransformed);
            } else if (Set.class.isAssignableFrom(returnType)) {
                valueTransformed = new HashSet<>(valueTransformed);
            } else {
                valueTransformed = new ArrayList<>(valueTransformed);
            }
        }
        return valueTransformed;
    }

    private int proxyHashCode(Object proxy) {
        return proxy.toString().hashCode();
    }

    private boolean proxyEquals(Object proxy, Object[] args) throws InvocationTargetException, IllegalAccessException {
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

    private void proxySet(Method m, Object[] args) {
        if (params.isImmutable()) {
            throw new IllegalStateException("Could not call set on immutable object");
        }
        String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
        final Object value = args[0];
        internal.put(attrName, value);
    }

    private Object proxyGet(Method m) throws ExecutionException {
        String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
        Object value = internal.get(attrName);
        if (params.isNullSafeCollection() && value == null && Collection.class.isAssignableFrom(m.getReturnType())) {
            if (params.isImmutable()) {
                value = Collections.EMPTY_LIST;
            } else {
                value = new ArrayList<>();
            }
        }
        AttributeInfo attributeInfo = typeInfoCache.get(clazz).get(attrName);
        if (attributeInfo != null) {
            if (Optional.class.isAssignableFrom(attributeInfo.getPropertyType())) {
                if (value instanceof Optional) {
                    return value;
                } else {
                    if (internal.containsKey(attrName) || params.isMapNullToOptionalAbsent()) {
                        return Optional.ofNullable(getValueAs(value, getRawType(attributeInfo.getParameterType(), 0), "Unable to get " + attrName + " attribute as %s"));
                    } else {
                        return null;
                    }
                }
            }
        }
        return getValueAs(value, m.getReturnType(), "Unable to get " + attrName + " attribute as %s");
    }

    private boolean proxyIs(Method m) {
        String attrName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
        final Object value = internal.get(attrName);
        return (Boolean) getValueAs(value, boolean.class, "Unable to get " + attrName + " attribute as %s");
    }

    private Object proxyToMap() {
        Map<Object, Object> map = new HashMap<>();
        for (Map.Entry entry : internal.entrySet()) {
            map.put(
                    mapEntryKey().andThen(objectToMapFunction()).apply(entry),
                    mapEntryValue().andThen(objectToMapFunction()).apply(entry));
        }
        return map;
    }

    private String proxyToString() {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (Map.Entry entry : internal.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            map.put(entry.getKey(), entry.getValue());
        }
        return "PROXY" + map;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        if (HASH_CODE.equals(m.getName())) {
            return proxyHashCode(proxy);
        } else if (EQUALS.equals(m.getName())) {
            return proxyEquals(proxy, args);
        } else if (!SET.equals(m.getName()) && m.getName().startsWith(SET)) {
            proxySet(m, args);
        } else if (!GET.equals(m.getName()) && m.getName().startsWith(GET)) {
            return proxyGet(m);
        } else if (!IS.equals(m.getName()) && m.getName().startsWith(IS)) {
            return proxyIs(m);
        } else if (TO_MAP.equals(m.getName())) {
            return proxyToMap();
        } else if (GET_ORIGINAL_MAP.equals(m.getName())) {
            return original;
        } else if (TO_STRING.equals(m.getName())) {
            return proxyToString();
        }
        return null;
    }

    private Object getValueAs(Object value, Class clazz, String errorPattern) {
        final Class valueClass = value != null ? value.getClass() : Void.class;
        final Optional<Class> valuePrimitiveClass = PRIMITIVES_TO_WRAPPERS.entrySet().stream()
                .filter(e -> Objects.equals(valueClass, e.getValue()))
                .map(e -> (Class) e.getKey())
                .findAny();

        if (value == null || clazz.isAssignableFrom(value.getClass()) || valuePrimitiveClass.isPresent() && clazz.isAssignableFrom(valuePrimitiveClass.get())) {
            return value;
        }

        try {
            clazz.getConstructor(valueClass).newInstance(value);
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
            return clazz.getMethod("parse", valueClass).invoke(null, value);
        } catch (Exception ex) {
            log.debug("Parse method not found to convert value");
        }
        if (valuePrimitiveClass.isPresent()) {
            try {
                return clazz.getMethod("parse", valuePrimitiveClass.get()).invoke(null, value);
            } catch (Exception ex) {
                log.debug("Parse method not found to convert primitive value");
            }
        }

        throw new IllegalStateException(MessageFormat.format(errorPattern, clazz.getName()));
    }

    private Function<Object, Map> objectToMap =
            (o) -> {
                if (o instanceof MapHolder) {
                    return ((MapHolder) o).toMap();
                } else if (o instanceof Map) {
                    return (Map) o;
                } else {
                    throw new IllegalStateException("Collection element type have to be " +
                            "hu.blackbelt.structured.map.proxy.MapHolder or java.util.Map ");
                }
            };

    private static Function objectToMapProxyFunction(Class type, MapProxyParams params) {
        return (o) -> MapProxy.builder(type).withParams(params).withMap((Map) o).newInstance();
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

    private Function<Object, Object> objectToMapFunction() {
        return (o) -> {
            if (o instanceof MapHolder) {
                return ((MapHolder) o).toMap();
            } else if (o instanceof Map) {
                return ((Map) o).entrySet().stream().collect(
                        Collectors.toMap(
                                mapEntryKey().andThen(objectToMapFunction()),
                                mapEntryValue().andThen(objectToMapFunction())
                        ));
            } else if (o instanceof Collection) {
                return ((Collection) o).stream().map(v -> {
                    if (v instanceof MapHolder) {
                        return ((MapHolder) v).toMap();
                    } else {
                        return v;
                    }
                }).collect(Collectors.toList());
            } else if (o instanceof Enum) {
                try {
                    return ((Enum) o).getClass().getMethod(params.getEnumMappingMethod()).invoke(o);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                return o;
            }
        };
    }

    private static Function<Map.Entry, ?> mapEntryKey() {
        return Map.Entry::getKey;
    }

    private static Function<Map.Entry, ?> mapEntryValue() {
        return Map.Entry::getValue;
    }

}
