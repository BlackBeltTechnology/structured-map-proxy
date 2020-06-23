package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class MapProxy implements InvocationHandler {

    public static final String SET = "set";
    public static final String GET = "get";
    public static final String IS = "is";


    public static <T> T newImmutableInstance(Class<T> clazz) {
        return newInstance(clazz, Collections.emptyMap(), true, null);
    }

    public static <T> T newInstance(Class<T> clazz) {
        return newInstance(clazz, Collections.emptyMap(), false, null);
    }

    public static <T> T newInstance(Class<T> clazz, Map<String, ?> map, boolean immutable, String identifierField) {
        try {
            return (T) java.lang.reflect.Proxy.newProxyInstance(
                    new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                    new Class[]{clazz, MapHolder.class},
                    new MapProxy(clazz, map, immutable, identifierField));
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Could not create instance", e);
        }
    }


    private Map<String, ?> original;
    private Map<String, Object> internal;
    private boolean immutable = true;
    private String identifierField = null;
    private Class clazz;

    private <T> MapProxy(Class<T> sourceClass, Map<String, ?> map, boolean immutable, String identifierField) throws IntrospectionException {
        original = map;
        internal = new HashMap<>(map);
        this.identifierField = identifierField;
        this.immutable = immutable;
        this.clazz = sourceClass;

        for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(sourceClass).getPropertyDescriptors()) {
            String attrName = propertyDescriptor.getName();
            if (internal.containsKey(attrName)) {
                Object value = internal.get(attrName);
                final Class returnType = propertyDescriptor.getReadMethod().getReturnType();
                Type genericReturnType = propertyDescriptor.getReadMethod().getGenericReturnType();
                if (value == null) {
                    internal.put(attrName, null);
                } else if (Collection.class.isAssignableFrom(returnType)) {
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException(String.format("The attribute %s in %s must be collection.", attrName, sourceClass.getName()));
                    }
                    internal.put(attrName, createCollectionValue((Collection) value, returnType, genericReturnType, immutable, identifierField));
                } else if (value instanceof Map) {
                    if (Map.class.isAssignableFrom(returnType)) {
                        internal.put(attrName, createMapValue((Map) value, genericReturnType, immutable, identifierField));
                    } else if (returnType.isInterface()) {
                        internal.put(attrName, MapProxy.newInstance(returnType, (Map) value, immutable, identifierField));
                    }
                } else if (returnType.isAssignableFrom(value.getClass())) {
                    internal.put(attrName, value);
                } else {
                    throw new IllegalArgumentException("Could not assigng " + value.getClass()
                            + " to " + sourceClass.getName() + "." + attrName);
                }
            }
        }
    }

    private Map createMapValue(Map value, Type genericReturnType, boolean immutable, String identifierField) {
        Map transformedValue;
        if (genericReturnType instanceof ParameterizedType) {
            final Class mapKeyType = getRawType((ParameterizedType) genericReturnType, 0);
            final Class mapValueType = getRawType((ParameterizedType) genericReturnType, 1);
            Function<Map.Entry, ?> keyMapper = mapEntryKey();
            Function<Map.Entry, ?> valueMapper = mapEntryValue();
            if (mapKeyType.isInterface()) {
                keyMapper = keyMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapKeyType, true, identifierField)));
            }
            if (mapValueType.isInterface()) {
                valueMapper = valueMapper.andThen(objectToMap.andThen(objectToMapProxyFunction(mapValueType, immutable, identifierField)));
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

    private Collection createCollectionValue(Collection value, Class returnType, Type genericReturnType, boolean immutable, String identifierField) {
        Collection transformedValue = value;
        if (genericReturnType instanceof ParameterizedType) {
            final Class collectionReturnType = getRawType((ParameterizedType) genericReturnType, 0);
            if (collectionReturnType.isInterface()
                    && !Map.class.isAssignableFrom(collectionReturnType)) {
                transformedValue = (Collection) ((Collection) value).stream()
                        .map(objectToMap)
                        .map(objectToMapProxyFunction(collectionReturnType, immutable, identifierField))
                        .collect(toCollectorForType(returnType));
                if (!immutable) {
                    transformedValue = createMutableCollection(returnType, transformedValue);
                }
            }
        }
        return transformedValue;
    }

    private Collection createMutableCollection(Class returnType, Collection valueTransformed) {
        if (List.class.isAssignableFrom(returnType)) {
            valueTransformed = new ArrayList<>(valueTransformed);
        } else if (Set.class.isAssignableFrom(returnType)) {
            valueTransformed = new HashSet<>(valueTransformed);
        } else {
            valueTransformed = new ArrayList<>(valueTransformed);
        }
        return valueTransformed;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {
        if ("hashCode".equals(m.getName())) {
            return proxy.toString().hashCode();
        } else if ("equals".equals(m.getName())) {
            Object obj = args[0];
            if (obj == null) {
                return false;
            } else if (obj.getClass().isAssignableFrom(clazz) || clazz.isAssignableFrom(obj.getClass())) {
                if (identifierField != null) {
                    Method getId = ReflectionUtil.findGetter(obj.getClass(), identifierField);
                    Object thisId = internal.get(identifierField);
                    Object thatId = getId.invoke(obj);
                    return this == null ^ thatId == null ? false : thisId != null ? thisId.equals(thatId) : false;
                } else if (obj.toString().equals(proxy.toString())) {
                    return true;
                }
            }
            return false;
        } else if (!SET.equals(m.getName()) && m.getName().startsWith(SET)) {
            if (immutable) {
                throw new IllegalStateException("Could not call set on immutable object");
            }
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            internal.put(attrName, args[0]);
        } else if (!GET.equals(m.getName()) && m.getName().startsWith(GET)) {
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            final Object value = internal.get(attrName);
            return value;
        } else if (!IS.equals(m.getName()) && m.getName().startsWith(IS)) {
            String attrName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
            return internal.get(attrName);
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
            return "PROXY" + map.toString();
        }
        return null;
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

    private static Function objectToMapProxyFunction(Class type, Boolean immutable, String identifierField) {
        return (o) -> MapProxy.newInstance(type, (Map) o, immutable, identifierField);
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

    private static Function<Object, Object> objectToMapFunction() {
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
