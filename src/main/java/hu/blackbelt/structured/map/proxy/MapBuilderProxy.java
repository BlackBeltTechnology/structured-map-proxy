package hu.blackbelt.structured.map.proxy;

import com.google.common.collect.ImmutableList;
import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public final class MapBuilderProxy implements InvocationHandler {

    Object internal;
    String prefix;

    public static <T, B> T newInstance(Class<T> builderClass, Class<B> targetClass, String builderMethodPrefix) {

        return (T) java.lang.reflect.Proxy.newProxyInstance(
                builderClass.getClassLoader(),
                new Class[] { builderClass },
                new MapBuilderProxy(MapProxy.newInstance(targetClass), builderMethodPrefix));
    }

    public static <T, B> T newInstance(Class<T> builderClass, Class<B> targetClass) {
        return newInstance(builderClass, targetClass, null);
    }

    private MapBuilderProxy(Object target, String builderMethodPrefix) {
        this.internal = target;
        this.prefix = builderMethodPrefix;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {
        if (m.getName().startsWith("build")) {
            return internal;
        } else {
            String attrName = Character.toUpperCase(m.getName().charAt(0)) + m.getName().substring(1);
            if (prefix != null && !prefix.equals("")) {
                attrName = Character.toUpperCase(m.getName().charAt(prefix.length())) + m.getName().substring(prefix.length() + 1);
            }
            Method setterMethod = ReflectionUtil.findSetter(internal.getClass(), attrName);
            Object[] value = null;
            if (args[0] instanceof Object[]) {
                value = new Object[]{ImmutableList.copyOf((Object[]) args[0])};
            } else {
                value = new Object[]{args[0]};
            }
            setterMethod.invoke(internal, value);
        }
        return proxy;
    }
}
