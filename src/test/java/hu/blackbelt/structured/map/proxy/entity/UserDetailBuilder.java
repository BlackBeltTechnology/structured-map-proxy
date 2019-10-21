package hu.blackbelt.structured.map.proxy.entity;

public interface UserDetailBuilder {
    UserDetailBuilder id(String id);
    UserDetailBuilder note(String note);
    UserDetail build();
}
