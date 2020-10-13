package hu.blackbelt.structured.map.proxy.entity;

public enum Country {

    HU(1, "Hungary"), AT(3, "Austria");

    private final String name;
    private final int ordinal;

    Country(int ordinal, String name) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
