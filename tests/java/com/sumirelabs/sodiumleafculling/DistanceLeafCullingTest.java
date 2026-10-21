package com.sumirelabs.sodiumleafculling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Standalone regression tests, also run by CI without Minecraft or Gradle. */
public final class DistanceLeafCullingTest {
    private static final long TICK = 50_000_000L;

    public static void main(String[] args) throws Exception {
        boundaryAndBuildSnapshot();
        transitionFacesStayVisible();
        unchangedBoundaryLeavesDoNotRebuild();
        movementAndRetries();
        worldResetAndCancelledBuild();
        staleResultsDoNotReplaceNewerMeshes();
        frameRateIndependentBudget();
        deferredSectionsDoNotBlockNearbyLeaves();
        nearbyLeavesTakePriorityAfterFastMovement();
        movementDuringRebuildConverges();
        negativeSectionCoordinates();
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
        update(culling, true, 0.5, 0.5, 0.5, TICK, rebuilds);
        check(!culling.isDistant(1000, 0, 0), "item rendering unaffected");
        culling.beginBuild();
        check(!culling.isDistant(64, 0, 0), "exact boundary stays near");
        check(culling.isDistant(65, 0, 0), "beyond boundary is fast");
        check(culling.isDistant(0, 65, 0), "height counts");
        check(!culling.isDistant(-64, 0, 0), "negative coordinates");
        update(culling, true, 100.5, 0.5, 0.5, 2 * TICK, rebuilds);
        check(culling.isDistant(65, 0, 0), "movement cannot change an active build");
        culling.endBuild(true);
        culling.beginBuild();
        check(!culling.isDistant(65, 0, 0), "new build sees new camera");
        culling.endBuild(true);
    }

