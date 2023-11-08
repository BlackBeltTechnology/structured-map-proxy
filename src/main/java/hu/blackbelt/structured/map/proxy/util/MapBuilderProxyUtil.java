package hu.blackbelt.structured.map.proxy.util;

import hu.blackbelt.structured.map.proxy.MapHolder;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

public final class MapBuilderProxyUtil {


    // This method removes the interfaces that have a descendant or the excludeInterfaces contains it.
    public static List<Class<?>> getInterfacesWithNoDescendants(List<Class<?>> clazzInterfaces, List<Class<?>> excludedInterfaces) {
        List<Class<?>> interfacesWithNoDescendants = new ArrayList<>();
        for (Class<?> inter : clazzInterfaces) {
            if (!hasDescendants(inter, clazzInterfaces)) {
                interfacesWithNoDescendants.add(inter);
            }
        }
        if(excludedInterfaces != null) {
            interfacesWithNoDescendants.removeAll(excludedInterfaces);
        }
        return interfacesWithNoDescendants;
    }

    private static boolean hasDescendants(Class<?> inter, List<Class<?>> clazzInterfaces) {
        for (Class<?> interOther : clazzInterfaces) {
            if (!inter.equals(interOther) && inter.isAssignableFrom(interOther)) {
                return true;
            }
        }
        return false;
    }
}
