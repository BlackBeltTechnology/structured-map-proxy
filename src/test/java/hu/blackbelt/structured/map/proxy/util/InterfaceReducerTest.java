package hu.blackbelt.structured.map.proxy.util;

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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static hu.blackbelt.structured.map.proxy.util.MapBuilderProxyUtil.getInterfacesWithNoDescendants;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class InterfaceReducerTest {

    interface A {
    }

    interface B extends A {
    }

    interface C extends A {
    }

    // Diamond
    interface D extends B, C {
    }

    interface E {
    }

    interface F extends B, E {
    }

    interface G {
    }

    interface H extends G {
    }

    @Test
    public void testInterfaceReducer() {

        //Diamond
        List<Class<?>> diamondList = new ArrayList<>(List.of(C.class, B.class, D.class, A.class));
        List<Class<?>> interfacesWithNoDescendants = getInterfacesWithNoDescendants(diamondList, null);

        assertEquals(1, interfacesWithNoDescendants.size());
        assertEquals(D.class, interfacesWithNoDescendants.get(0));

        // Multi Interfaces
        List<Class<?>> multiList = new ArrayList<>(List.of(F.class, B.class, E.class, A.class));
        interfacesWithNoDescendants = getInterfacesWithNoDescendants(multiList, null);
        assertEquals(1, interfacesWithNoDescendants.size());
        assertEquals(F.class, interfacesWithNoDescendants.get(0));

        List<Class<?>> interfacesWithNoRelation = new ArrayList<>(List.of(A.class, F.class, B.class, E.class, H.class, G.class));
        interfacesWithNoDescendants = getInterfacesWithNoDescendants(interfacesWithNoRelation, null);
        assertTrue(1 < interfacesWithNoDescendants.size());

        List<Class<?>> interfacesWithExclude = new ArrayList<>(List.of(F.class, B.class, E.class, A.class, G.class));
        interfacesWithNoDescendants = getInterfacesWithNoDescendants(interfacesWithExclude, List.of(G.class));
        assertEquals(1, interfacesWithNoDescendants.size());
        assertEquals(F.class, interfacesWithNoDescendants.get(0));

    }
}
