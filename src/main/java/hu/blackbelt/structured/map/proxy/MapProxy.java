package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MapProxy implements InvocationHandler {

    public static final String SET = "set";
    public static final String GET = "get";
    public static final String IS = "is";
    public static final String ID = "id";

    public static <T> T newInstance(Class<T> clazz) {
        return newInstance(clazz, Collections.emptyMap());
    }

    public static <T> T newInstance(Class<T> clazz, Map<String, Object> map) {
        // TODO - control if map is cloned or not
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                new CompositeClassLoader(clazz.getClassLoader(), MapHolder.class.getClassLoader()),
                new Class[]{clazz, MapHolder.class},
                new MapProxy(map));
    }

    private Map<String, Object> internal;

    private MapProxy() {
        this(Collections.emptyMap());
    }

    private MapProxy(Map<String, Object> map) {
        internal = new HashMap<>(map);
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
            // TODO: list, set, bag proxy support, get generic from returnType
            if (value instanceof Map && !Map.class.isAssignableFrom(returnType)) {
                return MapProxy.newInstance(returnType, (Map)value);
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
}
