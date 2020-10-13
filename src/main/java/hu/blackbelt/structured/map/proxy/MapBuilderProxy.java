package hu.blackbelt.structured.map.proxy;

import com.google.common.collect.ImmutableList;
import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;

public final class MapBuilderProxy implements InvocationHandler {

    Object internal;
    String prefix;

    public static <B, T> Builder<B, T> builder(Class<B> builderClass, Class<T> targetClass) {
        return new MapBuilderProxy.Builder<>(builderClass, targetClass);
    }

    public static <B, T> Builder<B, T> builder(Class<B> builderClass, T targetInstance) {
        return new MapBuilderProxy.Builder<B, T>(builderClass, (Class<T>) targetInstance.getClass()).withTargetInstance(targetInstance);
    }

    public static class Builder<B, T> {

        private final Class<B> builderClass;
        private final Class<T> targetClass;
        private T targetInstance;
        private String builderMethodPrefix;
        private String enumMappingMethod;

        private Builder(Class<B> builderClass, Class<T> targetClass) {
            this.builderClass = builderClass;
            this.targetClass = targetClass;
        }

        public Builder<B, T> withTargetInstance(T targetInstance) {
            this.targetInstance = targetInstance;
            return this;
        }

        public Builder<B, T> withBuilderMethodPrefix(String prefix) {
            this.builderMethodPrefix = prefix;
            return this;
        }

        public Builder<B, T> withEnumMappingMethod(String enumMappingMethod) {
            this.enumMappingMethod = enumMappingMethod;
            return this;
        }

        public B newInstance() {
            if (targetInstance == null) {
                targetInstance = MapProxy.builder(targetClass).withEnumMappingMethod(enumMappingMethod).newInstance();
            }
            return (B) java.lang.reflect.Proxy.newProxyInstance(
                    builderClass.getClassLoader(),
                    new Class[] { builderClass },
                    new MapBuilderProxy(targetInstance, builderMethodPrefix));
        }

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
