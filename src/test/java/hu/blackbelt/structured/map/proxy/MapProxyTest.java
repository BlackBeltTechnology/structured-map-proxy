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
import com.google.common.collect.ImmutableMap;
import hu.blackbelt.structured.map.proxy.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapProxyTest {

    User user;
    UserDetail userDetail1;
    UserDetail userDetail2;
    UserDetail userDetail3;
    UserDetail userDetail4;
    UserDetail userDetail5;

    @BeforeEach
    public void before() {
        user = MapProxy.builder(User.class).withNullSafeCollection(true).newInstance();

        userDetail1 = MapProxy.builder(UserDetail.class).newInstance();
        userDetail1.setId("1");
        userDetail1.setNote("Note1");

        userDetail2 = MapProxy.builder(UserDetail.class).newInstance();
        userDetail2.setId("2");
        userDetail2.setNote("Note2");

        userDetail3 = MapProxy.builder(UserDetail.class).newInstance();
        userDetail3.setId("3");
        userDetail3.setNote("Note3");

        userDetail4 = MapProxy.builder(UserDetail.class).newInstance();
        userDetail4.setId("4");
        userDetail4.setNote("Note4");

        userDetail5 = MapProxy.builder(UserDetail.class).newInstance();
        userDetail5.setId("5");
        userDetail5.setNote("Note5");

    }

    private void performStructuralTestCases() {
        assertEquals(Optional.of("teszt"), user.getLoginName());
        assertEquals("teszt", getMapValue(user, "loginName", String.class));

        assertEquals("1", user.getId());
        assertEquals("1", getMapValue(user, "id", String.class));

        assertEquals("Note1", user.getUserDetails().iterator().next().getNote());
        assertEquals("Note1", ((Map<String, Object>) getMapValue(user, "userDetails", Collection.class).
                iterator().next()).get("note"));

        assertEquals("v1", user.getMapWithoutType().get("k1"));
        assertEquals("v1", getMapValue(user, "mapWithoutType", Map.class).get("k1"));

        assertEquals("1", ((Map<String, Object>) getMapValue(user, "userDetails", Collection.class).
                iterator().next()).get("id"));
        assertEquals("1", getMapValue(user.getUserDetails().iterator().next(), "id", String.class));

        assertEquals("Test1", user.getCollectionWithoutType().iterator().next());
        assertEquals("Test1", getMapValue(user, "collectionWithoutType", Collection.class)
                .iterator().next());

        assertEquals("v1", ((Map) user.getCollectionWithMapType().iterator().next()).get("k1"));
        assertEquals("v1", ((Map) getMapValue(user, "collectionWithMapType", Collection.class)
                .iterator().next()).get("k1"));

        assertEquals("3", getMapValue(user.getMapWithValueType()
                .get("k1"), "id", String.class));
        assertEquals("3", ((Map) getMapValue(user, "mapWithValueType", Map.class).get("k1")).get("id"));

        Map<UserDetail, UserDetail> keyValue = user.getMapWithValueTypeAndKeyType();
        assertEquals("5", getMapValue(user.getMapWithValueTypeAndKeyType()
                .get(userDetail4), "id", String.class));
        assertEquals("5", ((Map) getMapValue(user, "mapWithValueTypeAndKeyType", Map.class)
                .get(((MapHolder) userDetail4).toMap())).get("id"));

        assertThat(user.getCountry().getName(), is("Austria"));

    }


    void assertMapStructure(Map map) {
        assertThat(map.get("active"), is(true));
        assertThat(map.get("id"), is("1"));
        assertThat(map.get("loginName"), is("teszt"));
        assertThat((Iterable<Map<String, Object>>) map.get("userDetails"), allOf(
                contains(
                        hasEntry(is("id"), is("1")),
                        hasEntry(is("id"), is("2")))
        ));
        assertThat((Iterable<String>) map.get("collectionWithoutType"), contains("Test1", "Test2"));
        assertThat((Iterable<Map<Object, Object>>) map.get("collectionWithMapType"),
                contains(
                        hasEntry(is("k1"), is("v1")),
                        hasEntry(is("k2"), is("v2"))
                ));
        assertThat((Map<?, ?>) map.get("mapWithoutType"), hasEntry(is("k1"), is("v1")));

        assertThat((Map<?, Map<Object, Object>>) map.get("mapWithValueType"),
                hasEntry(is("k1"), allOf(
                        hasEntry(is("id"), is("3")),
                        hasEntry(is("note"), is("Note3"))
                )));

        assertThat(map.get("country"), is(3));

    }

    @Test
    public void testBuild() {
        user.setActive(true);
        user.setLoginName("teszt");
        user.setId("1");
        user.setCountry(Country.AT);

        assertNotNull(user.getUserDetails());
        assertEquals(Collections.EMPTY_LIST, user.getUserDetails());

        user.setUserDetails(ImmutableList.of(userDetail1, userDetail2));
        user.setCollectionWithoutType(ImmutableList.of("Test1", "Test2"));
        user.setCollectionWithMapType(ImmutableList.of(
                ImmutableMap.of("k1", "v1"),
                ImmutableMap.of("k2", "v2"))
        );
        user.setMapWithoutType(ImmutableMap.of("k1", "v1"));
        user.setMapWithValueType(ImmutableMap.of("k1", userDetail3));
        user.setMapWithValueTypeAndKeyType(ImmutableMap.of(userDetail4, userDetail5));
        user.setBirthCountry(Country.AT);

        performStructuralTestCases();

        assertEquals(Optional.of(Country.AT), user.getBirthCountry());
        assertEquals(Country.AT.toString(), getMapValue(user, "birthCountry", Country.class));

    }

    @Test
    public void testOptional() {
        Map<String, Object> prepared = new HashMap<>();
        LocalDateTime time = LocalDateTime.of(2022, 2, 2, 22, 22, 22);
        prepared.put("active", true);
        prepared.put("id", "1");
        prepared.put("email", Optional.of("test@test.com"));
        prepared.put("loginName", Optional.of("teszt"));
        prepared.put("lastLoginTime", Optional.of(time));
        prepared.put("birthCountry", Optional.of(Country.AT.getOrdinal()));
        prepared.put("lastName", null);

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(true)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        Map map = ((MapHolder) user).toMap();
        assertThat(map.get("loginName"), is("teszt"));
        assertThat(user.getLoginName(), is(Optional.of("teszt")));
        assertThat(map.get("email"), is("test@test.com"));
        assertThat(user.getEmail(), is("test@test.com"));
        assertThat(map.get("lastLoginTime"), is(time));
        assertThat(user.getLastLoginTime(), is(time));

        assertThat(map.get("firstName"), is(nullValue()));
        assertThat(user.getFirstName(), is(nullValue()));

        assertThat(map.get("lastName"), is(nullValue()));
        assertThat(user.getLastName(), is(Optional.empty()));

        assertThat(map.get("birthCountry"), is(Country.AT.getOrdinal()));
        assertThat(user.getBirthCountry(), is(Optional.of(Country.AT)));

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(true)
                .withIdentifierField("id")
                .withMapNullToOptionalAbsent(true)
                .withEnumMappingMethod("getOrdinal").newInstance();

        assertThat(map.get("firstName"), is(nullValue()));
        assertThat(user.getFirstName(), is(Optional.empty()));

        assertThat(map.get("lastName"), is(nullValue()));
        assertThat(user.getLastName(), is(Optional.empty()));
    }

    @Test
    public void testEmbedded() {
        Map<String, Object> prepared = new HashMap<>();
        LocalDateTime time = LocalDateTime.of(2022, 2, 2, 22, 22, 22);
        prepared.put("__id", "ID");
        prepared.put("__type", "USER");

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(true)
                .withEnumMappingMethod("getOrdinal").newInstance();

        Map map = ((MapHolder) user).toMap();
        assertThat(map.get("__id"), is("ID"));
        assertThat(map.get("__type"), is("USER"));

        assertThat(user.getCompositeIdentifier().getId(), is("ID"));
        assertThat(user.getCompositeIdentifier().getType(), is("USER"));

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(false)
                .withEnumMappingMethod("getOrdinal").newInstance();

        user.setCompositeIdentifier(MapProxy.builder(Identifier.class)
                .withMap(ImmutableMap.of("__id", "ID2", "__type", "USER"))
                .newInstance());

        map = ((MapHolder) user).toMap();

        assertThat(map.get("__id"), is("ID2"));
        assertThat(map.get("__type"), is("USER"));

        assertThat(user.getCompositeIdentifier().getId(), is("ID2"));
        assertThat(user.getCompositeIdentifier().getType(), is("USER"));

    }

    @Test
    public void testEnum() {
        user.setCountry(Country.HU);
    }

    @Test
    public void testKeyAnnotation() {
        Map<String, Object> prepared = new HashMap<>();
        prepared.put("xmiid_key", "ID");

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(true)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        Map map = ((MapHolder) user).toMap();

        assertThat(map.get("xmiid_key"), is(equalTo("ID")));
        assertThat(user.getXmiid(), is(equalTo("ID")));

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(false)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        user.setXmiid("T1");
        map = ((MapHolder) user).toMap();
        assertThat(map.get("xmiid_key"), is(equalTo("T1")));
        assertThat(user.getXmiid(), is(equalTo("T1")));

    }

    @Test
    public void testBuildFromMap() {
        Map<String, Object> prepared = new HashMap<>();
        prepared.put("active", true);
        prepared.put("id", "1");
        prepared.put("loginName", "teszt");
        prepared.put("userDetails", ImmutableList.of(ImmutableMap.of("id", "1", "note", "Note1")));
        prepared.put("collectionWithoutType", ImmutableList.of("Test1", "Test2"));
        prepared.put("collectionWithMapType", ImmutableList.of(
                ImmutableMap.of("k1", "v1"),
                ImmutableMap.of("k2", "v2"))
        );
        prepared.put("mapWithoutType", ImmutableMap.of("k1", "v1"));
        prepared.put("mapWithValueType", ImmutableMap.of("k1",
                ImmutableMap.of("id", "3", "note", "Note3"))
        );
        prepared.put("mapWithValueTypeAndKeyType", ImmutableMap.of(
                ImmutableMap.of("id", "4", "note", "Note4"),
                ImmutableMap.of("id", "5", "note", "Note5"))
        );
        prepared.put("singleUserDetail", null);
        prepared.put("sms", null);
        prepared.put("country", 3);
        prepared.put("birthCountry", Country.AT.getOrdinal());
        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withImmutable(true)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        performStructuralTestCases();

        assertEquals(Optional.of(Country.AT), user.getBirthCountry());
        assertEquals(3, getMapValue(user, "birthCountry", Country.class));

    }

    @Test
    public void testBuildFromMapWithSingleMapChild() {
        Map<String, Object> prepared = new HashMap<>();
        Map<String, Object> detail = new HashMap<>();

        detail.put("id", "1");
        detail.put("note", "Note1");

        prepared.put("active", true);
        prepared.put("id", "1");
        prepared.put("singleUserDetail", detail );

        user = MapProxy.builder(User.class).
                withMap(prepared)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        assertTrue(user.getSingleUserDetail().getId().equals("1"));
    }

    @Test
    public void testToString() {
        Map<String, Object> prepared = new HashMap<>();

        prepared.put("active", true);
        prepared.put("id", "1");
        prepared.put("loginName", "teszt");
        prepared.put("userDetails", ImmutableList.of(
                ImmutableMap.of("id", "1", "note", "Note1"),
                ImmutableMap.of("id", "2", "note", "Note2"))
        );
        prepared.put("collectionWithoutType", ImmutableList.of("Test1", "Test2"));
        prepared.put("collectionWithMapType", ImmutableList.of(
                ImmutableMap.of("k1", "v1"),
                ImmutableMap.of("k2", "v2"))
                        );
        prepared.put("mapWithoutType", ImmutableMap.of("k1", "v1"));
        prepared.put("mapWithValueType", ImmutableMap.of("k1",
                ImmutableMap.of("id", "3", "note", "Note3")));
        prepared.put("mapWithValueTypeAndKeyType", ImmutableMap.of(
                ImmutableMap.of("id", "4", "note", "Note4"),
                ImmutableMap.of("id", "5", "note", "Note5"))
        );
        prepared.put("sms", null);
        prepared.put("singleUserDetail", null);

        user = MapProxy.builder(User.class).withMap(prepared).withImmutable(true).withIdentifierField("id").newInstance();
        assertEquals(
                "PROXY{" +
                        "active=true, " +
                        "collectionWithMapType=[{k1=v1}, {k2=v2}], " +
                        "collectionWithoutType=[Test1, Test2], " +
                        "id=1, " +
                        "loginName=teszt, " +
                        "mapWithValueType={k1=PROXY{id=3, note=Note3}}, " +
                        "mapWithValueTypeAndKeyType={PROXY{id=4, note=Note4}=PROXY{id=5, note=Note5}}, " +
                        "mapWithoutType={k1=v1}, " +
                        "singleUserDetail=null, " +
                        "sms=null, " +
                        "userDetails=[PROXY{id=1, note=Note1}, PROXY{id=2, note=Note2}]" +
                        "}",
                user.toString());
    }

    @Test
    public void testToMap() {
        Map<String, Object> prepared =
                ImmutableMap.<String, Object>builder()
                        .put("active", true)
                        .put("id", "1")
                        .put("loginName", "teszt")
                        .put("userDetails", ImmutableList.of(
                                ImmutableMap.of("id", "1", "note", "Note1"),
                                ImmutableMap.of("id", "2", "note", "Note2"))
                        )
                        .put("collectionWithoutType", ImmutableList.of("Test1", "Test2"))
                        .put("collectionWithMapType", ImmutableList.of(
                                ImmutableMap.of("k1", "v1"),
                                ImmutableMap.of("k2", "v2"))
                        )
                        .put("mapWithoutType", ImmutableMap.of("k1", "v1"))
                        .put("mapWithValueType", ImmutableMap.of("k1",
                                ImmutableMap.of("id", "3", "note", "Note3")))
                        .put("mapWithValueTypeAndKeyType",
                                ImmutableMap.of(
                                        ImmutableMap.of("id", "4", "note", "Note4"),
                                        ImmutableMap.of("id", "5", "note", "Note5")
                                )
                        )
                        .put("country", 3)
                        .build();

        user = MapProxy.builder(User.class).withMap(prepared).withImmutable(true).withIdentifierField("id").withEnumMappingMethod("getOrdinal").newInstance();

        Map map = ((MapHolder) user).toMap();
        assertMapStructure(map);
    }

    @Test
    public void testEqualsWithSameProxyValue() {
        Map<String, Object> prepared1 =
                ImmutableMap.<String, Object>builder()
                        .put("active", true)
                        .put("userDetails", ImmutableList.of(
                                ImmutableMap.of("id", "1", "note", "Note1"))
                        )
                        .put("id", "1")
                        .build();

        Map<String, Object> prepared2 =
                ImmutableMap.<String, Object>builder()
                        .put("active", true)
                        .put("userDetails", ImmutableList.of(
                                ImmutableMap.of("id", "1", "note", "Note1"))
                        )
                        .put("id", "1")
                        .build();

        User user1 = MapProxy.builder(User.class).withMap(prepared1).withImmutable(true).withIdentifierField("id").newInstance();
        User user2 = MapProxy.builder(User.class).withMap(prepared2).withImmutable(true).withIdentifierField("id").newInstance();

        assertTrue(user1.equals(user2));
        assertTrue(user2.equals(user1));
    }

    @Test
    public void testEqualsWithSameIdAndDifferentProxyValueWhenIdentifierFieldSet() {
        Map<String, Object> prepared1 =
                ImmutableMap.<String, Object>builder()
                        .put("active", false)
                        .put("id", "1")
                        .build();

        Map<String, Object> prepared2 =
                ImmutableMap.<String, Object>builder()
                        .put("active", true)
                        .put("id", "1")
                        .build();

        User user1 = MapProxy.builder(User.class).withMap(prepared1).withImmutable(true).withIdentifierField("id").newInstance();
        User user2 =MapProxy.builder(User.class).withMap(prepared2).withImmutable(true).withIdentifierField("id").newInstance();

        assertTrue(user1.equals(user2));
        assertTrue(user2.equals(user1));
    }

    @Test
    public void testEqualsWithSameIdAndDifferentProxyValueWhenIdentifierFieldNotSet() {
        Map<String, Object> prepared1 =
                ImmutableMap.<String, Object>builder()
                        .put("active", false)
                        .put("id", "1")
                        .build();

        Map<String, Object> prepared2 =
                ImmutableMap.<String, Object>builder()
                        .put("active", true)
                        .put("id", "1")
                        .build();

        User user1 = MapProxy.builder(User.class).withMap(prepared1).withImmutable(true).newInstance();;
        User user2 = MapProxy.builder(User.class).withMap(prepared2).withImmutable(true).newInstance();;

        assertFalse(user1.equals(user2));
        assertFalse(user2.equals(user1));
    }

    @Test
    public void testDifferentTypeWithConstructor() {
        Map<String, Object> prepared1 = new TreeMap<>();
        prepared1.put("title", "Test event");
        prepared1.put("date", 0L);
        prepared1.put("private", true);
        prepared1.put("room", "1/b");
        prepared1.put("notes", null);

        Event event1 = MapProxy.builder(Event.class).withMap(prepared1).withImmutable(true).newInstance();

        assertEquals(event1.getTitle(), "Test event");
        assertEquals(event1.getDate(), new Date(0));
        assertEquals(event1.isPrivate(), true);
        assertEquals(event1.getRoom(), Event.UpperCaseString.parse("1/B"));
        assertNull(event1.getNotes());
    }


    @Test
    public void testBuildFromBean() {
        UserBean userBean = UserBean.userBeanBuilder()
                .active(true)
                .id("1")
                .loginName("teszt")
                .userDetails(ImmutableList.of(UserDetailBean.builder()
                                .id("1")
                                .note("Note1")
                                .build(),
                             UserDetailBean.builder()
                                .id("2")
                                .note("Note2")
                                .build()))
                .collectionWithoutType(ImmutableList.of("Test1", "Test2"))
                .collectionWithMapType(ImmutableList.of(
                        ImmutableMap.of("k1", "v1"),
                        ImmutableMap.of("k2", "v2")))
                .mapWithoutType(ImmutableMap.of("k1", "v1"))
                .mapWithValueType(ImmutableMap.of("k1",
                                UserDetailBean.builder()
                                        .id("3")
                                        .note("Note3")
                                        .build()))
                .mapWithValueTypeAndKeyType(
                        ImmutableMap.of(
                                UserDetailBean.builder()
                                        .id("4")
                                        .note("Note4")
                                        .build(),
                                UserDetailBean.builder()
                                        .id("5")
                                        .note("Note5")
                                        .build())
                        )
                .singleUserDetail(null)
                .sms(null)
                .country(Country.AT)
                .birthCountry(Country.AT)
                .build();

        user = MapProxy.builder(User.class).
                withBean(userBean)
                .withImmutable(true)
                .withIdentifierField("id")
                .withEnumMappingMethod("getOrdinal").newInstance();

        performStructuralTestCases();

        assertEquals(Optional.of(Country.AT), user.getBirthCountry());
        assertEquals(3, getMapValue(user, "birthCountry", Country.class));

        Map map = ((MapHolder) user).toMap();
        assertMapStructure(map);
    }

    <T> T getMapValue(Object input, Object key, Class<T> target) {
        return (T) ((MapHolder) input).toMap().get(key);
    }
}
