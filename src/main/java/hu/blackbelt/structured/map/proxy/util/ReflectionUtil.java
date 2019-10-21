package hu.blackbelt.structured.map.proxy.util;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;

public final class ReflectionUtil {

    private static final String GET_PREFIX = "get";
    private static final String SET_PREFIX = "set";

    public static final String JUDO_FIELD_PREFIX = "$judo";
    public static final String META_FIELD_NAME = JUDO_FIELD_PREFIX + "$meta";

    private static final Predicate<Field> STATIC = new Predicate<Field>() {
        @Override
        public boolean apply(Field input) {
            return isStatic(input.getModifiers());
        }
    };

    private static final Predicate<Field> FINAL = new Predicate<Field>() {
        @Override
        public boolean apply(Field input) {
            return isFinal(input.getModifiers());
        }
    };

    private static final Predicate<Field> JUDO_META = new Predicate<Field>() {

        @Override
        public boolean apply(Field input) {
            return input.getName().startsWith(JUDO_FIELD_PREFIX);
        }
    };

    private ReflectionUtil() {
    }


    public static Field findNonStaticField(Class<?> cl, final String field) {
        return Iterables.find(findNonStaticFields(cl), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.getName().equals(field);
            }
        });
    }

    public static Field findFinalField(Class<?> cl, final String field) {
        return Iterables.find(findFinalFields(cl), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.getName().equals(field);
            }
        });
    }

    /**
     * <p>
     * Finds the input class fields including the fields defined in superclass.
     * </p>
     * <p>
     * The method will return all fields regardless its visibility except the ones starting with {@link #JUDO_FIELD_PREFIX}
     * </p>
     *
     * @param cl
     * @return
     */
    public static Iterable<Field> findNonStaticFields(Class<?> cl) {
        return queryFields(cl, and(not(STATIC), not(JUDO_META)));
    }

    public static Iterable<Field> findFinalFields(Class<?> cl) {
        return queryFields(cl, and(FINAL, not(JUDO_META)));
    }


    private static Iterable<Field> queryFields(Class<?> cl, Predicate<Field> cond) {
        final ImmutableList.Builder<Field> listBuilder = ImmutableList.builder();
        if (cl.getSuperclass() != null) {
            listBuilder.addAll(queryFields(cl.getSuperclass(), cond));
        }
        listBuilder.addAll(filter(asList(cl.getDeclaredFields()), cond));
        return listBuilder.build();
    }

    public static List<Method> findGetters(Class<?> cl) {
        return findMethodsByNamePrefix(cl, GET_PREFIX);
    }

    public static List<Method> findSetters(Class<?> cl) {
        return findMethodsByNamePrefix(cl, SET_PREFIX);
    }

    public static Method findGetter(Class<?> cl, String name) {
        return findMethodByName(cl, GET_PREFIX + LOWER_CAMEL.to(UPPER_CAMEL, name));
    }

    public static Method findSetter(Class<?> cl, String name) {
        return findMethodByName(cl, SET_PREFIX + LOWER_CAMEL.to(UPPER_CAMEL, name));
    }

    public static Method findPrivateMethodByName(Class<?> cl, String name) {
        Method method = findMethodByName(cl.getDeclaredMethods(), cl, name);
        method.setAccessible(true);
        return method;
    }

    public static Method findMethodByName(Class<?> cl, String name) {
        return findMethodByName(cl.getMethods(), cl, name);
    }

    private static Method findMethodByName(Method[] availableMethods, Class<?> cl, String name) {
        List<Method> methods = newArrayList();
        for (Method method : availableMethods) {
            if (method.getName().equals(name)) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(format("No method found for %s class and %s name.", cl.getName(), name));
        } else {
            return methods.get(0);
        }
    }

    public static List<Method> findMethodsByNamePrefix(Class<?> cl, String prefix) {
        List<Method> methods = newArrayList();
        for (Method method : cl.getMethods()) {
            if (method.getName().startsWith(prefix)) {
                methods.add(method);
            }
        }
        return ImmutableList.copyOf(methods);
    }

    public static List<Method> findMethodsByName(Class<?> cl, String name) {
        List<Method> methods = newArrayList();
        for (Method method : cl.getMethods()) {
            if (method.getName().equals(name)) {
                methods.add(method);
            }
        }
        return ImmutableList.copyOf(methods);
    }

    public static Method findGetSetPair(Method method) {
        return findGetSetPair(method.getDeclaringClass(), method.getName());
    }

    public static Method findGetSetPair(Class<?> cl, String methodName) {
        checkNotNull(cl);
        checkNotNull(methodName);
        if (methodName.startsWith(GET_PREFIX)) {
            return findSetter(cl, UPPER_CAMEL.to(LOWER_CAMEL, methodName.substring(GET_PREFIX.length())));
        } else if (methodName.startsWith(SET_PREFIX)) {
            return findGetter(cl, UPPER_CAMEL.to(LOWER_CAMEL, methodName.substring(SET_PREFIX.length())));
        } else {
            throw new IllegalArgumentException("Only set/get methods are allowed.");
        }
    }

    public static <T> Predicate<Method> methodParameterType(final String name, final Class<T> cl) {
        return new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                return input.getName().equals(name) && input.getParameterTypes().length == 1 && input.getParameterTypes()[0].isAssignableFrom(cl);
            }
        };
    }

    public static Object getNonStaticFieldValue(Object target, String fieldName) {
        checkNotNull(target);
        return getFieldValue(target, findNonStaticField(target.getClass(), fieldName));
    }


    public static Object getFieldValue(Object target, Field field) {
        try {
            boolean state = field.isAccessible();
            if (!state) {
                field.setAccessible(true);
            }
            Object ret =  field.get(target);
            if (!state) {
                field.setAccessible(false);
            }
            return ret;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot get field value via reflection", e);
        }
    }

    public static Object getFieldValue(Object subject, Iterator<String> pathElements) {
        Object ret = null;
        if (subject != null && pathElements != null && pathElements.hasNext()) {
            Method getter = ReflectionUtil.findGetter(subject.getClass(), pathElements.next());
            if (getter != null) {
                try {
                    ret = getter.invoke(subject);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    String msg = String.format("Can not get field[%s] value on objectClass[%s]", getter.getName(), subject.getClass());
                    throw new IllegalArgumentException(msg, e);
                }
                if (pathElements.hasNext() && ret != null) {
                    ret = getFieldValue(ret, pathElements);
                }
            }
        }
        return ret;
    }

    public static void setFieldValue(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot set field value via reflection", e);
        }
    }

    public static Object invokeCustomOperation(Object service, String methodName, Map<String, Object> inputVariables) throws Throwable {
        Method method = findCustomOperation(methodName, service.getClass());
        try {
            Object[] arguments = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterCount(); i++) {
                arguments[i] = inputVariables.get(method.getParameters()[i].getName());
            }
            return method.invoke(service, arguments);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    public static Method findCustomOperation(String methodName, Class<?> serviceClass) {
        return findCustomOperation(methodName, serviceClass, false);
    }

    public static Method findCustomOperation(String methodName, Class<?> serviceClass, boolean batch) {
        List<Method> methods = findMethodsByName(serviceClass, methodName);
        if (methods.size() == 1) {
            // class based methods
            checkArgument(!batch, "Class bases methods has no batch equivalent.");
            return methods.get(0);
        } else if (methods.size() == 2) {
            Method normalMethod;
            Method batchMethod;
            // instance based methods
            if (methods.get(0).getParameterTypes()[0] == List.class) {
                batchMethod = methods.get(0);
                normalMethod = methods.get(1);
            } else {
                normalMethod = methods.get(0);
                batchMethod = methods.get(1);
            }
            if (batch) {
                return batchMethod;
            } else {
                return normalMethod;
            }
        } else {
            throw new IllegalArgumentException("Wrong number of methods found: " + methods);
        }
    }
}
