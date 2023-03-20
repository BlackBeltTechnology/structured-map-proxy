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

import hu.blackbelt.structured.map.proxy.entity.User;
import hu.blackbelt.structured.map.proxy.entity.UserBuilder;
import hu.blackbelt.structured.map.proxy.entity.UserBuilderWithPrefix;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapBuilderProxyTest {

    @Test
    public void testBuild() {
        User user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance().id("1").active(true).loginName("teszt").build();

        assertEquals(Optional.of("teszt"), user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

    }

    @Test
    public void testBuildFromExisting() {
        User user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance().id("1").active(true).loginName("teszt").build();
        User user2 = MapBuilderProxy.builder(UserBuilder.class, user).newInstance().id("2").build();
        assertEquals(Optional.of("teszt"), user2.getLoginName());
        assertEquals("2", user2.getId());
        assertEquals("teszt", ((MapHolder) user2).toMap().get("loginName"));
    }

    @Test
    public void testBuildWithProxy() {
        User user = MapBuilderProxy.builder(UserBuilderWithPrefix.class, User.class).withBuilderMethodPrefix("with").newInstance()
                .withId("1").withActive(true).withLoginName("teszt").build();

        assertEquals(Optional.of("teszt"), user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

        user = MapBuilderProxy.builder(UserBuilderWithPrefix.class, User.class).withBuilderMethodPrefix("with").newInstance()
                .withId("1").withActive(true).withLoginName(null).build();

        assertEquals(Optional.empty(), user.getLoginName());

    }

}
