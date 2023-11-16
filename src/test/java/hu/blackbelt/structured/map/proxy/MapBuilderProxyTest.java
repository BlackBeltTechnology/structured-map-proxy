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

import com.google.common.collect.ImmutableMap;
import hu.blackbelt.structured.map.proxy.entity.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;

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
        UserBuilder builderFromExisting = MapBuilderProxy.builder(UserBuilder.class, user).newInstance().id("2").active(true);

        User user2 = builderFromExisting.build();
        User user3 = builderFromExisting.loginName("Brian").active(false).id("3").build();

        assertEquals(Optional.of("teszt"), user2.getLoginName());
        assertEquals("2", user2.getId());
        assertEquals("teszt", ((MapHolder) user2).toMap().get("loginName"));

        assertEquals(Optional.of("Brian"), user3.getLoginName());
        assertEquals("3", user3.getId());
        assertFalse(user3.getActive());
        assertEquals("Brian", ((MapHolder) user3).toMap().get("loginName"));
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

    @Test
    public void testBuildCopy() {

        UserDetailBuilder detailsBuilder = MapBuilderProxy.builder(UserDetailBuilder.class, UserDetail.class).newInstance().id("id1").note("note1");
        detailsBuilder.build();

        UserBuilder aBuilder = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance().loginName("a").userDetails(List.of(detailsBuilder.build()));
        UserBuilder bBuilder = aBuilder.id("1").userDetails(List.of(detailsBuilder.id("id2").build()));
        UserBuilder cBuilder = aBuilder.id("2").loginName("c");

        User a = aBuilder.build();
        User b = bBuilder.build();
        User c = cBuilder.build();

        assertNull(a.getId());
        assertEquals(Optional.of("a"), a.getLoginName());
        assertThat(a.getUserDetails(), hasItem(detailsBuilder.build()));

        assertEquals("1", b.getId());
        assertEquals(Optional.of("a"), b.getLoginName());
        assertThat(b.getUserDetails(), hasItem(detailsBuilder.id("id2").build()));

        assertEquals("2", c.getId());
        assertEquals(Optional.of("c"), c.getLoginName());
        assertThat(c.getUserDetails(), hasItem(detailsBuilder.build()));

        // value as a map

        UserDetail userDetail1 = detailsBuilder.id("id1").note("note1").build();
        UserDetail userDetail2 = detailsBuilder.id("id2").note("note2").build();
        UserDetail userDetail3 = detailsBuilder.id("id3").note("note3").build();

        UserBuilder userBuilderForMap1 = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance();
        UserBuilder userBuilderForMap2 = userBuilderForMap1.mapWithValueTypeAndKeyType(ImmutableMap.of(userDetail1, userDetail1)).mapWithoutType(ImmutableMap.of("k1", "v1")).mapWithValueType(Map.of("k1", userDetail1));
        UserBuilder userBuilderForMap3 = userBuilderForMap2.mapWithValueTypeAndKeyType(ImmutableMap.of(userDetail2, userDetail3));

        User usermap1 = userBuilderForMap1.build();
        User usermap2 = userBuilderForMap2.build();
        User usermap3 = userBuilderForMap3.build();

        assertNull(usermap1.getMapWithValueTypeAndKeyType());
        assertNull(usermap1.getMapWithoutType());
        assertNull(usermap1.getMapWithValueType());

        assertEquals("id1", getMapHolderValue(usermap2.getMapWithValueTypeAndKeyType()
                .get(userDetail1), "__id", String.class));
        assertEquals("v1", usermap2.getMapWithoutType().get("k1"));
        assertEquals("id1", getMapHolderValue(usermap2.getMapWithValueType()
                .get("k1"), "__id", String.class));

        assertEquals("id3", getMapHolderValue(usermap3.getMapWithValueTypeAndKeyType()
                .get(userDetail2), "__id", String.class));
        assertEquals("v1", usermap3.getMapWithoutType().get("k1"));
        assertEquals("id1", getMapHolderValue(usermap3.getMapWithValueType()
                .get("k1"), "__id", String.class));

    }

    @Test
    public void testBuilderAdd() {

        UserDetail userDetail1 = MapBuilderProxy.builder(UserDetailBuilder.class, UserDetail.class).newInstance().id("id1").note("note1").build();
        UserDetail userDetail2 = MapBuilderProxy.builder(UserDetailBuilder.class, UserDetail.class).newInstance().id("id2").note("note2").build();
        UserDetail userDetail3 = MapBuilderProxy.builder(UserDetailBuilder.class, UserDetail.class).newInstance().id("id3").note("note3").build();
        UserDetail userDetail4 = MapBuilderProxy.builder(UserDetailBuilder.class, UserDetail.class).newInstance().id("id4").note("note4").build();

        User user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .addToUserDetails(userDetail1)
                .build();

        assertEquals(1, user.getUserDetails().size());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id1")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .addToUserDetails(userDetail2, userDetail3)
                .build();

        assertEquals(2, user.getUserDetails().size());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .addToUserDetails(userDetail2, userDetail2)
                .build();

        assertEquals(2, user.getUserDetails().size());
        assertEquals(2, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .addToUserDetails(userDetail2)
                .addToUserDetails(userDetail2)
                .build();

        assertEquals(2, user.getUserDetails().size());
        assertEquals(2, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .addToUserDetails(userDetail1)
                .addToUserDetails(userDetail2)
                .addToUserDetails(userDetail3)
                .build();

        assertEquals(3, user.getUserDetails().size());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id1")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .userDetails(List.of(userDetail1, userDetail2))
                .addToUserDetails(userDetail3, userDetail4)
                .build();

        assertEquals(4, user.getUserDetails().size());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id1")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id4")).count());

        user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()
                .id("1")
                .active(true)
                .loginName("teszt")
                .userDetails(List.of(userDetail1, userDetail2))
                .addToUserDetails(userDetail3)
                .addToUserDetails(userDetail4)
                .build();

        assertEquals(4, user.getUserDetails().size());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id1")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id2")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id3")).count());
        assertEquals(1, user.getUserDetails().stream().filter(userDetail -> userDetail.getId().equals("id4")).count());
    }

    <T> T getMapHolderValue(Object input, Object key, Class<T> target) {
        return (T) ((MapHolder) input).toMap().get(key);
    }
}
