# SodiumLeafCulling-Unofficial

An unofficial, multi-version fork of Sodium Leaf Culling. The project uses
[Stonecutter](https://stonecutter.kikugie.dev/) to share one source tree across
Fabric, Forge, and NeoForge builds.

## Supported builds

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.16.3–1.16.4 | Yes | — | — |
| 1.16.5 | Yes | Yes | — |
| 1.17–1.17.1 | Yes | — | — |
| 1.18–1.18.1 | Yes | — | — |
| 1.18.2 | Yes | Yes | — |
| 1.19–1.19.1 | Yes | — | — |
| 1.19.2 | Yes | Yes | — |
| 1.19.3–1.20 | Yes | — | — |
| 1.20.1 | Yes | Yes | — |
| 1.20.2 | Yes | Yes | Yes |
| 1.20.3 | Yes | — | Yes |
| 1.20.4 | Yes | — | Yes |
| 1.20.5–1.20.6 | Yes | — | Yes |
| 1.21 | Yes | — | Yes |
| 1.21.1–1.21.8 | Yes | — | Yes |
| 1.21.9 | Yes | — | — |
| 1.21.10–1.21.11 | Yes | — | Yes |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | Yes | — | Yes |

NeoForge 1.21.9 is intentionally absent because Sodium does not publish a
compatible NeoForge artifact for that Minecraft release.

The Fabric mod ID is `slc-unofficial`. Forge and NeoForge use
`slc_unofficial` because those loaders do not permit hyphens in mod IDs. The
Java package is `com.sumirelabs.sodiumleafculling`; the resource namespace is
`sodiumleafculling`.

## Development

Use the checked-in Gradle wrapper. JDK 17 or newer can launch the wrapper; the
repository pins the Gradle daemon to Java 25 and can provision it through
Foojay when necessary. Compilation uses JDK 21 for older targets and JDK 25
for Minecraft 26.x, targeting the following Java bytecode/runtime versions:

- Minecraft 1.16.x: Java 8
- Minecraft 1.17.x: Java 16
- Minecraft 1.18.x–1.20.4: Java 17
- Minecraft 1.20.5–1.21.x: Java 21
- Minecraft 26.x: Java 25

Foojay can provision the daemon and compilation toolchains after Gradle starts,
but the wrapper still needs one valid launcher JDK. If it reports an invalid
`JAVA_HOME`, point that variable at an installed JDK 17+ (or put such a `java`
on `PATH`) and run the wrapper again.

Build one target from Windows PowerShell:

```powershell
.\gradlew.bat :1.21.11-fabric:build --configure-on-demand
```

Or from Linux/macOS:

```sh
bash ./gradlew :1.21.11-fabric:build --configure-on-demand
```

Replace `1.21.11-fabric` with any entry represented in the support table. To
build every registered target, run the checks, and collect only the
distributable jars (without sources jars), run:

```powershell
.\gradlew.bat build buildAndCollect --configure-on-demand -x sourcesJar -x remapSourcesJar
```

The distributable jars are collected under `build/libs/<mod-version>`. The two
`-x` options prevent new sources jars from being built, but do not remove a
sources jar left in that collection directory by an older run; use a fresh
collection directory when checking runtime-only output. Omit the exclusions
only when sources jars are also wanted locally.

Use `tasks` to inspect all available Gradle tasks and `projects` to list the
generated target projects:

```powershell
.\gradlew.bat tasks
.\gradlew.bat projects
```

## Build and release automation

The three workflows under `.github/workflows` share one verified set of
runtime jars:

1. **Build all versions** runs on branch pushes, pull requests, and manual
   dispatches. It builds all 63 Minecraft/loader targets in separate jobs,
   rejects sources/dev/plain jars, and exposes the combined
   `slc-unofficial-jars` artifact on the Actions run for 14 days.
2. **Release** runs for any pushed tag. The tag identifies the GitHub Release,
   while `mod.version` in the tagged commit determines the JAR and platform
   version. It rebuilds all targets once, creates a GitHub Release with
   generated notes, and attaches all 63 runtime jars plus `SHA256SUMS`.
3. **Publish to CurseForge and Modrinth** is called only after the GitHub
   Release succeeds. It publishes each target as its own platform version so
   its Minecraft version and loader metadata remain accurate. A manual run can
   select one platform and one exact target when retrying a partial failure.

Configure these GitHub repository variables before enabling platform uploads:

- `MODRINTH_PROJECT_ID`
- `CURSEFORGE_PROJECT_ID`

Configure `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` as secrets in a GitHub
Environment named `publishing` (repository secrets also work). For unattended
publishing, configure its deployment tag rules to match the tag convention you
intend to use, leave required reviewers disabled, and protect tag creation or
modification with a repository ruleset. GitHub approvals apply to individual
matrix deployments, so enabling required reviewers can require repeated
approval waves while the two-at-a-time publisher advances. A platform is
skipped when its project-ID variable is empty; if an ID exists but its token is
missing, that platform job fails instead of silently pretending to publish.

For a partial retry, dispatch **Publish to CurseForge and Modrinth**, choose the
failed platform, and enter its exact target such as `1.21.11-neoforge`. Leaving
the target blank republishes all 63 entries and will be rejected if some are
already present. If the `publishing` environment permits only tags, run the
retry on the tag ref instead of the default branch, for example:

```sh
gh workflow run release-to-cf-mr.yml --ref 3.0.0 \
  -f tag=3.0.0 -f target=1.21.11-neoforge \
  -f publish_modrinth=true -f publish_curseforge=false
```

To create version `3.0.0` after changing and validating `mod.version`:

```sh
git tag 3.0.0
git push origin 3.0.0
```

Use one release tag per `mod.version`. Pushing multiple alias tags such as
`3.0.0` and `v3.0.0` for the same tagged version produces identical Modrinth
and CurseForge version identifiers, so the later platform upload is rejected as
a duplicate.

All workflow actions are pinned to immutable commit SHAs. The publishing
workflow uses `mc-publish` 3.3.1 and does not rebuild source code or receive
either platform token until its dedicated `publishing` jobs start.

## Updating versions and dependencies

Project metadata and dependency pins are centralized in
[`stonecutter.properties.toml`](stonecutter.properties.toml). Global pins such
as Fabric Loader, MixinSquared, and MixinExtras live at the top of that file;
Minecraft- and loader-specific pins live in the corresponding version table.
The per-loader `mod.renderer_compat` values declare the minimum runtime Sodium
or Embeddium version for the API generation that each mixin was verified
against. Build dependencies remain pinned separately under `deps.sodium` or
`deps.embeddium` so builds stay reproducible while newer compatible renderer
releases are accepted at runtime.
All targets use the shared `mod.version` from `stonecutter.properties.toml`.
The new IDs and build layout are intentionally not release-compatible with
artifacts published under the original mod IDs.

When adding or updating a Minecraft target:

1. Update its dependency table in `stonecutter.properties.toml`.
2. Add or adjust the target registration in `settings.gradle.kts` and the
   support table above.
3. Keep the fixed target lists and expected JAR counts in `build.yml` and
   `release-to-cf-mr.yml` synchronized with Stonecutter.
4. Build the affected Fabric, Forge, or NeoForge project directly.
5. Run `build buildAndCollect` with the sources exclusions shown above, or
   dispatch **Build all versions** before merging a broad dependency update.

Forge 1.20.1 uses `mixins.sodiumleafculling.forge.json` so its production SRG
refmap is loaded; Fabric and NeoForge use `mixins.sodiumleafculling.json`.
Keep the two client-mixin lists aligned when adding or removing a mixin.

The Forge 1.20.1 and NeoForge 1.20.4 targets use the dedicated `embeddium`
Stonecutter branch. It changes Embeddium's initial `RenderType`-to-`Material`
selection for surrounded leaves, so the model is emitted once into the solid
terrain buffer instead of being manually rendered alongside the cutout copy.
The Forge 1.20.1 metadata also declares Xenon incompatible because Xenon already
includes equivalent leaf-culling functionality.

### Build backends

All 63 targets run inside the root Gradle 9 build and consume the shared
Stonecutter source tree. Loader/toolchain families share their scripts:

| Script | Targets / backend |
| --- | --- |
| `build.fabric.gradle.kts` | Fabric through 1.21.x / Fabric Loom remap |
| `build.fabric.unobfuscated.gradle.kts` | Fabric 26.x / Fabric Loom without remapping |
| `build.forge.gradle.kts` | Forge 1.16.5–1.20.1 / TauMC ModDevGradle |
| `build.neoloom.gradle.kts` | Forge 1.20.2 / NeoLoom |
| `build.neoforge.gradle.kts` | NeoForge 1.20.4, 1.20.6 and newer / TauMC ModDevGradle |
| `build.neogradle.gradle.kts` | NeoForge 1.20.2, 1.20.3, 1.20.5 / NeoGradle |

Fabric dependency, compiler, metadata and collection settings live in
`build.fabric-common.gradle.kts`. Both NeoForge backends use the
`neoforge-resources` convention in `buildSrc` for their metadata and Mixin
resources. There are no per-version nested Gradle builds or copied Java
source trees. `buildAndCollect` collects runtime JARs only.

Forge 1.16.5 uses TauMC's Mojang class-name support and compiles the shared
source to Java 8. MixinExtras is shaded and relocated because this loader
predates Jar-in-Jar; a Mixin config plugin initializes that private copy.
Forge 1.20.2 uses a Minecraft jar processor to correct the SRG member names
left in NeoLoom 1.18.5's development jar. Production output still goes through
NeoLoom's normal remapping task.

To configure only selected targets, pass an exact comma-separated list:

```powershell
.\gradlew.bat :1.18.2-fabric:build -Pbuild_targets=1.18.2-fabric
```

Omitting `build_targets` registers all 63 targets. The active Stonecutter
version remains registered when filtering, and unknown target names are
rejected. CI uses this filter for each matrix job.

CI prefetches Fabric's Minecraft client/server jars from Mojang and checks
their official SHA-1 values before Gradle starts. This avoids the concurrent
Loom download progress-logger failure observed with Gradle 9.8 on 1.18.2.
The prefetch script retries network failures and reuses verified cached jars.

Build-plugin versions are pinned in `settings.gradle.kts` and the backend
scripts. Update them deliberately and keep Gradle, Loom/ModDevGradle/NeoGradle,
and Java compatibility requirements aligned.

### Stonecutter 0.10

The build uses **0.10-alpha.12** from KikuGie's snapshots repository. This is
a prerelease; keep the plugin version in `settings.gradle.kts` and the API
dependency in `buildSrc/build.gradle.kts` identical when updating it.

Target metadata is read through `sc.properties` (or `StonecutterBuildExtension`
in convention plugins). Automatic exposure as Gradle project properties is
disabled. Optional strings use `getOrNull(name)` without a type parameter;
typed values use the 0.10 `getAs`/`getAsOrNull` API.

The root `build` and `buildAndCollect` tasks use `stonecutter.tasks.names` to
depend on target task paths lazily. Source generation is connected through
Stonecutter's source sets, without depending on its internal task API.

## License

This project is licensed under the [MIT License](LICENSE.md).

