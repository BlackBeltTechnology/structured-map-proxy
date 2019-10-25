package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private <T> MapProxy(Class<T> clazz, Map<String, ?> map, boolean immutable, String identifierField) throws IntrospectionException {
        original = map;
        internal = new HashMap<>(map);
        this.identifierField = identifierField;
        this.immutable = immutable;
        this.clazz = clazz;

        for (PropertyDescriptor propertyDescriptor :
                Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
            String attrName = propertyDescriptor.getName();
            if (internal.containsKey(attrName)) {
                Object value = internal.get(attrName);
                final Class returnType = propertyDescriptor.getReadMethod().getReturnType();
                Type genericReturnType = propertyDescriptor.getReadMethod().getGenericReturnType();

                final Function transformToValueMapFunction = objectToMap(attrName, value);
                if (Collection.class.isAssignableFrom(returnType)) {
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException("The attribute " + attrName
                                + " in " + clazz.getName() + " have to be collection");
                    }
                    if (genericReturnType instanceof ParameterizedTypeImpl) {
                        final Class collectionReturnType = getRawType((ParameterizedType) genericReturnType, 0);

                        // Check return type is interface
                        if (collectionReturnType.isInterface()
                                && !Map.class.isAssignableFrom(collectionReturnType)) {
                            Object valueTransformed = ((Collection) value).stream()
                                    .map(transformToValueMapFunction)
                                    .map(objectToMapProxyFunction(collectionReturnType, immutable, identifierField))
                                    .collect(toCollectorForType(returnType));

                            if (!immutable) {
                                if (List.class.isAssignableFrom(returnType)) {
                                    valueTransformed = new ArrayList<>((Collection) valueTransformed);
                                } else if (Set.class.isAssignableFrom(returnType)) {
                                    valueTransformed = new HashSet<>((Collection) valueTransformed);
                                } else {
                                    valueTransformed = new ArrayList<>((Collection) valueTransformed);
                                }
                            }
                            internal.put(attrName, valueTransformed);
                        } else {
                            internal.put(attrName, value);
                        }
                    } else {
                        internal.put(attrName, value);
                    }
                } else if (value instanceof Map && returnType.isInterface() && !Map.class.isAssignableFrom(returnType)) {
                    internal.put(attrName, MapProxy.newInstance(returnType, (Map) value, immutable, identifierField));
                } else if (value instanceof Map && Map.class.isAssignableFrom(returnType)) {
                    // Check type generics same as key and value
                    if (genericReturnType instanceof ParameterizedType) {
                        final Class mapKeyType = getRawType((ParameterizedType) genericReturnType, 0);
                        final Class mapValueType = getRawType((ParameterizedType) genericReturnType, 1);

                        Function<Map.Entry, ?> keyMapper = mapEntryKey();
                        Function<Map.Entry, ?> valueMapper = mapEntryValue();

                        if (mapKeyType.isInterface()) {
                            keyMapper =  keyMapper
                                    .andThen(transformToValueMapFunction
                                            .andThen(objectToMapProxyFunction(mapKeyType, true, identifierField)));
                        }
                        if (mapValueType.isInterface()) {
                            valueMapper = valueMapper
                                    .andThen(transformToValueMapFunction
                                            .andThen(objectToMapProxyFunction(mapValueType, immutable, identifierField)));
                        }

                        Object valueTransformed = ((Map) value).entrySet().stream()
                                .collect(Collectors.toMap(keyMapper, valueMapper));

                        if (!immutable) {
                            valueTransformed = new HashMap<>((Map) valueTransformed);
                        }
                        internal.put(attrName, valueTransformed);
                    } else {
                        internal.put(attrName, value);
                    }
                } else if (returnType.isAssignableFrom(value.getClass())) {
                    internal.put(attrName, value);
                } else {
                    throw new IllegalArgumentException("Could not assigng " + value.getClass()
                            + " to " + clazz.getName() + "." + attrName);
                }
            }
        }
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
            return internal.entrySet().stream().collect(
                    Collectors.toMap(
                            mapEntryKey().andThen(objectToMapFuntion()),
                            mapEntryValue().andThen(objectToMapFuntion())
                    ));
        } else if ("getOriginalMap".equals(m.getName())) {
            return original;
        } else if ("toString".equals(m.getName())) {
            return "PROXY" + String.valueOf(internal.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new)));
        }
        return null;
    }

    private Function objectToMap(String name, Object value) {
        return (o) -> {
            if (o instanceof MapHolder) {
                return ((MapHolder) o).toMap();
            } else if (o instanceof Map) {
                return o;
            } else {
                throw new IllegalStateException("Collection element type have to " +
                        "hu.blackbelt.structured.map.proxy.MapHolder or java.util.Map Name: " +
                        name + " Value: " + value.toString());
            }
        };
    }

    private static Function objectToMapProxyFunction(Class type, Boolean immutable, String identifierField) {
        return (o) -> MapProxy.newInstance(type, (Map) o, immutable, identifierField);
    }

    private static Collector toCollectorForType(Class type) {
        Collector collector = Collectors.toList();
        if (Set.class.isAssignableFrom(type)) {
            collector = Collectors.toSet();
        }
        return collector;
    }

    private static Class getRawType(ParameterizedType parameterizedType, int argnum) {
        Type collectionGenericType = parameterizedType.getActualTypeArguments()[argnum];
        if (collectionGenericType instanceof ParameterizedTypeImpl) {
            return ((ParameterizedTypeImpl) collectionGenericType).getRawType();
        } else {
            return (Class) parameterizedType.getActualTypeArguments()[argnum];
        }
    }

    private static Function<Object, Object> objectToMapFuntion() {
        return (o) -> {
            if (o instanceof MapHolder) {
                return ((MapHolder) o).toMap();
            } else if (o instanceof Map) {
                return ((Map) o).entrySet().stream().collect(
                        Collectors.toMap(
                                mapEntryKey().andThen(objectToMapFuntion()),
                                mapEntryValue().andThen(objectToMapFuntion())
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
        return (e) -> e.getKey();
    }

    private static Function<Map.Entry, ?> mapEntryValue() {
        return (e) -> e.getValue();
    }

}
