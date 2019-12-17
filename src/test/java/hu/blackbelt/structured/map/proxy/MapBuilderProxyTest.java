package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.structured.map.proxy.entity.User;
import hu.blackbelt.structured.map.proxy.entity.UserBuilder;
import hu.blackbelt.structured.map.proxy.entity.UserBuilderWithPrefix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapBuilderProxyTest {

    @Test
    public void testBuild() {
        User user = MapBuilderProxy.newInstance(UserBuilder.class, User.class).id("1").active(true).loginName("teszt").build();

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

    }

    @Test
    public void testBuildWithProxy() {
        User user = MapBuilderProxy.newInstance(UserBuilderWithPrefix.class, User.class, "with")
                .withId("1").withActive(true).withLoginName("teszt").build();

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));

    }

}