    private static void transitionFacesStayVisible() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        culling.beginBuild();
        check(!culling.canCullAdjacent(65, 0, 0, 64, 0, 0), "opaque/fancy boundary retains its face");
        check(culling.canCullAdjacent(66, 0, 0, 65, 0, 0), "two far leaves in one mesh can cull");
        check(!culling.canCullAdjacent(80, 0, 0, 79, 0, 0), "independently uploaded sections retain their faces");
        check(!culling.canCullAdjacent(0, 80, 0, 0, 79, 0), "vertical section boundaries retain faces");
        check(!culling.canCullAdjacent(-81, 0, 0, -80, 0, 0), "negative section boundaries retain faces");
        update(culling, true, 100.5, 0.5, 0.5, TICK, rebuilds);
        check(culling.canCullAdjacent(66, 0, 0, 65, 0, 0), "face decisions use the same mesh snapshot");
        culling.endBuild(true);
        check(!culling.canCullAdjacent(66, 0, 0, 65, 0, 0), "no distance face culling outside a mesh build");
    }

    private static void unchangedBoundaryLeavesDoNotRebuild() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 63, 0, 0, true);
        // The section intersects the sphere in both positions, but its only leaf remains near.
        for (int i = 0; i < 200; i++) {
            update(culling, true, i % 2 == 0 ? 1.5 : 0.5, 0.5, 0.5, (i + 1) * TICK, rebuilds);
        }
        check(rebuilds.isEmpty(), "boundary sections do not rebuild unless an actual leaf changes");
    }

    private static void movementAndRetries() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 80, 0, 0, true);
        build(culling, 0, 0, 0, true);
        update(culling, true, 0.5, 0.5, 0.5, TICK, rebuilds);
        check(rebuilds.isEmpty(), "stationary camera does not rebuild");
        update(culling, true, 0.6, 0.5, 0.5, 2 * TICK, rebuilds);
        check(rebuilds.isEmpty(), "sub-block jitter does not rebuild");
        update(culling, true, 32.5, 0.5, 0.5, 3 * TICK, rebuilds);
        check(rebuilds.size() == 1 && rebuilds.get(0).equals("5,0,0"), "only a changed leaf section rebuilds");
        update(culling, true, 32.5, 0.5, 0.5, 4 * TICK, rebuilds);
        check(rebuilds.size() == 1, "pending builds are not flooded");
        update(culling, true, 32.5, 0.5, 0.5, 4_000_000_000L, rebuilds);
        check(rebuilds.size() == 1, "slow workers are not cancelled every second");
        update(culling, true, 32.5, 0.5, 0.5, 6_000_000_000L, rebuilds);
        check(rebuilds.size() == 2, "dropped work is eventually retried");
        build(culling, 80, 0, 0, true);
        update(culling, true, 32.5, 0.5, 0.5, 7_000_000_000L, rebuilds);
        check(rebuilds.size() == 2, "finished rebuild stops retries");
        update(culling, false, 32.5, 0.5, 0.5, 8_000_000_000L, rebuilds);
        culling.beginBuild();
        check(!culling.isDistant(1000, 0, 0), "disable returns to normal");
        culling.endBuild(true);
    }

    private static void worldResetAndCancelledBuild() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0, 0, 0, 0, rebuilds);
        culling.beginBuild();
        culling.isDistant(80, 0, 0);
        culling.reset();
        culling.endBuild(true);
        update(culling, true, 32, 0, 0, TICK, rebuilds);
        check(rebuilds.isEmpty(), "old-world results ignored");
        build(culling, 96, 0, 0, false);
        update(culling, true, 64, 0, 0, 2 * TICK, rebuilds);
        check(rebuilds.isEmpty(), "cancelled initial build ignored");
    }

    private static void staleResultsDoNotReplaceNewerMeshes() throws Exception {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        Thread oldWorker = new Thread(() -> {
            culling.beginBuild();
            culling.isDistant(80, 0, 0);
            started.countDown();
            try { finish.await(); } catch (InterruptedException ex) { throw new AssertionError(ex); }
            culling.endBuild(true);
        });
        oldWorker.setDaemon(true);
        oldWorker.start();
        check(started.await(5, TimeUnit.SECONDS), "old worker starts");
        update(culling, true, 32.5, 0.5, 0.5, TICK, rebuilds);
        build(culling, 80, 0, 0, true);
        update(culling, true, 32.5, 0.5, 0.5, 2 * TICK, rebuilds);
        finish.countDown();
        oldWorker.join(5000);
        check(!oldWorker.isAlive(), "old worker finishes");
        update(culling, true, 32.5, 0.5, 0.5, 3 * TICK, rebuilds);
        check(rebuilds.isEmpty(), "late stale results cannot overwrite the newer classification");
    }

    private static void frameRateIndependentBudget() {
        int low = runAtFrameRate(60), high = runAtFrameRate(240);
        check(low == high && high == 20, "rebuild rate is bounded and independent of FPS");
    }

    private static int runAtFrameRate(int fps) {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0, 0, 0, 0, rebuilds);
        seedBoundary(culling);
        for (int frame = 0; frame <= fps; frame++) {
            long now = frame * 1_000_000_000L / fps;
            culling.update(true, 64, 80, 0, 0, 32, now, (x, y, z) -> {
                rebuilds.add(x + "," + y + "," + z);
                build(culling, x * 16, y * 16, z * 16, true);
            });
        }
        return rebuilds.size();
    }

    private static void deferredSectionsDoNotBlockNearbyLeaves() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 0, 0, 0, true);
        build(culling, 16, 0, 0, true);
        build(culling, 256, 0, 0, true);
        update(culling, true, 128.5, 0.5, 0.5, TICK, rebuilds);
        update(culling, true, 128.5, 0.5, 0.5, 2 * TICK, rebuilds);
        check(rebuilds.size() == 2, "two old sections wait for renderer visibility");
        // Sodium can defer offscreen rebuilds indefinitely. They are dirty marks, not active jobs.
        update(culling, true, 256.5, 0.5, 0.5, 3 * TICK, rebuilds);
        check(rebuilds.size() == 3 && rebuilds.get(2).equals("16,0,0"),
                "deferred offscreen sections cannot block nearby leaf restoration");
        for (int tick = 4; tick <= 20; tick++) update(culling, true, 256.5, 0.5, 0.5, tick * TICK, rebuilds);
        check(rebuilds.size() == 3, "outstanding section requests are still deduplicated");
    }

    private static void nearbyLeavesTakePriorityAfterFastMovement() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 0, 0, 0, true);
        build(culling, 16, 0, 0, true);
        build(culling, 80, 0, 0, true);
        build(culling, 112, 0, 0, true);
        update(culling, true, 112.5, 0.5, 0.5, TICK, rebuilds);
        check(rebuilds.size() == 1 && rebuilds.get(0).equals("7,0,0"),
                "closest returning leaves precede old distant work and farther returning leaves");
        build(culling, 112, 0, 0, true);
        update(culling, true, 112.5, 0.5, 0.5, 2 * TICK, rebuilds);
        check(rebuilds.size() == 2 && rebuilds.get(1).equals("5,0,0"),
                "remaining returning leaves precede switching old nearby leaves to Fast");
    }

    private static void seedBoundary(DistanceLeafCulling culling) {
        for (int i = 0; i < 20; i++) build(culling, 80, (i % 5 - 2) * 16, (i / 5 - 2) * 16, true);
    }

    private static void movementDuringRebuildConverges() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, 0, 0, 0, true);
        update(culling, true, 128.5, 0.5, 0.5, TICK, rebuilds);
        culling.beginBuild();
        check(culling.isDistant(0, 0, 0), "old camera's requested mesh starts as Fast");
        update(culling, true, 0.5, 0.5, 0.5, 2 * TICK, rebuilds);
        culling.endBuild(true);
        update(culling, true, 0.5, 0.5, 0.5, 3 * TICK, rebuilds);
        check(rebuilds.size() == 2, "returning while a worker runs schedules a corrective mesh");
        build(culling, 0, 0, 0, true);
        for (int tick = 4; tick <= 200; tick++) update(culling, true, 0.5, 0.5, 0.5, tick * TICK, rebuilds);
        check(rebuilds.size() == 2, "stopping after rapid movement converges without repeated rebuilds");
    }

    private static void negativeSectionCoordinates() {
        DistanceLeafCulling culling = new DistanceLeafCulling();
        List<String> rebuilds = new ArrayList<>();
        update(culling, true, 0.5, 0.5, 0.5, 0, rebuilds);
        build(culling, -65, 16, 31, true);
        update(culling, true, -64.5, 16.5, 31.5, TICK, rebuilds);
        check(rebuilds.size() == 1 && rebuilds.get(0).equals("-5,1,1"), "leaf masks preserve negative coordinates and axes");
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
        culling.update(enabled, 64, x, y, z, 32, now, (sx, sy, sz) -> rebuilds.add(sx + "," + sy + "," + sz));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
