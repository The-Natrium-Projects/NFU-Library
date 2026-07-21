# NFU-Library — AI Agent Guide

> Machine-readable orientation document for AI coding agents. Describes what this
> repository is, how it is built, and how it relates to the other Natrium projects.
> **Active branch: `1.20.1-0.2.33-de-legacy`** (study this, not the default `1.20.1`).

## 1. Identity

| Field | Value |
|-------|-------|
| Name | NFU Library (Natrium Forge Utilities) |
| Repo | `The-Natrium-Projects/NFU-Library` |
| Mod ID | `nfulib` |
| Root package | `net.sodiumzh.nfu` |
| Maven group | `net.sodiumzh.nfu` |
| Language | Java 100% (Java 17 toolchain) |
| Version | `0.2.33` (`filename_version = 0.2.33-dev`) |
| License | LGPL-3.0 |
| Author | SodiumZH |

## 2. Purpose

NFU-Library is the **foundational, general-purpose utility library** for the
Natrium mod family. It contains no gameplay content of its own; instead it
provides reusable Minecraft Forge modding utilities that the higher-level mods
(NFF-Services, NFF-Girls) build upon.

## 3. Dependency Position

```
NFU-Library  (base layer — depends on nothing in the family)
    ^
    |
NFF-Services (depends on NFU-Library)
    ^
    |
NFF-Girls    (depends on NFU-Library + NFF-Services)
```

NFU-Library is the **bottom of the stack**. Changes here ripple upward to both
other repositories. It must remain content-agnostic and backward-compatible.

## 4. Tech Stack

- **Loader:** Minecraft Forge `47.1.44` for **Minecraft 1.20.1**
  (`minecraft_version_range = [1.20.1,1.20.2)`).
- **Build:** Gradle + ForgeGradle `[6.0,6.2)`, Librarian/ParchmentMC mappings
  (`parchment 2023.06.26-1.20.1`).
- **Mixins:** SpongePowered Mixin `0.7.+` + MixinExtras `0.3.1`.
  - Refmap: `nfulib.refmap.json`, config: `nfulib.mixins.json`.
- **JarJar** is enabled; MixinExtras-forge is jar-in-jar bundled.
- **JEI** (`15.2.0.27`) declared as compile/runtime dependency (optional integration).

## 5. Source Layout

Root source package `src/main/java/net/sodiumzh/nfu/` contains:

| Package | Responsibility |
|---------|----------------|
| `NFULibrary.java` | Mod entry point (`@Mod("nfulib")`). |
| `annotation` | Custom annotations. |
| `block` | Block helpers/base classes. |
| `capability` | Forge capability helpers. |
| `client` | Client-side utilities (rendering, UI). |
| `compat` | Third-party mod compatibility shims. |
| `container` | Menu/container (GUI) helpers. |
| `effect` | Mob effect utilities. |
| `entity` | Entity helpers/base types. |
| `event`, `eventhandler` | Event bus abstractions & handlers. |
| `exception` | Custom exception types. |
| `function` | Functional-interface helpers. |
| `info` | Metadata / descriptor types. |
| `item` | Item helpers/base classes. |
| `level` | World/level utilities. |
| `math` | Math/geometry helpers. |
| `mixin` | Mixin classes (see `nfulib.mixins.json`). |
| `network` | Packet/networking helpers. |
| `object` | Generic object/data structures. |
| `reflection` | Reflection utilities. |
| `registry` | Registration helpers. |
| `savedata` | Persistent save data; includes `savedata.redirector.SaveDataLocationRedirector` used by NFF-Services for legacy namespace migration. |
| `util` | Miscellaneous utilities. |

## 6. Build & Consumption

- Produces `nfulib-1.20.1-<version>` jars (`archive_base_name = nfulib-1.20.1`).
- Downstream repos consume it via a **flat-dir repository** pointing at
  `../NFU-Library/build/libs`, so this repo is expected to be checked out as a
  sibling directory. Build this project first (`./gradlew build`) before building
  NFF-Services or NFF-Girls.

## 7. Notes for Agents

- Keep the public API stable; downstream mods reference these utilities directly.
- The build uses `${...}` property expansion in `META-INF/mods.toml` and
  `pack.mcmeta` via `processResources` — update `gradle.properties` when bumping
  versions, not hardcoded strings.
- The default GitHub branch is `1.20.1`, but active development is on
  `1.20.1-0.2.33-de-legacy`. Always target the active branch.
