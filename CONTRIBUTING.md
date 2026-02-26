# Contributing to Structured Map Proxy

Everyone is welcome to contribute! This guide explains how to set up your environment and submit changes.

## Development Environment

### Required Software

| Tool | Version | Notes |
|---|---|---|
| JDK | 21+ | OpenJDK-based distributions recommended (e.g., Azul Zulu, Eclipse Temurin) |
| Maven | 3.9.4+ | Maven wrapper (`./mvnw`) is included in the repo |

### Verify Your Setup

```bash
# Should show Java 21+
java -version

# Should show Maven 3.9.4+
mvn -version
```

### JVM Module Access

Java 21 requires `--add-opens` flags for the reflection-heavy proxy mechanism. These are pre-configured in `.mvn/jvm.config` and the Surefire plugin — no manual setup needed.

## Build Commands

```bash
# Run all tests
mvn clean test

# Full build (compile, test, package, install to local repo)
mvn clean install
```

## Submitting Issues

Before opening a new issue, search the [issue tracker](https://github.com/BlackBeltTechnology/structured-map-proxy/issues) — your problem may already be reported or resolved.

When filing a bug, include:

- Output of `java -version` and `mvn -version`
- Your `pom.xml` or `.flattened-pom.xml` (if relevant)
- A **minimal reproduction** — the smallest code that demonstrates the problem

> **Note:** We require a minimal reproduction to efficiently diagnose bugs. Without one, we may not be able to investigate.

## Submitting Pull Requests

This project follows [GitHub's standard forking model](https://guides.github.com/activities/forking/). Fork the repository, make your changes, and open a pull request.

For details on the CI/CD pipeline and branch naming conventions, see the [CI Flow documentation](.github/CIFLOW.md).

> **Important:** Every commit must reference a JIRA ticket number (e.g., `JNG-1234`). There is no commit without a ticket number.
