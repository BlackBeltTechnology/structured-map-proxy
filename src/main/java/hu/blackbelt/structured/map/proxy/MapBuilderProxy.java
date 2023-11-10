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

import com.google.common.collect.ImmutableList;
import hu.blackbelt.structured.map.proxy.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static hu.blackbelt.structured.map.proxy.util.MapBuilderProxyUtil.getInterfacesWithNoDescendants;

public final class MapBuilderProxy<B, T> implements InvocationHandler {

    MapProxyParams params;

    Object internal;
    String prefix;

    Class<B> builderClass;

    Class<T> targetClass;

    public static <B, T> Builder<B, T> builder(Class<B> builderClass, Class<T> targetClass) {
        return new MapBuilderProxy.Builder<>(builderClass, targetClass);
    }

    public static <B, T> Builder<B, T> builder(Class<B> builderClass, T targetInstance) {
        if (targetInstance instanceof Proxy) {
            List interfacesList = new ArrayList(Arrays.stream(targetInstance.getClass().getInterfaces()).collect(Collectors.toSet()));
            List<Class<?>> interfacesWithNoDescendants = getInterfacesWithNoDescendants(interfacesList, List.of(MapHolder.class, InvocationHandler.class));
            if (interfacesWithNoDescendants.size() != 1) {
                throw new RuntimeException("Proxy contains more than one interfaces");
            }
            return new MapBuilderProxy.Builder<B, T>(builderClass, (Class<T>) interfacesWithNoDescendants.get(0)).withTargetInstance(targetInstance);
        }
        return new MapBuilderProxy.Builder<B, T>(builderClass, (Class<T>) targetInstance.getClass()).withTargetInstance(targetInstance);
    }

    public static class Builder<B, T> {

        private final Class<B> builderClass;
        private final Class<T> targetClass;
        private T targetInstance;
        private String builderMethodPrefix;
        private MapProxyParams params = new MapProxyParams();

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

        private Builder<B, T> withParams(MapProxyParams params) {
            this.params.setImmutable(params.isImmutable());
            this.params.setNullSafeCollection(params.isNullSafeCollection());
            this.params.setEnumMappingMethod(params.getEnumMappingMethod());
            this.params.setMapNullToOptionalAbsent(params.isMapNullToOptionalAbsent());
            return this;
        }

        public Builder<B, T> withImmutable(boolean immutable) {
            this.params.setImmutable(immutable);
            return this;
        }

        public Builder<B, T> withNullSafeCollection(boolean nullSafeCollection) {
            this.params.setNullSafeCollection(nullSafeCollection);
            return this;
        }

        public Builder<B, T> withEnumMappingMethod(String enumMappingMethod) {
            this.params.setEnumMappingMethod(enumMappingMethod);
            return this;
        }

        public Builder<B, T> withMapNullToOptionalAbsent(boolean mapNullToOptionalAbsent) {
            this.params.setMapNullToOptionalAbsent(mapNullToOptionalAbsent);
            return this;
        }

        public B newInstance() {
            if (targetInstance == null) {
                targetInstance = MapProxy.builder(targetClass)
                        .withParams(params)
                        .newInstance();
            }

            return (B) java.lang.reflect.Proxy.newProxyInstance(
                    builderClass.getClassLoader(),
                    new Class[] { builderClass },
                    new MapBuilderProxy(targetInstance, builderMethodPrefix, params, builderClass, targetClass));
        }
    }

    private MapBuilderProxy(Object target, String builderMethodPrefix, MapProxyParams params, Class<B> builderClass, Class<T> targetClass) {
        this.internal = target;
        this.prefix = builderMethodPrefix;
        this.params = params;
        this.builderClass = builderClass;
        this.targetClass = targetClass;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {
        if (m.getName().startsWith("build")) {
            return MapProxy.builder(targetClass).withMap(((MapHolder) internal).$internalMap()).withParams(params).newInstance();
        } else {
            T newInstance = MapProxy.builder(targetClass).withMap(((MapHolder) internal).$internalMap()).withParams(params).newInstance();

            B b = MapBuilderProxy.builder(builderClass, targetClass).withParams(params).withBuilderMethodPrefix(prefix).withTargetInstance(newInstance).newInstance();

            String attrName = m.getName();
            String methodPrefix = "";
            if (prefix != null && !prefix.equals("") && attrName.startsWith(prefix)) {
                attrName = Character.toUpperCase(m.getName().charAt(prefix.length())) + m.getName().substring(prefix.length() + 1);
                methodPrefix = prefix;
            }  else if (!MapProxy.METHOD_ADD.equals(m.getName()) && m.getName().startsWith(MapProxy.METHOD_ADD)) {
                attrName = Character.toUpperCase(m.getName().charAt(MapProxy.METHOD_ADD.length())) + m.getName().substring(MapProxy.METHOD_ADD.length() + 1);
                methodPrefix = MapProxy.METHOD_ADD;
            }
            Method method = null;

            if (methodPrefix.equals(MapProxy.METHOD_ADD)) {
                method = ReflectionUtil.findAdder(newInstance.getClass(), attrName);
            } else {
                method = ReflectionUtil.findSetter(newInstance.getClass(), attrName);
            }

            method.invoke(newInstance, args);

            return b;
        }
    }
}
