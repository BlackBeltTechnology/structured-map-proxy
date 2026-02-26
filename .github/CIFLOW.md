# CI Flow and Branch Strategy

This document describes the Git branching model and CI/CD pipeline for the structured-map-proxy project. The workflow is based on [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

## Branch Model

The project uses five types of branches, each with a specific role in the development lifecycle:

| Branch | Pattern | Based On | Purpose |
|---|---|---|---|
| **develop** | `develop` | — | Main development branch; always contains the latest in-progress work |
| **feature** | `feature/JNG-<number>_short_summary` | `develop` | New features for the next release |
| **release** | `release/<version>` or `<version>` | `develop` | Stabilization and testing before a production release |
| **bugfix** | `bugfix/JNG-<number>_short_summary` | release branch | Fixes applied during release testing (merged back to release and develop) |
| **support** | `support/JNG-<number>_short_summary` | release branch | Minor changes to a previous release (merged back to its release branch) |
| **hotfix** | `hotfix/JNG-<number>_short_summary` | `master` | Critical fixes applied to both `master` and `develop` |
| **master** | `master` | — | Latest released production version |

```mermaid
gitGraph
    commit id: "init"
    branch develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1"
    commit id: "feat-2"
    checkout develop
    merge feature/JNG-1 id: "merge-feat"
    branch release/1.0-beta1
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "bugfix"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-bugfix"
    checkout develop
    merge release/1.0-beta1 id: "merge-release"
    checkout main
    merge release/1.0-beta1 id: "release-1.0" tag: "v1.0"
```

## Version Numbering

The project uses **semantic versioning** with up to four segments: `major.minor.qualifier.micro`.

| Event | Version Change | Example |
|---|---|---|
| Start a feature branch | No change | stays `2.0.0-SNAPSHOT` |
| Start a release branch from develop | Increment 2nd number on develop | develop → `2.1.0-SNAPSHOT` |
| Bugfix on release branch | No change | stays at release version |
| Start a support branch | Increment 3rd number | `1.0.1-SNAPSHOT` |
| Start a hotfix branch | Increment 4th number | `1.0.0.1-SNAPSHOT` |

## CI Pipeline (GitHub Actions)

### build.yml — Main Build

Triggered on pushes to `develop` and pull requests targeting `develop`, `master`, `increment/*`, or `release/*`.

```mermaid
flowchart TD
    A[Push or PR event] --> B{Base branch?}
    B -->|master, release/*| C[Version = pom.xml<br/>without -SNAPSHOT]
    B -->|develop, increment/*| D[Version = major.minor.qualifier<br/>.date_commitId_branch]
    C --> E[Build & Deploy to Nexus]
    D --> E
    E --> F[Create git tag v‹version›]
    F --> G{Branch type?}
    G -->|increment/*, release/*| H[Create merge-pr/‹version› tag]
    H --> I["Trigger merge-pr-tagged.yml"]
    G -->|develop| J[Build changelog]
    J --> K[Create GitHub pre-release]
```

### merge-pr-tagged.yml — PR Merge Handler

Triggered when a `merge-pr/*` tag is pushed.

```mermaid
flowchart TD
    A["Tag push: merge-pr/‹version›"] --> B{Version format?}
    B -->|major.minor.qualifier| C[Merge PR to master]
    C --> D["Trigger create-release-on-master.yml"]
    B -->|Other| E[Squash PR to develop]
    E --> F["Trigger build.yml"]
    C --> G["Delete merge-pr/‹version› tag"]
    E --> G
```

### create-release-on-master.yml — Release Creation

Triggered on pushes to `master`.

```mermaid
flowchart LR
    A[Push to master] --> B[Get version from tag]
    B --> C[Build changelog]
    C --> D[Create GitHub release]
```

### release.yml — Manual Release

Triggered manually with a version parameter (`auto` or a specific `major.minor.qualifier`).

```mermaid
flowchart TD
    A[Manual trigger with version] --> B{Version = 'auto'?}
    B -->|Yes| C[Use pom.xml version<br/>without -SNAPSHOT]
    B -->|No| D[Use provided version]
    C --> E[Set next version = qualifier + 1]
    D --> E
    E --> F[Create PR to master<br/>with release version]
    E --> G[Create PR to develop<br/>with next version]
    F --> H["Trigger build.yml"]
    G --> H
```

## Development Rules

> **Important:** Every commit must include a JIRA ticket reference (e.g., `JNG-1234`). There is no commit without a ticket number.

Issue tracking is managed in [JIRA](https://blackbelt.atlassian.net/jira/dashboards).
