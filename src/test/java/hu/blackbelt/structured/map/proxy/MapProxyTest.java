package hu.blackbelt.structured.map.proxy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hu.blackbelt.structured.map.proxy.entity.User;
import hu.blackbelt.structured.map.proxy.entity.UserDetail;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapProxyTest {

    @Test
    public void testBuild() {
        User user = MapProxy.newInstance(User.class);
        user.setActive(true);
        user.setLoginName("teszt");
        user.setId("1");

        UserDetail userDetail1 = MapProxy.newInstance(UserDetail.class);
        userDetail1.setId("1");
        userDetail1.setNote("Note1");

        UserDetail userDetail2 = MapProxy.newInstance(UserDetail.class);
        userDetail2.setId("2");
        userDetail2.setNote("Note2");

        user.setUserDetails(ImmutableList.of(userDetail1, userDetail2));

        user.setCollectionWithoutType(ImmutableList.of("Test1", "Test2"));
        user.setCollectionWithMapType(ImmutableList.of(ImmutableMap.of("k1", "v1"), ImmutableMap.of("k2", "v2")));

        user.setMapWithoutType(ImmutableMap.of("k1", "v1"));

        UserDetail userDetail3 = MapProxy.newInstance(UserDetail.class);
        userDetail3.setId("3");
        userDetail3.setNote("Note3");

        user.setMapWithValueType(ImmutableMap.of("k1", userDetail3));

        UserDetail userDetail4 = MapProxy.newInstance(UserDetail.class);
        userDetail4.setId("4");
        userDetail4.setNote("Note4");

        UserDetail userDetail5 = MapProxy.newInstance(UserDetail.class);
        userDetail5.setId("5");
        userDetail5.setNote("Note5");

        user.setMapWithValueTypeAndKeyType(ImmutableMap.of(userDetail4, userDetail5));

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("Note1", user.getUserDetails().iterator().next().getNote());
        assertEquals("v1", user.getMapWithoutType().get("k1"));

        assertEquals("teszt", getProxyValue(user,  "loginName", String.class));
        assertEquals("1", getProxyValue(getProxyValue(user, "userDetails", Collection.class).
                iterator().next(), "id", String.class));
        assertEquals("1", getProxyValue(user.getUserDetails().iterator().next(), "id", String.class));

        assertEquals("Test1", getProxyValue(user, "collectionWithoutType", Collection.class)
                .iterator().next());
        assertEquals("v1", ((Map)(getProxyValue(user, "collectionWithMapType", Collection.class))
                .iterator().next()).get("k1"));
        assertEquals("3", getProxyValue(getProxyValue(user, "mapWithValueType", Map.class).get("k1"),
                "id", String.class));
        assertEquals("3", getProxyValue(user.getMapWithValueType().get("k1"), "id", String.class));

        assertEquals("5", getProxyValue(getProxyValue(user, "mapWithValueTypeAndKeyType", Map.class).get(userDetail4),
                "id", String.class));
        assertEquals("5", getProxyValue(user.getMapWithValueTypeAndKeyType().get(userDetail4), "id", String.class));

    }

    @Test
    public void testBuildFromMap() {
        Map<String, Object> prepared = ImmutableMap.of("loginName", "teszt",
                "userDetails", ImmutableList.of(ImmutableMap.of("id", "1", "note", "Note1")),
                "collectionWithoutType", ImmutableList.of("Test1", "Test2"),
                "collectionWithMapType", ImmutableList.of(ImmutableMap.of("k1", "v1"), ImmutableMap.of("k2", "v2"))
        );
        User user = MapProxy.newInstance(User.class, prepared, true);
        user.setActive(true);
        user.setId("1");

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("Note1", user.getUserDetails().iterator().next().getNote());

        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));
        assertEquals("1", ((Map) ((Collection)((MapHolder) user).toMap().get("userDetails")).iterator().next()).get("id"));
        assertEquals("1", ((MapHolder) (user.getUserDetails()).iterator().next()).toMap().get("id"));

        assertEquals("Test1", ((Collection)((MapHolder) user).toMap().get("collectionWithoutType")).iterator().next());
        assertEquals("v1", ((Map)((Collection)((MapHolder) user).toMap().get("collectionWithMapType")).iterator().next()).get("k1"));
    }

    @Test
    public void testEquals() {
    }

    <T> T getProxyValue(Object input, String name, Class<T> target ) {
        return (T) ((MapHolder) input).toMap().get(name);
    }
}
