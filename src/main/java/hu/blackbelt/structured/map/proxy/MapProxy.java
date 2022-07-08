package hu.blackbelt.structured.map.proxy;

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

    public static final String DEFAULT_ENUM_MAPPING_METHOD = "name";

    private Map<String, ?> original;
    private Map<String, Object> internal;
    private boolean immutable;
    private boolean nullSafeCollection;
    private String identifierField;
    private Class clazz;
    private final String enumMappingMethod;

    public static <T> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public static <T> Builder<T> builder(MapProxy proxy) {
        return new Builder(proxy.clazz)
                .withEnumMappingMethod(proxy.enumMappingMethod)
                .withImmutable(proxy.immutable)
                .withNullSafeCollection(proxy.nullSafeCollection)
                .withMap(proxy.original)
                .withIdentifierField(proxy.identifierField);
    }

    public static class Builder<T> {
        private final Class<T> clazz;
        private boolean immutable = false;
        private boolean nullSafeCollection = false;
        private Map<String, ?> map = Collections.emptyMap();
        private String identifierField;
        private String enumMappingMethod = DEFAULT_ENUM_MAPPING_METHOD;

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Builder<T> withImmutable(boolean immutable) {
            this.immutable = immutable;
            return this;
        }

        public Builder<T> withNullSafeCollection(boolean nullSafeCollection) {
            this.nullSafeCollection = nullSafeCollection;
            return this;
        }

        public Builder<T> withIdentifierField(String identifierField) {
            this.identifierField = identifierField;
            return this;
        }

        public Builder<T> withMap(Map<String, ?> map) {
            this.map = map;
            return this;
        }

        public Builder<T> withEnumMappingMethod(String enumMappingMethod) {
            this.enumMappingMethod = enumMappingMethod;
            return this;
        }

        public T newInstance() {
            return MapProxy.newInstance(clazz, map, immutable, nullSafeCollection, identifierField, enumMappingMethod);
        }

    }

    private static <T> T newInstance(Class<T> clazz, Map<String, ?> map, boolean immutable, boolean nullSafeCollection, String identifierField, String enumMappingMethod) {
        try {
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                    new Class[]{clazz, MapHolder.class},
                    new MapProxy(clazz, map, immutable, nullSafeCollection, identifierField, enumMappingMethod));
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

    private <T> MapProxy(Class<T> sourceClass, Map<String, ?> map, boolean immutable, boolean nullSafeCollection, String identifierField, String enumMappingMethod) throws IntrospectionException {
        original = map;
        internal = new HashMap<>(map);
        this.identifierField = identifierField;
        this.immutable = immutable;
        this.nullSafeCollection = nullSafeCollection;
        this.clazz = sourceClass;
        this.enumMappingMethod = enumMappingMethod;

        Map<String, AttributeInfo> typeInfo = null;
        try {
            typeInfo = typeInfoCache.get(sourceClass);
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
                        throw new IllegalArgumentException(String.format("The attribute %s in %s must be collection.", attrName, sourceClass.getName()));
                    }
                    internal.put(attrName, createCollectionValue((Collection) value, propertyType, parametrizedType.orElse(null), immutable, identifierField, enumMappingMethod));
                } else if (Optional.class.isAssignableFrom(propertyType) && parametrizedType.isPresent()) {
                    Class optionalType = getRawType(parametrizedType.orElseThrow(() ->
                            new IllegalStateException(String.format("Optional type attribute %s in %s class does not have generic type.", attrName, sourceClass.getName()))), 0);
                    if (value instanceof Map) {
                        if (optionalType.isInterface()) {
                            internal.put(attrName, MapProxy.builder(optionalType)
                                    .withMap((Map) value)
                                    .withImmutable(immutable)
                                    .withIdentifierField(identifierField)
                                    .withEnumMappingMethod(enumMappingMethod)
                                    .newInstance());
                        } else {
                            throw new IllegalArgumentException(String.format("The attribute %s in %s is Optional. The Optional's generic type have to be interface.", attrName, sourceClass.getName()));
                        }
                    } else if (optionalType.isAssignableFrom(value.getClass())) {
                        internal.put(attrName, value);
                    }
                } else if (value instanceof Map) {
                    if (Map.class.isAssignableFrom(propertyType)) {
                        internal.put(attrName, createMapValue((Map) value, propertyType, parametrizedType.orElse(null), immutable, identifierField, enumMappingMethod));
                    } else if (propertyType.isInterface()) {
                        internal.put(attrName, MapProxy.builder(propertyType)
                                .withMap((Map) value)
                                .withImmutable(immutable)
                                .withIdentifierField(identifierField)
                                .withEnumMappingMethod(enumMappingMethod)
                                .newInstance());
                    }
                } else if (propertyType.isAssignableFrom(value.getClass())) {
                    internal.put(attrName, value);
                } else if (propertyType.isEnum()) {
                    internal.put(attrName, createEnumValue(value, propertyType));
                } else {
                    internal.put(attrName, getValueAs(value, propertyType, "Could not assign " + value.getClass()
                            + " to " + sourceClass.getName() + "." + attrName + " as %s"));
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
        if (enumMappingMethod.equals("name")) {
            enumValue = Enum.valueOf(returnType, (String) value);
        } else {
            for (Object enumConstant : returnType.getEnumConstants()) {
                try {
                    Object enumAttributeValue = returnType.getMethod(enumMappingMethod).invoke(enumConstant);
                    if (value.equals(enumAttributeValue)) {
                        enumValue = (Enum) enumConstant;
                        break;
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    enumValue = null;
                }
            }
            if (enumValue == null) {
                throw new IllegalArgumentException(String.format("Enumeration couldn't be resolved: %s.%s via method %s()", returnType, value, enumMappingMethod));
            }
        }
        return enumValue;
    }

    private Map createMapValue(Map value, Class propertyType, ParameterizedType parameterizedType, boolean immutable, String identifierField, String enumMappingMethod) {
        Map transformedValue;

        if (parameterizedType != null) {
            final Class mapKeyType = getRawType(parameterizedType, 0);
            final Class mapValueType = getRawType(parameterizedType, 1);
            Function<Map.Entry, ?> keyMapper = mapEntryKey();
            Function<Map.Entry, ?> valueMapper = mapEntryValue();
            if (mapKeyType.isInterface()) {
                keyMapper = keyMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapKeyType, true, identifierField, enumMappingMethod)));
            }
            if (mapValueType.isInterface()) {
                valueMapper = valueMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapValueType, immutable, identifierField, enumMappingMethod)));
            }
            transformedValue = (Map) value.entrySet().stream().collect(Collectors.toMap(keyMapper, valueMapper));
            if (!immutable) {
                transformedValue = new HashMap<>(transformedValue);
            }
        } else {
            transformedValue = value;
        }
        return transformedValue;
    }

    private Collection createCollectionValue(Collection value, Class propertyType, ParameterizedType parameterizedType, boolean immutable, String identifierField, String enumMappingMethod) {
        Collection transformedValue = value;
        if (parameterizedType != null) {
            final Class collectionType = getRawType(parameterizedType, 0);
            if (collectionType.isInterface()
                    && !Map.class.isAssignableFrom(collectionType)) {
                transformedValue =  (Collection) value.stream()
                        .map(objectToMap)
                        .map(objectToMapProxyFunction(collectionType, immutable, identifierField, enumMappingMethod))
                        .collect(toCollectorForType(propertyType));
                if (!immutable) {
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

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        if ("hashCode".equals(m.getName())) {
            return proxy.toString().hashCode();
        } else if ("equals".equals(m.getName())) {
            Object obj = args[0];
            if (obj == null) {
                return false;
            } else if (obj.getClass().isAssignableFrom(clazz) || clazz.isAssignableFrom(obj.getClass())) {
                if (identifierField == null) {
                    return obj.toString().equals(proxy.toString());
                }
                Method getId = ReflectionUtil.findGetter(obj.getClass(), identifierField);
                Object thisId = internal.get(identifierField);
                Object thatId = getId.invoke(obj);
                return thisId != null && thisId.equals(thatId);
            }
            return false;
        } else if (!SET.equals(m.getName()) && m.getName().startsWith(SET)) {
            if (immutable) {
                throw new IllegalStateException("Could not call set on immutable object");
            }
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            final Object value = args[0];
            internal.put(attrName, value);
        } else if (!GET.equals(m.getName()) && m.getName().startsWith(GET)) {
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            Object value = internal.get(attrName);
            if (nullSafeCollection && value == null && Collection.class.isAssignableFrom(m.getReturnType())) {
                if (immutable) {
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
                        return Optional.ofNullable(getValueAs(value, getRawType(attributeInfo.getParameterType(), 0), "Unable to get " + attrName + " attribute as %s"));
                    }
                }
            }
            return getValueAs(value, m.getReturnType(), "Unable to get " + attrName + " attribute as %s");
        } else if (!IS.equals(m.getName()) && m.getName().startsWith(IS)) {
            String attrName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
            final Object value = internal.get(attrName);
            return getValueAs(value, boolean.class, "Unable to get " + attrName + " attribute as %s");
        } else if ("toMap".equals(m.getName())) {
            Map<Object, Object> map = new HashMap<>();
            for (Map.Entry entry : internal.entrySet()) {
                map.put(
                        mapEntryKey().andThen(objectToMapFunction()).apply(entry),
                        mapEntryValue().andThen(objectToMapFunction()).apply(entry));
            }
            return map;
        } else if ("getOriginalMap".equals(m.getName())) {
            return original;
        } else if ("toString".equals(m.getName())) {
            Map<Object, Object> map = new LinkedHashMap<>();
            for (Map.Entry entry : internal.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                map.put(entry.getKey(), entry.getValue());
            }
            return "PROXY" + map;
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

    private static Function objectToMapProxyFunction(Class type, Boolean immutable, String identifierField, String enumMappingMethod) {
        return (o) -> MapProxy.builder(type).withMap((Map) o).withIdentifierField(identifierField).withImmutable(immutable).withEnumMappingMethod(enumMappingMethod).newInstance();
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
                    return ((Enum) o).getClass().getMethod(enumMappingMethod).invoke(o);
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
