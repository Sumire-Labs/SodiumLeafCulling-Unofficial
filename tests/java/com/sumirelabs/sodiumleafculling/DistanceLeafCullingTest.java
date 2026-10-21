package com.sumirelabs.sodiumleafculling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Standalone regression tests, also run by CI without Minecraft or Gradle. */
public final class DistanceLeafCullingTest {
    public static void main(String[] args) throws Exception {
        boundaryAndBuildSnapshot();
        movementAndRetries();
        worldResetAndCancelledBuild();
        rebuildBudget();
        classificationIsConservative();
        check(DistanceLeafCulling.clampDistance(-10) == 1, "minimum distance");
        check(DistanceLeafCulling.clampDistance(10000) == 256, "maximum distance");
        System.out.println("Distance leaf culling tests passed");
    }

    private static void boundaryAndBuildSnapshot() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, false, 0.5, 0.5, 0.5, 0, rebuilds);
        culling.beginBuild();
        check(!culling.isDistant(1000, 0, 0), "disabled by default");
        culling.endBuild(true);
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        check(!culling.isDistant(1000, 0, 0), "item rendering unaffected");
        culling.beginBuild();
        check(!culling.isDistant(64, 0, 0), "exact boundary stays near");
        check(culling.isDistant(65, 0, 0), "beyond boundary is fast");
        check(culling.isDistant(0, 65, 0), "height counts");
        check(!culling.isDistant(-64, 0, 0), "negative coordinates");
        update(culling, true, 100.5, 0.5, 0.5, 1, rebuilds);
        check(!culling.isDistant(64, 0, 0), "build uses a fixed snapshot");
        check(culling.isDistant(65, 0, 0), "movement cannot change an active build");
        culling.endBuild(true);
        culling.beginBuild();
        check(!culling.isDistant(65, 0, 0), "new build sees new camera");
        culling.endBuild(true);
    }

    private static void movementAndRetries() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 80, 0, 0, true);
        build(culling, 0, 0, 0, true);
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        check(rebuilds.isEmpty(), "stationary camera does not rebuild");
        update(culling, true, 0.6, 0.5, 0.5, 1, rebuilds);
        check(rebuilds.isEmpty(), "sub-block jitter does not rebuild");
        update(culling, true, 32.5, 0.5, 0.5, 2, rebuilds);
        check(rebuilds.size() == 1 && rebuilds.get(0).equals("5,0,0"), "only changed section rebuilds");
        update(culling, true, 32.5, 0.5, 0.5, 3, rebuilds);
        check(rebuilds.size() == 1, "pending builds are not flooded");
        update(culling, true, 32.5, 0.5, 0.5, 1_000_000_003L, rebuilds);
        check(rebuilds.size() == 2, "cancelled or dropped work is retried");
        build(culling, 80, 0, 0, true);
        update(culling, true, 32.5, 0.5, 0.5, 2_000_000_004L, rebuilds);
        check(rebuilds.size() == 2, "finished rebuild stops retries");
        update(culling, false, 32.5, 0.5, 0.5, 3_000_000_005L, rebuilds);
        culling.beginBuild();
        check(!culling.isDistant(1000, 0, 0), "disable returns to normal");
        culling.endBuild(true);
    }

    private static void worldResetAndCancelledBuild() throws Exception {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0, 0, 0, 0, rebuilds);
        culling.beginBuild();
        culling.isDistant(80, 0, 0);
        culling.reset();
        culling.endBuild(true);
        update(culling, true, 32, 0, 0, 1, rebuilds);
        check(rebuilds.isEmpty(), "old-world results ignored");
        build(culling, 96, 0, 0, false);
        update(culling, true, 64, 0, 0, 2, rebuilds);
        check(rebuilds.isEmpty(), "cancelled initial build ignored");
        Thread worker = new Thread(() -> build(culling, 144, 0, 0, true));
        worker.start();
        worker.join();
        update(culling, true, 112, 0, 0, 3, rebuilds);
        check(rebuilds.size() == 1, "worker results reach render thread");
    }

    private static void rebuildBudget() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0, 0, 0, 0, rebuilds);
        for (int i = 0; i < 20; i++) {
            build(culling, 80, (i % 5 - 2) * 16, (i / 5 - 2) * 16, true);
        }
        update(culling, true, 80, 0, 0, 1, rebuilds);
        check(rebuilds.size() == 8, "per-frame rebuild budget");
        update(culling, true, 80, 0, 0, 2, rebuilds);
        check(rebuilds.size() > 8, "remaining work drains on later frames");
    }

    private static void classificationIsConservative() {
        Random random = new Random(271828);
        Object world = new Object();
        for (int sample = 0; sample < 500; sample++) {
            DistanceLeafCulling.Snapshot a = new DistanceLeafCulling.Snapshot(world, true,
                    1 + random.nextInt(256), random.nextDouble() * 256 - 128,
                    random.nextDouble() * 256 - 128, random.nextDouble() * 256 - 128);
            DistanceLeafCulling.Snapshot b = new DistanceLeafCulling.Snapshot(world, true,
                    1 + random.nextInt(256), random.nextDouble() * 256 - 128,
                    random.nextDouble() * 256 - 128, random.nextDouble() * 256 - 128);
            int sx = random.nextInt(16) - 8, sy = random.nextInt(16) - 8, sz = random.nextInt(16) - 8;
            if (!DistanceLeafCulling.mayChange(a, b, sx, sy, sz)) {
                for (int i = 0; i < 4096; i++) {
                    double x = sx * 16 + (i & 15) + 0.5;
                    double y = sy * 16 + (i >> 4 & 15) + 0.5;
                    double z = sz * 16 + (i >> 8) + 0.5;
                    check(a.isDistant(x, y, z) == b.isDistant(x, y, z), "no changed leaf may be missed");
                }
            }
        }
    }

    private static void build(DistanceLeafCulling culling, int x, int y, int z, boolean success) {
        culling.beginBuild();
        culling.isDistant(x, y, z);
        culling.endBuild(success);
    }

    private static void update(DistanceLeafCulling culling, boolean enabled, double x, double y, double z,
                               long now, List<String> rebuilds) {
        culling.update(enabled, 64, x, y, z, 32, now,
                (sx, sy, sz) -> rebuilds.add(sx + "," + sy + "," + sz));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
