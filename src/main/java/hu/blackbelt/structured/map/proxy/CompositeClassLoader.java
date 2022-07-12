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

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static java.util.Arrays.asList;

@Slf4j
public class CompositeClassLoader extends ClassLoader {
    private static final String MANDATORY_CLASS_LOADER_MESSAGE = "The ClassLoader argument must be non-null.";
    // Class is used instead of interface to access the putIfAbsent() method.
    private CopyOnWriteArrayList<ClassLoader> classLoaders;

    public CompositeClassLoader(ClassLoader... classLoaders) {
        super(CompositeClassLoader.class.getClassLoader());
        Arrays.stream(classLoaders)
                .forEach(classLoader -> checkNotNull(classLoader, MANDATORY_CLASS_LOADER_MESSAGE));
        this.classLoaders = newCopyOnWriteArrayList(asList(classLoaders));
    }

    public void insert(ClassLoader classLoader) {
        checkNotNull(classLoader, MANDATORY_CLASS_LOADER_MESSAGE);
        throw new UnsupportedOperationException();
    }

    public void append(ClassLoader classLoader) {
        checkNotNull(classLoader, MANDATORY_CLASS_LOADER_MESSAGE);
        classLoaders.addIfAbsent(classLoader);
    }

    public void remove(ClassLoader classLoader) {
        checkNotNull(classLoader, MANDATORY_CLASS_LOADER_MESSAGE);
        classLoaders.remove(classLoader);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        log.trace("Finding {} class.", name);
        for (ClassLoader classLoader : classLoaders) {
            try {
                Class<?> cl = classLoader.loadClass(name);
                log.trace("Class {} found using {} class loader.", name, classLoader);
                return cl;
            } catch (ClassNotFoundException cnfe) {
                // This block intentionally left blank.
            }
        }
        log.trace("Class {} not found.", name);
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        log.trace("Finding {} resource.", name);
        URL result = null;
        for (ClassLoader classLoader : classLoaders) {
            result = classLoader.getResource(name);
            if (result != null) {
                log.trace("Resource {} found using {} class loader.", name, classLoader);
                break;
            }
        }
        if (result == null) {
            log.trace("Resource {} not found.", name);
        }
        return result;
    }
}
