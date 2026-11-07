package com.sumirelabs.sodiumleafculling;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** Memoizes Solid decisions only while one worker owns a terrain snapshot. */
final class LeafOcclusionCache {
    private final byte[] results = new byte[16 * 16 * 16];
    final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
    private BlockGetter view;
    private int sectionX, sectionY, sectionZ;
    boolean active;

    void begin() {
        view = null;
        active = true;
    }

    void end() {
        view = null;
        active = false;
    }

    boolean knownSolidLeaf(BlockGetter source, int x, int y, int z) {
        if (view != source || sectionX != (x >> 4) || sectionY != (y >> 4) || sectionZ != (z >> 4)) return false;
        return (results[index(x, y, z)] & 2) != 0;
    }

    private static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    boolean surrounded(BlockGetter source, BlockPos pos, boolean aggressive) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (view == null) {
            // The first face lookup can point across a section boundary.
            // An interior leaf queried by the mesher is in its own section.
            if (!LeafCulling.sectionInterior(x) || !LeafCulling.sectionInterior(y) || !LeafCulling.sectionInterior(z)) {
                return LeafCulling.checkSurrounded(source, pos, aggressive, scratch);
            }
            Arrays.fill(results, (byte) 0);
            view = source;
            sectionX = x >> 4;
            sectionY = y >> 4;
            sectionZ = z >> 4;
        }
        // Neighbors just outside this section still need their normal checks.
        // Never reuse an entry for another view or for aliased coordinates.
        if (view != source || sectionX != (x >> 4) || sectionY != (y >> 4) || sectionZ != (z >> 4)) {
            return LeafCulling.checkSurrounded(source, pos, aggressive, scratch);
        }
        int index = index(x, y, z);
        int known = aggressive ? 4 : 1;
        int enclosed = known << 1;
        int entry = results[index];
        if ((entry & known) != 0) return (entry & enclosed) != 0;
        boolean result = LeafCulling.checkSurrounded(source, pos, aggressive, scratch);
        results[index] = (byte) (entry | known | (result ? enclosed : 0));
        return result;
    }
}
