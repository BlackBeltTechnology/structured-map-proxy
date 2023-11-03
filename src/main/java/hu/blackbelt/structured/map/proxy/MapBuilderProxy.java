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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
        if( targetInstance instanceof Proxy) {
            Class<?>[] interfaces = targetInstance.getClass().getInterfaces();

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
            return internal;
        } else {
            // TODO: Clone
            Map<String, Object> clonedMap = asMap(((MapHolder) internal).$internalMap());

            T newInstance = MapProxy.builder(targetClass).withMap(clonedMap).withParams(params).newInstance();

            B b = MapBuilderProxy.builder(builderClass, targetClass).withParams(params).withBuilderMethodPrefix(prefix).withTargetInstance(newInstance).newInstance();

            String attrName = Character.toUpperCase(m.getName().charAt(0)) + m.getName().substring(1);
            if (prefix != null && !prefix.equals("")) {
                attrName = Character.toUpperCase(m.getName().charAt(prefix.length())) + m.getName().substring(prefix.length() + 1);
            }
            Method setterMethod = ReflectionUtil.findSetter(newInstance.getClass(), attrName);
            Object[] value = null;
            if (args[0] instanceof Object[]) {
                value = new Object[]{ImmutableList.copyOf((Object[]) args[0])};
            } else {
                value = new Object[]{args[0]};
            }
            setterMethod.invoke(newInstance, value);

            return b;
        }
        //return proxy;
    }

    Map<String, Object> asMap(Map<String, Object> map) {
        return map != null ? asMapRec(map) : null;
    }

    Map<String, Object> asMapRec(Map<String, Object> map) {
        for (String key : map.keySet()) {
            if (key == null) {
                throw new IllegalArgumentException("Map contains null key(s)");
            }
        }
        Map<String, Object>  internal = new TreeMap<>();
        for (String key : new TreeSet<>(map.keySet())) {
            Object value = map.get(key);
            if (value instanceof List) {
                internal.put(key, ((List<Map<String, Object>>) value).stream().map(
                        e -> asMap(e)).collect(Collectors.toList()));
            } else if (value instanceof Collection) {
                internal.put(key, ((Collection<Map<String, Object>>) value).stream().map(
                        e -> asMap(e)).collect(Collectors.toSet()));
            } else if (value instanceof Map) {
                internal.put(key, asMap((Map<String, Object>) value));
            } else {
                internal.put(key, value);
            }
        }
        return internal;
    }


//    Class<?>  urgeInterfaceses(Proxy proxy) {
//
//        Class<?>[] interfaces = proxy.getClass().getInterfaces();
//
//        // reduce interfaces
//
//    }


}
