package com.sumirelabs.sodiumleafculling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Distance decisions are fixed for a mesh build; only the render thread schedules rebuilds. */
public final class DistanceLeafCulling {
    public static final int DEFAULT_DISTANCE = 64;
    public static final int MIN_DISTANCE = 1;
    public static final int MAX_DISTANCE = 256;
    private static final int REBUILDS_PER_FRAME = 8;
    private static final long RETRY_NANOS = 1_000_000_000L;

    public interface RebuildScheduler {
        void rebuild(int x, int y, int z);
    }

    private volatile Snapshot snapshot = new Snapshot(new Object(), false, 64, 0, 0, 0);
    private final ThreadLocal<Build> build = new ThreadLocal<>();
    private final ConcurrentLinkedQueue<Section> completed = new ConcurrentLinkedQueue<>();
    private final Map<Long, Section> sections = new LinkedHashMap<>();
    private final ArrayDeque<Section> pending = new ArrayDeque<>();

    public static int clampDistance(int distance) {
        return Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    /** Called when the renderer reloads or changes worlds. Old worker results are ignored. */
    public void reset() {
        Snapshot previous = snapshot;
        snapshot = new Snapshot(new Object(), previous.enabled, previous.distance, previous.x, previous.y, previous.z);
        sections.clear();
        pending.clear();
        completed.clear();
    }

    public void beginBuild() {
        Snapshot current = snapshot;
        if (current.enabled) {
            build.set(new Build(current));
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
        current.record(x >> 4, y >> 4, z >> 4);
        return current.snapshot.isDistant(x + 0.5, y + 0.5, z + 0.5);
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
                sections.put(key(result.x, result.y, result.z), result);
            }
        }
        if (!enabled) {
            sections.clear();
            pending.clear();
            return;
        }

        int cameraX = ((int) Math.floor(x)) >> 4;
        int cameraZ = ((int) Math.floor(z)) >> 4;
        Iterator<Section> iterator = sections.values().iterator();
        while (iterator.hasNext()) {
            Section section = iterator.next();
            // Sections outside the loaded horizontal range will be built again if loaded later.
            if (Math.abs(section.x - cameraX) > renderDistance + 2 ||
                    Math.abs(section.z - cameraZ) > renderDistance + 2) {
                iterator.remove();
                continue;
            }
            if (section.checked != current) {
                section.dirty = mayChange(section.built, current, section.x, section.y, section.z);
                section.checked = current;
                if (!section.dirty) {
                    section.built = current;
                }
            }
            if (section.dirty && !section.queued &&
                    (!section.requested || now - section.requestedAt >= RETRY_NANOS)) {
                section.queued = true;
                pending.add(section);
            }
        }

        int scheduled = 0;
        while (!pending.isEmpty() && scheduled < REBUILDS_PER_FRAME) {
            Section section = pending.remove();
            section.queued = false;
            if (!section.dirty || sections.get(key(section.x, section.y, section.z)) != section) {
                continue;
            }
            scheduler.rebuild(section.x, section.y, section.z);
            section.requested = true;
            section.requestedAt = now;
            scheduled++;
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
        List<Section> sections;
        Section last;

        Build(Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        void record(int x, int y, int z) {
            if (last != null && last.x == x && last.y == y && last.z == z) {
                return;
            }
            if (sections == null) {
                sections = new ArrayList<>(1);
            }
            for (Section section : sections) {
                if (section.x == x && section.y == y && section.z == z) {
                    last = section;
                    return;
                }
            }
            last = new Section(x, y, z, snapshot);
            sections.add(last);
        }
    }

    private static final class Section {
        final int x, y, z;
        Snapshot built, checked;
        boolean dirty, queued, requested;
        long requestedAt;

        Section(int x, int y, int z, Snapshot snapshot) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.built = snapshot;
        }
    }
}
