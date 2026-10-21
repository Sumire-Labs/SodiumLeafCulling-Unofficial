package com.sumirelabs.sodiumleafculling;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/** Distance decisions are fixed for a mesh build; only the render thread schedules rebuilds. */
public final class DistanceLeafCulling {
    public static final int DEFAULT_DISTANCE = 64;
    public static final int MIN_DISTANCE = 1;
    public static final int MAX_DISTANCE = 256;
    private static final long UPDATE_INTERVAL_NANOS = 50_000_000L;
    private static final long RETRY_NANOS = 5_000_000_000L;

    public interface RebuildScheduler {
        void rebuild(int x, int y, int z);
    }

    private volatile Snapshot snapshot = new Snapshot(new Object(), false, 64, 0, 0, 0);
    private final ThreadLocal<Build> build = new ThreadLocal<>();
    private final ConcurrentLinkedQueue<Section> completed = new ConcurrentLinkedQueue<>();
    private final Map<Long, Section> sections = new LinkedHashMap<>();
    private final AtomicLong buildSequence = new AtomicLong();
    private boolean evaluated;
    private long evaluatedAt;

    public static int clampDistance(int distance) {
        return Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    /** Called when the renderer reloads or changes worlds. Old worker results are ignored. */
    public void reset() {
        Snapshot previous = snapshot;
        snapshot = new Snapshot(new Object(), previous.enabled, previous.distance, previous.x, previous.y, previous.z);
        sections.clear();
        completed.clear();
        evaluated = false;
    }

    public void beginBuild() {
        Snapshot current = snapshot;
        if (current.enabled) {
            build.set(new Build(current, buildSequence.incrementAndGet()));
        } else {
            build.remove();
        }
    }

    public void endBuild(boolean success) {
        Build current = build.get();
        build.remove();
        if (success && current != null && current.sections != null) {
            completed.addAll(current.sections);
        }
    }

    /** Outside a terrain build (for example inventory rendering), distance culling is inactive. */
    public boolean isDistant(int x, int y, int z) {
        Build current = build.get();
        if (current == null) {
            return false;
        }
        return current.isDistant(x, y, z);
    }

    /** Both faces must belong to one mesh; neighboring sections can swap meshes independently. */
    public boolean canCullAdjacent(int x, int y, int z, int otherX, int otherY, int otherZ) {
        Build current = build.get();
        return current != null && (x >> 4) == (otherX >> 4) && (y >> 4) == (otherY >> 4) && (z >> 4) == (otherZ >> 4)
                && current.snapshot.isDistant(x + 0.5, y + 0.5, z + 0.5)
                && current.snapshot.isDistant(otherX + 0.5, otherY + 0.5, otherZ + 0.5);
    }

    /** Camera movement is sampled in one-block increments to avoid rebuilding for sub-block jitter. */
    public void update(boolean enabled, int distance, double x, double y, double z,
                       int renderDistance, long now, RebuildScheduler scheduler) {
        distance = clampDistance(distance);
        Snapshot previous = snapshot;
        double dx = x - previous.x, dy = y - previous.y, dz = z - previous.z;
        if (enabled != previous.enabled || distance != previous.distance ||
                (enabled && dx * dx + dy * dy + dz * dz >= 1.0)) {
            snapshot = new Snapshot(previous.world, enabled, distance, x, y, z);
        }
        Snapshot current = snapshot;
        Section result;
        while ((result = completed.poll()) != null) {
            if (result.built.world == current.world && current.enabled) {
                long key = key(result.x, result.y, result.z);
                Section previousResult = sections.get(key);
                if (previousResult != null) {
                    if (result.sequence < previousResult.sequence) continue;
                    if (previousResult.requested && result.sequence <= previousResult.requestSequence) {
                        // A build started before our request cannot acknowledge that request.
                        result.requested = true;
                        result.requestedAt = previousResult.requestedAt;
                        result.requestSequence = previousResult.requestSequence;
                    }
                }
                sections.put(key, result);
            }
        }
        if (!enabled) {
            sections.clear();
            evaluated = false;
            return;
        }

        // A fixed time budget avoids multiplying rebuild traffic by the client's FPS.
        if (evaluated && now - evaluatedAt < UPDATE_INTERVAL_NANOS) return;
        evaluated = true;
        evaluatedAt = now;

        int cameraX = ((int) Math.floor(x)) >> 4;
        int cameraZ = ((int) Math.floor(z)) >> 4;
        Section next = null;
        double nextDistance = Double.POSITIVE_INFINITY;
        Iterator<Section> iterator = sections.values().iterator();
        while (iterator.hasNext()) {
            Section section = iterator.next();
            // Sections outside the loaded horizontal range will be built again if loaded later.
            if (Math.abs(section.x - cameraX) > renderDistance + 2 ||
                    Math.abs(section.z - cameraZ) > renderDistance + 2) {
                iterator.remove();
                continue;
            }
            if (section.requested && now - section.requestedAt >= RETRY_NANOS) {
                section.requested = false;
            }
            if (section.checked != current) {
                section.dirty = section.changesAt(current);
                section.checked = current;
                if (!section.dirty) {
                    section.built = current;
                }
            }
            if (section.dirty && !section.requested) {
                double distanceSquared = section.distanceSquared(x, y, z);
                if (next == null || (section.restoresNear && !next.restoresNear) ||
                        (section.restoresNear == next.restoresNear && distanceSquared < nextDistance)) {
                    next = section;
                    nextDistance = distanceSquared;
                }
            }
        }

        // These requests mark sections dirty; Sodium may defer offscreen sections without starting
        // a worker. Never let those deferred requests reserve a global worker slot. Re-evaluate
        // priority at the current camera position instead of draining an old movement's FIFO.
        if (next != null) {
            next.requested = true;
            next.requestedAt = now;
            next.requestSequence = buildSequence.get();
            scheduler.rebuild(next.x, next.y, next.z);
            // At most one new request per 50 ms, including high-FPS clients.
        }
    }

    static boolean mayChange(Snapshot before, Snapshot after, int x, int y, int z) {
        if (before == after) {
            return false;
        }
        int a = before.classify(x, y, z), b = after.classify(x, y, z);
        // A section intersecting either sphere may contain leaves that crossed the threshold.
        return a == 0 || b == 0 || a != b;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3fffffL) << 42 | ((long) z & 0x3fffffL) << 20 | ((long) y & 0xfffffL);
    }

