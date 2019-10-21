package hu.blackbelt.structured.map.proxy;

import hu.blackbelt.judo.framework.lang.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapProxyTest {

    @Test
    public void testBuild() {
        User user = MapProxy.newInstance(User.class);
        user.setActive(true);
        user.setLoginName("teszt");
        user.setId("1");

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));
    }

    @Test
    public void testBuildFromMap() {
        Map<String, Object> prepared = Collections.singletonMap("loginName", "teszt");
        User user = MapProxy.newInstance(User.class, prepared);
        user.setActive(true);
        user.setId("1");

        assertEquals("teszt", user.getLoginName());
        assertEquals("1", user.getId());
        assertEquals("teszt", ((MapHolder) user).toMap().get("loginName"));
    }
}
