# [Unreleased]
- Short-circuit leaf-depth checks, reuse neighbor positions and the selected
  quality, and remove redundant solid-side checks without changing culling rules.
- Omit empty renderer-accessor mixins on targets that do not use them and
  omit the Forge/NeoForge mod entry class from Fabric JARs.
- Losslessly compress the mod logo while preserving its dimensions and pixels.

# [3.2.0]
- Include library locations in NeoGradle's cache keys to prevent restored
  library lists from referencing removed nested builds or previous checkouts.
- Migrate to Stonecutter 0.10-alpha.12, including its structured-property API,
  lazy task aggregation and Gradle Property-based version configuration.
- Disable automatic Gradle project-property exposure and use Stonecutter's
  metadata API in all backends and the shared NeoForge resource convention.
- Connect source generation through source sets rather than internal task names.
- Consolidate all targets into the root Gradle build using shared Fabric Loom,
  TauMC ModDevGradle, NeoGradle and NeoLoom backends; remove nested wrappers
  and the duplicated Forge 1.16.5 source tree.
- Shade and initialize MixinExtras for Forge 1.16.5 and correct Forge 1.20.2
  development mappings without changing production remapping.
- Configure only the requested CI target and prefetch SHA-1-verified Mojang
  jars to avoid Loom's concurrent download progress failure on 1.18.2.
- Pin loader-specific Embeddium file IDs and share NeoForge resource handling.
- Add Minecraft 26.3 support for Fabric and NeoForge, using Sodium 0.9.2.
- Update Stonecutter, Loom Back Compat, Loom, ModDevGradle, Gradle wrappers,
  Fabric Loader, MixinSquared, MixinExtras, and available stable dependencies
  for existing Minecraft targets.
- Run the root build on Java 25 and legacy Gradle 8 bridges on Java 21.
- Collect only the current version's runtime JAR from legacy builds, excluding
  artifacts left over from earlier builds.
- Expand legacy NeoForge 1.20.2/1.20.3 metadata and Mixin resources before
  launching their nested builds, using a shared resource convention.
- Document the purpose and maintenance tradeoffs of legacy loader builds.

# [3.1.1]
- Fix Xenon incompatibility metadata to reference the correct Embeddium fork on Modrinth and CurseForge.
- Correct Xenon's dependency type from optional to incompatible.

# [3.1.0]
- expand version support
    - Fabric: 1.16.3～1.20、1.20.2、1.20.3、1.20.5、1.20.6、1.21
    - Forge: 1.16.5、1.18.2、1.19.2、1.20.2
    - NeoForge: 1.20.2、1.20.3、1.20.5、1.20.6、1.21
- Fix loader-specific metadata, mixins, renderer integration, and runtime dependencies
- Support multiple generations of the Sodium and Embeddium APIs

# [3.0.0]

- rewrote the project from scratch using Stonecutter 9.x.
- Optimized the rendering path when using Embeddium. Performance has improved slightly.
- Xenon has been officially marked as incompatible.
- Support for more versions/loaders
