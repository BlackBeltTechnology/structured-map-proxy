package hu.blackbelt.structured.map.proxy.util;

import hu.blackbelt.structured.map.proxy.MapHolder;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

public final class MapBuilderProxyUtil {


    // This method removes the interfaces that have a descendant or the excludeInterfaces contains it.
    public static void getNoDescendantInterfaces(List<Class<?>> interfacesList, List<Class<?>> excludedInterfaces) {
        if(excludedInterfaces != null) {
            interfacesList.removeAll(excludedInterfaces);
        }
        getNoDescendantInterfacesRec(interfacesList);
    }

    private static void getNoDescendantInterfacesRec(List<Class<?>> interfacesList) {
        if (interfacesList.size() >= 2) {
            Class<?> aClass = interfacesList.get(0);
            Set<Class<?>> removeSet = new HashSet<>();
            for (Class<?> inter : interfacesList) {
                if (!aClass.equals(inter) && aClass.isAssignableFrom(inter)) {
                    removeSet.add(aClass);
                } else if (!aClass.equals(inter) && inter.isAssignableFrom(aClass)) {
                    removeSet.add(inter);
                }
            }
            if (!removeSet.isEmpty()) {
                interfacesList.removeAll(removeSet);
                getNoDescendantInterfacesRec(interfacesList);
            }
        }
    }
}