    static final class Snapshot {
        final Object world;
        final boolean enabled;
        final int distance;
        final double x, y, z;

        Snapshot(Object world, boolean enabled, int distance, double x, double y, double z) {
            this.world = world;
            this.enabled = enabled;
            this.distance = distance;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean isDistant(double bx, double by, double bz) {
            double dx = bx - x, dy = by - y, dz = bz - z;
            return enabled && dx * dx + dy * dy + dz * dz > (double) distance * distance;
        }

        /** -1: entirely near, 1: entirely far, 0: intersects the distance boundary. */
        int classify(int sx, int sy, int sz) {
            if (!enabled) {
                return -1;
            }
            double cx = sx * 16.0 + 8, cy = sy * 16.0 + 8, cz = sz * 16.0 + 8;
            double dx = Math.abs(cx - x), dy = Math.abs(cy - y), dz = Math.abs(cz - z);
            double minX = Math.max(0, dx - 7.5), minY = Math.max(0, dy - 7.5), minZ = Math.max(0, dz - 7.5);
            double radiusSquared = (double) distance * distance;
            if (minX * minX + minY * minY + minZ * minZ > radiusSquared) {
                return 1;
            }
            double maxX = dx + 7.5, maxY = dy + 7.5, maxZ = dz + 7.5;
            return maxX * maxX + maxY * maxY + maxZ * maxZ <= radiusSquared ? -1 : 0;
        }
    }

    private static final class Build {
        final Snapshot snapshot;
        final long sequence;
        List<Section> sections;
        Section last;
        boolean hasLastBlock, lastDistant;
        int lastX, lastY, lastZ;

        Build(Snapshot snapshot, long sequence) {
            this.snapshot = snapshot;
            this.sequence = sequence;
        }

        boolean isDistant(int x, int y, int z) {
            if (hasLastBlock && lastX == x && lastY == y && lastZ == z) return lastDistant;
            hasLastBlock = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastDistant = snapshot.isDistant(x + 0.5, y + 0.5, z + 0.5);
            Section section = section(x >> 4, y >> 4, z >> 4);
            int index = (y & 15) << 8 | (z & 15) << 4 | x & 15;
            (lastDistant ? section.distantLeaves : section.nearLeaves).set(index);
            return lastDistant;
        }

        Section section(int x, int y, int z) {
            if (last != null && last.x == x && last.y == y && last.z == z) {
                return last;
            }
            if (sections == null) {
                sections = new ArrayList<>(1);
            }
            for (Section section : sections) {
                if (section.x == x && section.y == y && section.z == z) {
                    last = section;
                    return last;
                }
            }
            last = new Section(x, y, z, snapshot, sequence);
            sections.add(last);
            return last;
        }
    }

    private static final class Section {
        final int x, y, z;
        final long sequence;
        final BitSet nearLeaves = new BitSet();
        final BitSet distantLeaves = new BitSet();
        Snapshot built, checked;
        boolean dirty, requested, restoresNear;
        long requestedAt;
        long requestSequence;

        Section(int x, int y, int z, Snapshot snapshot, long sequence) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.built = snapshot;
            this.sequence = sequence;
        }

        boolean changesAt(Snapshot next) {
            restoresNear = false;
            if (!mayChange(built, next, x, y, z)) return false;
            restoresNear = changes(distantLeaves, true, next);
            return restoresNear || changes(nearLeaves, false, next);
        }

        double distanceSquared(double cameraX, double cameraY, double cameraZ) {
            double dx = x * 16.0 + 8 - cameraX;
            double dy = y * 16.0 + 8 - cameraY;
            double dz = z * 16.0 + 8 - cameraZ;
            return dx * dx + dy * dy + dz * dz;
        }

        private boolean changes(BitSet leaves, boolean wasDistant, Snapshot next) {
            for (int index = leaves.nextSetBit(0); index >= 0; index = leaves.nextSetBit(index + 1)) {
                double bx = x * 16.0 + (index & 15) + 0.5;
                double by = y * 16.0 + (index >> 8) + 0.5;
                double bz = z * 16.0 + (index >> 4 & 15) + 0.5;
                if (next.isDistant(bx, by, bz) != wasDistant) return true;
            }
            return false;
        }
    }
}
