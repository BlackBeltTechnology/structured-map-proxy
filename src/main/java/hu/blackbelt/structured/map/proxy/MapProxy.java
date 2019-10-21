package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    public static final String ID = "id";

    public static <T> T newInstance(Class<T> clazz) {
        return newInstance(clazz, Collections.emptyMap(), true);
    }

    public static <T> T newInstance(Class<T> clazz, Map<String, Object> map, boolean immutable) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                new Class[]{clazz, MapHolder.class},
                new MapProxy(map, immutable));
    }

    private Map<String, Object> internal;
    private boolean immutable = true;

    private MapProxy() {
        this(Collections.emptyMap(), true);
    }

    private MapProxy(Map<String, Object> map, boolean immutable) {
        internal = new HashMap<>(map);
        this.immutable = immutable;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {
        if ("equals".equals(m.getName())) {
            Object obj = args[0];
            if (obj == proxy) {
                return true;
            } else if (obj != null && (proxy.getClass().isInstance(obj) || obj.getClass().isInstance(proxy))) {
                Method getId = ReflectionUtil.findGetter(obj.getClass(), ID);
                Object thisId = internal.get(ID);
                Object thatId = getId.invoke(obj);
                return this == null ^ thatId == null ? false : thisId != null ? thisId.equals(thatId) : false;
            }
            return false;
        } else if (!SET.equals(m.getName()) && m.getName().startsWith(SET)) {
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            internal.put(attrName, args[0]);
        } else if (!GET.equals(m.getName()) && m.getName().startsWith(GET)) {
            String attrName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            final Object value = internal.get(attrName);
            final Class returnType = m.getReturnType();
            final Function transformToValueMapFunction = transformValueToMap(m, value);

            if (value instanceof Collection && Collection.class.isAssignableFrom(returnType)) {
                Type genericReturnType = m.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    final Class collectionReturnType = (Class)((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                    final Function transformMapToProxyFunction = tranformMapToProxy(collectionReturnType, immutable)

                    // Check return type is interface
                    if (collectionReturnType.isInterface()
                            && !Map.class.isAssignableFrom(collectionReturnType)) {
                        Collector collector;
                        if (List.class.isAssignableFrom(returnType)) {
                            collector = Collectors.toList();
                        } else if (Set.class.isAssignableFrom(returnType)) {
                            collector = Collectors.toSet();
                        } else {
                            collector = Collectors.toList();
                        }
                        Object ret = ((Collection) value).stream()
                                .map(transformToValueMapFunction)
                                .map(transformMapToProxyFunction)
                                .collect(collector);

                        if (!immutable) {
                            if (List.class.isAssignableFrom(returnType)) {
                                ret = new ArrayList<>((Collection) ret);
                            } else if (Set.class.isAssignableFrom(returnType)) {
                                ret = new HashSet<>((Collection) ret);
                            } else {
                                ret = new ArrayList<>((Collection) ret);
                            }
                        }
                        return ret;
                    }
                } else {
                    return value;
                }
            } else if (value instanceof Map && !Map.class.isAssignableFrom(returnType)) {
                return MapProxy.newInstance(returnType, (Map) value, true);
            } else if (value instanceof Map && Map.class.isAssignableFrom(returnType)) {
                // Check type generics same as key and value
                Type genericReturnType = m.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    Class mapKeyType = (Class)((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                    Class mapValueType = (Class)((ParameterizedType) genericReturnType).getActualTypeArguments()[1];

                    Function keyMapper = Function.identity();
                    Function valueMapper = Function.identity();

                    if (mapKeyType.isInterface()) {
                        keyMapper = transformToValueMapFunction.andThen(tranformMapToProxy(mapKeyType, immutable));
                    }
                    if (mapValueType.isInterface()) {
                        valueMapper = transformToValueMapFunction.andThen(tranformMapToProxy(mapKeyType, immutable));
                    }

                    Object ret = ((Map) value).entrySet().stream().collect(Collectors.toMap(keyMapper, valueMapper));
                    if (!immutable) {
                        return new HashMap<>((Map) ret);
                    }
                } else {
                    return value;
                }
            } else {
                return value;
            }
        } else if (!IS.equals(m.getName()) && m.getName().startsWith(IS)) {
            String attrName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
            return internal.get(attrName);
        } else if ("toMap".equals(m.getName())) {
            return Collections.unmodifiableMap(internal);
        } else if ("toString".equals(m.getName())) {
            return "PROXY" + String.valueOf(internal);
        }
        return null;
    }

    private Function transformValueToMap(Method m, Object value) {
        return (o) -> {
            if (o instanceof MapHolder) {
                return ((MapHolder) o).toMap();
            } else if (o instanceof Map) {
                return o;
            } else {
                throw new IllegalStateException("Collection element type have to " +
                        "hu.blackbelt.structured.map.proxy.MapHolder or java.util.Map Method: " +
                        m.getName() + " Value: " + value.toString());
            }
        };
    }

    private Function tranformMapToProxy(Class type, Boolean immutable) {
        return (o) -> MapProxy.newInstance(type, (Map<String, Object>) o, immutable);
    }

    private Collector getCollectionCollectorForType(Class type) {
        Collector collector = Collectors.toList();
        if (Set.class.isAssignableFrom(type)) {
            collector = Collectors.toSet();
        }
        return collector;
    }

    private Collector getMapCollectorForType(Class type, ) {
        Collector collector = null;
        if (Collection.class.isAssignableFrom(type)) {
            if (List.class.isAssignableFrom(type)) {
                collector = Collectors.toList();
            } else if (Set.class.isAssignableFrom(type)) {
                collector = Collectors.toSet();
            } else {
                collector = Collectors.toList();
            }
        }
        return collector;
    }

}
