# [4.0.1]

- Restore the original green/cyan leaf icon, resized to 128x128 pixels and
  optimized as a PNG.
- Keep leaf faces at distance-mode boundaries and between independently rebuilt
  terrain sections to prevent visible gaps during near/far transitions.
- Rebuild terrain only when recorded leaf blocks actually change distance mode.
  Limit rebuild requests independently of frame rate, deduplicate pending work, and
  ignore stale worker results to reduce transition-related rebuild spikes.
- Prioritize nearby leaves returning from Fast mode after rapid movement, and
  prevent deferred offscreen rebuilds from blocking updates to other sections.

# [4.0.0]

- Avoid duplicate shared/generated sources and resources when running config
  smoke tests for Stonecutter's active target, including Fabric 26.2. Pass the
  smoke-test classpath through a JVM argument file for long Windows paths.
- Implement Embeddium's additional option-name accessor on Forge 1.16.5 to
  fix compilation of the dependent distance slider.
- Retry transient dependency-download failures in CI, including upstream HTTP
  502 errors, while preserving immediate failures for compilation errors.
- Change the project license from MIT to Blue Oak Model License 1.0.0.
- Rename the mod to Sodium/Embeddium LeafCulling and unify its Fabric, Forge,
  and NeoForge mod IDs as `seleafculling`. Replace the old mod JAR when updating.
- Replace the mod icon with the new artwork from `icon.png`, resized to
  128x128 pixels and losslessly optimized as a PNG.
- Change the default Leaf Culling Mode from Solid Aggressive to Solid.
  Existing saved mode selections are preserved.
- Rewrite the README with updated features, requirements, credits, and
  the reason for maintaining this fork.
- Add optional distance-based leaf culling to Sodium and Embeddium settings.
  Disabled by default, it keeps the selected culling mode within a configurable
  distance (64 blocks by default) and renders farther leaves as opaque Fast
  leaves. Camera movement queues affected terrain sections for rebuilding.
- Disable the distance slider while distance-based culling is off, following
  unapplied toggle changes in both modern and legacy settings screens.
- Set the modern distance slider's required value formatter to prevent
  configuration registration crashes, and add Fabric config registration
  smoke tests covering both settings API generations.
- Fix the performance-options Mixin for Sodium 0.9.3-beta.1 on Minecraft 26.3,
  accommodating its additional method argument while retaining compatibility
  with earlier Sodium versions.

# [3.3.0]

- Add Fabric support for Minecraft 26.2-rc-1/rc-2 and 26.3-rc-1/rc-2,
  including RC-aware target parsing, CI downloads and publishing filenames.
- Move Java packages and the Maven group to `com.sumirelabs`, including Mixin
  entrypoints and the relocated MixinExtras copy for legacy Forge.
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
