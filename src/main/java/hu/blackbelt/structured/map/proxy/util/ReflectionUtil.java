package hu.blackbelt.structured.map.proxy.util;

import com.google.common.base.Predicate;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

public final class ReflectionUtil {

    private static final String GET_PREFIX = "get";
    private static final String SET_PREFIX = "set";

    private ReflectionUtil() {
    }

    public static Method findGetter(Class<?> cl, String name) {
        return findMethodByName(cl, GET_PREFIX + LOWER_CAMEL.to(UPPER_CAMEL, name));
    }

    public static Method findSetter(Class<?> cl, String name) {
        return findMethodByName(cl, SET_PREFIX + LOWER_CAMEL.to(UPPER_CAMEL, name));
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

    public static <T> Predicate<Method> methodParameterType(final String name, final Class<T> cl) {
        return new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                return input.getName().equals(name) && input.getParameterTypes().length == 1 && input.getParameterTypes()[0].isAssignableFrom(cl);
            }
        };
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
            Method getter = findGetter(subject.getClass(), pathElements.next());
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
}
