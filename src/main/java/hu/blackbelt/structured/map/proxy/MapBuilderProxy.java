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
import java.util.Collections;
import java.util.Map;

public final class MapBuilderProxy implements InvocationHandler {

    Object internal;
    String prefix;

    MapProxyParams params;

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
            this.params.setIdentifierField(params.getIdentifierField());
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

        public Builder<B, T> withIdentifierField(String identifierField) {
            this.params.setIdentifierField(identifierField);
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
