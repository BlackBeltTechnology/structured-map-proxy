# structured-map-proxy Specification

## Purpose

Provides dynamic JDK proxy instances backed by `java.util.Map`, enabling type-safe interface access to map-based data structures with support for nested interfaces, collections, optionals, annotations, builders, and bean conversion.

## Architecture

The library centers on `MapProxy` (an `InvocationHandler`) that intercepts method calls on proxy instances and routes them to an underlying `Map<String, Object>`. Configuration is held in `MapProxyParams`. All proxies implement `MapHolder` for map access and type conversion. `MapBuilderProxy` provides fluent builder support. Annotations `@Key` and `@Embedded` customize the mapping. `ReflectionUtil` handles method discovery, and `CompositeClassLoader` resolves classes across multiple loaders.

## Requirements

### Requirement: Proxy creation from Map

The library SHALL create a proxy instance for a given interface class backed by a provided `Map<String, Object>`.

#### Scenario: Create proxy with map data
- **GIVEN** a `Map<String, Object>` with key `"email"` set to `"test@test.com"`
- **WHEN** `MapProxy.builder(User.class).withMap(map).newInstance()` is called
- **THEN** the returned proxy implements `User` and `MapHolder`, and `user.getEmail()` returns `"test@test.com"`

#### Scenario: Create empty proxy
- **WHEN** `MapProxy.builder(User.class).newInstance()` is called without `withMap()`
- **THEN** a proxy is returned with all getters returning `null` (or `Optional.empty()` / empty collection per configuration)

### Requirement: Getter/setter method dispatch via JavaBean conventions

The library SHALL map getter methods (`get<Field>()`) and setter methods (`set<Field>(<Type>)`) to map keys using JavaBean naming conventions (lowercase first letter of field name).

#### Scenario: Getter reads from map
- **GIVEN** a proxy with backing map containing `"active" → true`
- **WHEN** `proxy.getActive()` is called
- **THEN** `true` is returned

#### Scenario: Setter writes to map
- **GIVEN** a mutable proxy
- **WHEN** `proxy.setEmail("new@test.com")` is called
- **THEN** the backing map contains `"email" → "new@test.com"`

### Requirement: Custom key mapping with @Key annotation

The library SHALL use the `@Key(name = "...")` annotation on getter methods to override the default map key name.

#### Scenario: @Key overrides map key
- **GIVEN** an interface with `@Key(name = "__id") Serializable getId()`
- **AND** a backing map with `"__id" → "123"`
- **WHEN** `proxy.getId()` is called
- **THEN** `"123"` is returned

### Requirement: Embedded field flattening with @Embedded annotation

The library SHALL flatten fields of a nested interface marked with `@Embedded` into the parent map level, rather than creating a nested map structure.

#### Scenario: @Embedded reads from flat map
- **GIVEN** an `Entity` interface with `@Embedded Identifier getId()`
- **AND** the `Identifier` interface has `@Key(name = "__id") getId()` and `@Key(name = "__type") getType()`
- **AND** a backing map `{"__id": "1", "__type": "User", "name": "John"}`
- **WHEN** `entity.getId().getId()` is called
- **THEN** `"1"` is returned from the flattened map

#### Scenario: @Embedded setter flattens into parent map
- **GIVEN** a mutable `Entity` proxy
- **WHEN** `entity.setId(identifierProxy)` is called with an identifier containing `__id=1, __type=User`
- **THEN** the parent map contains `"__id" → "1"` and `"__type" → "User"` at the top level

### Requirement: Nested interface proxy creation

The library SHALL automatically create nested proxies for interface-typed properties when the backing map contains a nested `Map<String, Object>`.

#### Scenario: Nested interface from map
- **GIVEN** a `User` proxy with backing map containing `"singleUserDetail" → {"note": "Hello"}`
- **WHEN** `user.getSingleUserDetail()` is called
- **THEN** a `UserDetail` proxy is returned where `getNote()` returns `"Hello"`

### Requirement: Collection of interface proxies

The library SHALL wrap collections of maps into collections of proxies when the property type is `Collection<InterfaceType>`.

#### Scenario: Collection getter returns proxied elements
- **GIVEN** a backing map with `"userDetails" → [{"note": "A"}, {"note": "B"}]`
- **WHEN** `user.getUserDetails()` is called
- **THEN** a collection of two `UserDetail` proxies is returned

### Requirement: Collection mutation via addTo/removeFrom methods

The library SHALL support `addTo<Collection>()` and `removeFrom<Collection>()` methods for mutating collection properties.

#### Scenario: addTo adds element to collection
- **GIVEN** a mutable proxy with an empty `userDetails` collection
- **WHEN** `user.addToUserDetails(detail1, detail2)` is called
- **THEN** `user.getUserDetails()` contains both elements

### Requirement: Optional property support

The library SHALL handle `Optional<T>` return types by wrapping map values in `Optional`.

