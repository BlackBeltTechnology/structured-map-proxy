package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.entity.User;
import hu.blackbelt.structured.map.proxy.entity.UserBuilder;
import hu.blackbelt.structured.map.proxy.entity.UserBuilderWithPrefix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapBuilderProxyTest {

    @Test
    public void testBuild() {
        User user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance().id("1").active(true).loginName("teszt").build();

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

    }

    @Test
    public void testBuildFromExisting() {
        User user = MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance().id("1").active(true).loginName("teszt").build();
        User user2 = MapBuilderProxy.builder(UserBuilder.class, user).newInstance().id("2").build();
        assertEquals("teszt", user2.getLoginName());
        assertEquals("2", user2.getId());
        assertEquals("teszt", ((MapHolder) user2).toMap().get("loginName"));
    }

    @Test
    public void testBuildWithProxy() {
        User user = MapBuilderProxy.builder(UserBuilderWithPrefix.class, User.class).withBuilderMethodPrefix("with").newInstance()
                .withId("1").withActive(true).withLoginName("teszt").build();

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

    }

}
