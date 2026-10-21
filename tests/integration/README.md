# Config registration smoke test

Run the real Fabric Loader and Mixin environment, then build this mod's options
using the renderer's own configuration builders. No game window is opened and
the test instance exits before Minecraft starts. Test mods and configuration
files are isolated under `run/config-smoke/<target>`.

```powershell
.\gradlew.bat :26.3-fabric:configRegistrationSmoke -Pbuild_targets=26.3-fabric --configure-on-demand --no-configuration-cache --init-script tests/integration/config-smoke.init.gradle
```

Replace the target to check other Fabric versions. This verifies:

- The options Mixin is applied and registers all three settings.
- Defaults are Solid, distance culling off, and 64 blocks.
- The modern integer option has its required value formatter.
- Slider availability follows unapplied toggle changes in both UI generations.
- Legacy disabled controls reject value edits and retain the distance value.

For the active Stonecutter target (currently `26.2-fabric`), also check the CI
sequence: a normal `build buildAndCollect` invocation followed by this test in
a separate Gradle invocation. The smoke-test source sets exclude shared original
paths so JavaExec can request generated sources without compiling or copying
both versions. A JVM argument file keeps long classpaths visible to Fabric Loader
on Windows instead of hiding them in Gradle's classpath-only JAR.

The modern test allocates only the config hook's owner without its vanilla GUI
constructor, supplies in-memory settings, and invokes the injected hook. This
exercises real option validation without needing a window or graphics context.
It does not replace full client startup, rendering, or world tests. Forge and
NeoForge are outside this Fabric-specific harness.

Legacy pages that cache the Minecraft window receive a constructor-free client
singleton with a null window. Only this mod's injected options hook is executed;
vanilla option pages and the native window are not constructed.