#### Scenario: Optional with value present
- **GIVEN** a backing map with `"firstName" → "John"`
- **WHEN** `user.getFirstName()` is called (returns `Optional<String>`)
- **THEN** `Optional.of("John")` is returned

#### Scenario: Optional with mapNullToOptionalAbsent enabled
- **GIVEN** a proxy built with `withMapNullToOptionalAbsent(true)` and no `"firstName"` in map
- **WHEN** `user.getFirstName()` is called
- **THEN** `Optional.empty()` is returned

### Requirement: Immutable proxy mode

The library SHALL throw `IllegalStateException` on any mutating operation when the proxy is created with `withImmutable(true)`.

#### Scenario: Setter on immutable proxy throws
- **GIVEN** a proxy created with `MapProxy.builder(User.class).withImmutable(true).withMap(map).newInstance()`
- **WHEN** `user.setEmail("x")` is called
- **THEN** `IllegalStateException` is thrown

### Requirement: Null-safe collections

The library SHALL return an empty collection instead of `null` for unset collection properties when `withNullSafeCollection(true)` is configured.

#### Scenario: Unset collection returns empty list
- **GIVEN** a proxy with `nullSafeCollection=true` and no `"userDetails"` in the map
- **WHEN** `user.getUserDetails()` is called
- **THEN** an empty collection is returned (not `null`)

### Requirement: toMap serialization

The library SHALL serialize the proxy back to a `Map<String, Object>` via the `toMap()` method, converting nested proxies to maps, enums to their configured representation, and respecting `@Key` and `@Embedded` mappings.

#### Scenario: toMap round-trip
- **GIVEN** a proxy created from a map with nested structures
- **WHEN** `((MapHolder) proxy).toMap()` is called
- **THEN** the returned map is structurally equivalent to the original input

### Requirement: adaptTo bean conversion

The library SHALL convert a proxy to a concrete Java bean class via `adaptTo(Class<T>)`, matching getter names to setter names and recursively converting nested objects.

#### Scenario: Convert proxy to bean
- **GIVEN** a `User` proxy with `email="test"`
- **WHEN** `user.adaptTo(UserBean.class)` is called
- **THEN** a `UserBean` instance is returned with `getEmail()` returning `"test"`

### Requirement: adaptTo interface conversion

The library SHALL create a new proxy of a different interface type when `adaptTo()` is called with an interface class.

#### Scenario: Convert proxy to different interface
- **GIVEN** a proxy implementing interface `A`
- **WHEN** `proxy.adaptTo(B.class)` is called where `B` is an interface
- **THEN** a new `MapProxy` of type `B` is returned, backed by the same map data

### Requirement: Builder proxy construction

`MapBuilderProxy` SHALL provide fluent builder construction where each setter-like method returns the builder, and `build()` returns the target proxy.

#### Scenario: Builder creates proxy
- **GIVEN** `MapBuilderProxy.builder(UserBuilder.class, User.class).newInstance()`
- **WHEN** `.active(true).email("test").build()` is called
- **THEN** a `User` proxy is returned with `getActive()=true` and `getEmail()="test"`

### Requirement: Builder method prefix

`MapBuilderProxy` SHALL support a configurable method prefix so builder methods like `withEmail()` map to the `email` field.

#### Scenario: Builder with prefix
- **GIVEN** `MapBuilderProxy.builder(UserBuilder.class, User.class).withBuilderMethodPrefix("with").newInstance()`
- **WHEN** `.withEmail("test").build()` is called
- **THEN** the resulting proxy has `getEmail()="test"`

### Requirement: Enum mapping configuration

The library SHALL support configurable enum serialization via `enumMappingMethod` — using `name()` by default, but supporting `ordinal()` or custom methods.

#### Scenario: Enum stored as name
- **GIVEN** a proxy with default `enumMappingMethod` and a map containing `"country" → "US"`
- **WHEN** `proxy.getCountry()` is called (returns `Country` enum)
- **THEN** `Country.US` is returned

### Requirement: Static method overrides for toString, equals, hashCode

The library SHALL use static methods `toString(T)`, `equals(T, Object)`, and `hashCode(T)` defined on the interface to override the default proxy behavior for these Object methods.

#### Scenario: Custom toString
- **GIVEN** an interface with `static String toString(MyType o) { return "custom:" + o.getId(); }`
- **WHEN** `proxy.toString()` is called
- **THEN** the custom static method is invoked

#### Scenario: Default toString
- **GIVEN** an interface without a custom `toString` static method
- **WHEN** `proxy.toString()` is called
- **THEN** `"PROXY{key1=val1, key2=val2}"` format is returned with sorted internal map entries

### Requirement: Descriptor caching

The library SHALL cache method and field descriptors per interface class using `LoadingCache` with a configurable TTL (default 60 seconds, configurable via `structuredMapProxyCacheExpireInSecond` system property).

#### Scenario: Cache expiration is configurable
- **GIVEN** system property `structuredMapProxyCacheExpireInSecond=120`
- **WHEN** MapProxy initializes its caches
- **THEN** cache entries expire after 120 seconds instead of 60
