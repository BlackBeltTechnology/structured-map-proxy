# Structured Map Proxy - Project Documentation

## Project Overview

**Repository:** BlackBeltTechnology/structured-map-proxy
**License:** Apache License 2.0
**Java Version:** 21
**Build System:** Maven 3.9.4 with Maven Wrapper (`./mvnw`)

1. Creates dynamic JDK proxies backed by `java.util.Map` instances, providing type-safe interface access to dynamic map-based data structures
2. Maps interface getter/setter methods to map keys following JavaBean naming conventions, with support for `@Key` (custom key names) and `@Embedded` (field flattening) annotations
3. Handles nested interfaces, collections, optionals, enums, and supports immutable proxies and null-safe collections
4. Provides `MapBuilderProxy` for fluent builder-pattern construction of proxy instances
5. Supports bidirectional conversion between proxies, maps, and concrete Java beans via `adaptTo()`

## Directory Structure

```
structured-map-proxy/
├── src/
│   ├── main/java/hu/blackbelt/structured/map/proxy/   # Core library
│   │   ├── annotation/                                  # @Key, @Embedded
│   │   └── util/                                        # Reflection utilities
│   └── test/java/hu/blackbelt/structured/map/proxy/    # Test suites
│       └── entity/                                      # Test interfaces & beans
├── pom.xml                                              # Maven build (OSGi bundle)
├── .mvn/                                                # Maven wrapper & JVM config
├── .github/workflows/                                   # CI/CD pipelines
├── .vscode/                                             # VS Code settings
├── .zed/                                                # Zed editor settings
└── openspec/                                            # OpenSpec configuration
```

## Core Modules

This is a **single-module project** (no Maven submodules). All source lives under one artifact.

| Component | Type | Purpose |
|---|---|---|
| `MapProxy` | Class (InvocationHandler) | Core proxy implementation — intercepts interface method calls and routes them to the backing map |
| `MapBuilderProxy` | Class (InvocationHandler) | Builder-pattern proxy — wraps a target type with a fluent builder interface |
| `MapHolder` | Interface | Contract implemented by all proxies — exposes `toMap()`, `$originalMap()`, `$internalMap()`, `adaptTo()` |
| `MapProxyParams` | Class | Configuration container — immutable, nullSafeCollection, enumMappingMethod, mapNullToOptionalAbsent |
| `CompositeClassLoader` | Class | Custom ClassLoader that delegates to multiple parent class loaders |
| `@Key` | Annotation | Maps a getter to a custom map key name |
| `@Embedded` | Annotation | Flattens a nested interface's fields into the parent map level |
| `ReflectionUtil` | Utility | Finds getter/setter/adder methods via reflection and Guava CaseFormat |
| `MapBuilderProxyUtil` | Utility | Interface hierarchy analysis for builder proxies |

## Technology Stack

### Core Technologies
- **Java 21** with `--add-opens` for `java.lang`, `java.util`, `java.time`, `java.net` modules
- **Google Guava 30.0-jre** — `LoadingCache` for method/field descriptor caching, `CaseFormat` for name conversion, immutable collections
- **SLF4J 2.0.16** / **Logback 1.5.12** — logging
- **Lombok 1.18.34** — code generation (builders, getters/setters in test beans)
- **OSGi Core 6.0.0** / **OSGi CMPN 7.0.0** — bundle packaging support (provided scope)

### Build & Quality
- **Maven 3.9.4** with wrapper (`./mvnw`)
- **JUnit Jupiter 5.9.1** — test framework
- **Hamcrest 2.2** — assertion matchers
- **Mockito 4.8.0** — mocking
- **JaCoCo 0.8.12** — code coverage
- **SonarQube** — static analysis (sonar-maven-plugin 3.9.1.2184)
- **maven-bundle-plugin 5.1.8** — OSGi bundle packaging
- **flatten-maven-plugin 1.3.0** — CI-friendly version resolution (`${revision}`)

## Build Commands

```bash
# Run all tests
mvn clean test

# Full build and install
mvn clean install

# Run a single test class
mvn test -Dtest=MapProxyTest

# Run a single test method
mvn test -Dtest=MapProxyTest#testGetterSetter

# Build with specific version (CI)
./mvnw -B -Drevision=1.0.0 clean install

# Deploy to Judo Nexus
./mvnw -B -Drevision=1.0.0 -Prelease-judong deploy

# Deploy to Maven Central
./mvnw -B -Drevision=1.0.0 -P"release-central,sign-artifacts" deploy
```

### Maven Profiles

| Profile | Purpose |
|---|---|
| `sign-artifacts` | GPG-sign artifacts for release |
| `release-dummy` | Deploy to local file system (testing) |
| `release-judong` | Deploy to Judo Nexus (snapshots) |
| `release-central` | Deploy to Maven Central via Sonatype OSSRH |
| `generate-github-asciidoc-diagrams` | Generate diagram images from AsciiDoc sources |
| `update-source-code-license` | Update Apache 2.0 license headers in source files |

## Key Configuration Files

| File | Purpose |
|---|---|
| `pom.xml` | Maven build definition — dependencies, plugins, OSGi bundle config, profiles |
| `.mvn/jvm.config` | JVM options for Maven: memory settings and `--add-opens` flags for Java 21 |
| `.mvn/extensions.xml` | Maven wagon extensions (file and webdav-jackrabbit) |
| `logback-test.xml` | Logback configuration for test execution |
| `.github/workflows/build.yml` | Main CI/CD pipeline — build, test, deploy, release |
| `.github/workflows/release.yml` | Manual release workflow |

## Development Environment

**Required:**
- Java 21 JDK (OpenJDK-based distribution recommended)
- Maven 3.9.4+ (or use the included `./mvnw` wrapper)

**JVM Configuration:**
The `.mvn/jvm.config` file provides required `--add-opens` flags automatically. No manual JVM configuration needed.

**System Properties:**
- `structuredMapProxyCacheExpireInSecond` (default: `60`) — TTL in seconds for Guava `LoadingCache` entries that cache method and field descriptors per interface class

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** `${revision}` property in pom.xml, resolved by flatten-maven-plugin (currently `2.0.0-SNAPSHOT`)
- **Branch naming:** GitFlow model — `feature/JNG-*`, `bugfix/JNG-*`, `release/*`, `hotfix/JNG-*`, `support/JNG-*`
- **Commit rule:** Every commit must reference a JIRA ticket (e.g., `JNG-1234`)
- **Release flow:** Automated via GitHub Actions — build.yml, merge-pr-tagged.yml, create-release-on-master.yml, release.yml

## Important Notes

1. This is a **single-artifact OSGi bundle** — it exports `hu.blackbelt.structured.map.proxy.*` and can be deployed in OSGi containers
2. The `MapProxy` class is ~1600 lines and contains most of the core logic including proxy creation, method dispatch, type coercion, nested proxy handling, and bean conversion
3. Method/field descriptors are cached per interface class using Guava `LoadingCache` with a 60-second TTL (configurable via system property)
4. The `@Embedded` annotation **flattens** nested interface fields into the parent map — it does NOT create nested map structure
5. Static methods `toString(T)`, `equals(T, Object)`, and `hashCode(T)` on interfaces override the corresponding `Object` methods on the proxy
6. Proxy method dispatch follows this priority: hashCode → equals → toString → setter → adder → remover → $originalMap → $internalMap → getter → isGetter → toMap → adaptTo → @Embedded factory methods

## Related Documentation

- [README.md](README.md) — Library overview, usage examples, and API reference
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development setup and submission guidelines
- [.github/CIFLOW.md](.github/CIFLOW.md) — CI/CD pipeline and branch strategy documentation
