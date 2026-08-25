# SodiumLeafCulling-Unofficial

An unofficial, multi-version fork of Sodium Leaf Culling. The project uses
[Stonecutter](https://stonecutter.kikugie.dev/) to share one source tree across
Fabric, Forge, and NeoForge builds.

## Supported builds

| Minecraft | Fabric | Forge | NeoForge |
| --- | :---: | :---: | :---: |
| 1.20.1 | Yes | Yes | — |
| 1.20.4 | Yes | — | Yes |
| 1.21.1–1.21.8 | Yes | — | Yes |
| 1.21.9 | Yes | — | — |
| 1.21.10–1.21.11 | Yes | — | Yes |
| 26.1, 26.1.1, 26.1.2, 26.2 | Yes | — | Yes |

NeoForge 1.21.9 is intentionally absent because Sodium does not publish a
compatible NeoForge artifact for that Minecraft release.

The Fabric mod ID is `slc-unofficial`. Forge and NeoForge use
`slc_unofficial` because those loaders do not permit hyphens in mod IDs. The
internal Java package and resource namespace remain `sodiumleafculling`.

## Development

Use the checked-in Gradle wrapper. JDK 17 or newer can launch the wrapper; the
repository pins the Gradle daemon to Java 21 and can provision it through
Foojay when necessary. Compilation toolchains are also selected per Minecraft
version and downloaded automatically:

- Minecraft 1.20.x: Java 17
- Minecraft 1.21.x: Java 21
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
build every registered target and collect the distributable jars, run:

```powershell
.\gradlew.bat buildAndCollect --configure-on-demand
```

The distributable and sources jars are collected under
`build/libs/<mod-version>`.

Use `tasks` to inspect all available Gradle tasks and `projects` to list the
generated target projects:

```powershell
.\gradlew.bat tasks
.\gradlew.bat projects
```

## Updating versions and dependencies

Project metadata and dependency pins are centralized in
[`stonecutter.properties.toml`](stonecutter.properties.toml). Global pins such
as Fabric Loader, MixinSquared, and MixinExtras live at the top of that file;
Minecraft- and loader-specific pins live in the corresponding version table.
The per-loader `mod.renderer_compat` values also keep runtime Sodium or
Embeddium versions on the API generation that each mixin was verified against.
All targets currently use mod version `3.0.0` because the new IDs and build
layout are intentionally not release-compatible with earlier artifacts.

When adding or updating a Minecraft target:

1. Update its dependency table in `stonecutter.properties.toml`.
2. Add or adjust the target registration in `settings.gradle.kts`.
3. Build the affected Fabric, Forge, or NeoForge project directly.
4. Run `buildAndCollect`, or dispatch the **Build** workflow with
   `full_matrix` enabled before merging a broad dependency update.

Forge 1.20.1 uses `mixins.sodiumleafculling.forge.json` so its production SRG
refmap is loaded; Fabric and NeoForge use `mixins.sodiumleafculling.json`.
Keep the two client-mixin lists aligned when adding or removing a mixin.

The Forge 1.20.1 and NeoForge 1.20.4 targets use the dedicated `embeddium`
Stonecutter branch. It changes Embeddium's initial `RenderType`-to-`Material`
selection for surrounded leaves, so the model is emitted once into the solid
terrain buffer instead of being manually rendered alongside the cutout copy.
The Forge 1.20.1 metadata also declares Xenon incompatible because Xenon already
includes equivalent leaf-culling functionality.

Build-plugin versions are kept in `settings.gradle.kts`, the loader-specific
build scripts, and `gradle/wrapper/gradle-wrapper.properties`. Update them
deliberately and keep the Gradle, Loom/ModDevGradle, and Java compatibility
requirements aligned.

## License

This project is licensed under the [MIT License](LICENSE.md).

