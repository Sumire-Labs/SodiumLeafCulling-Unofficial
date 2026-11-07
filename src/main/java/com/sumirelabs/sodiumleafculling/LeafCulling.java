package com.sumirelabs.sodiumleafculling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
//? if legacy_component {
/*import net.minecraft.network.chat.TranslatableComponent;
*///?}
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LeafCulling {
    private static final Direction[] VALUES = Direction.values();
    public static final DistanceLeafCulling DISTANCE = new DistanceLeafCulling();
    private static final ThreadLocal<LeafOcclusionCache> OCCLUSION = ThreadLocal.withInitial(LeafOcclusionCache::new);

    private LeafCulling() { }

    public static void beginOcclusionBuild() {
        OCCLUSION.get().begin();
    }

    public static void endOcclusionBuild() {
        OCCLUSION.get().end();
    }

    public static Component translatable(String key) {
        //? if legacy_component {
        /*return new TranslatableComponent(key);
        *///?} else {
        return Component.translatable(key);
        //?}
    }

    public static LeafCullingQuality getQuality() {
        return settings().sodiumleafculling$getQuality();
    }

    public static LeafCullingConfig settings() {
        return LeafCullingConfig.get();
    }

    public static boolean shouldCullAdjacentBlockFace(BlockGetter view, BlockPos pos, BlockState state,
                                                       int leafX, int leafY, int leafZ) {
        // This optional optimization only uses work the leaf renderer already did.
        // A missing entry leaves the original face intact, rather than scanning again.
        return sectionInterior(leafX) && sectionInterior(leafY) && sectionInterior(leafZ) &&
                OCCLUSION.get().knownSolidLeaf(view, leafX, leafY, leafZ) &&
                state.isSolidRender(/*? if <1.21.2 {*//*view, pos*//*?}*/);
    }

    static boolean sectionInterior(int coordinate) {
        int local = coordinate & 15;
        return local != 0 && local != 15;
    }

    public static boolean canCullAdjacentBlocks() {
        LeafCullingConfig config = settings();
        return config.cullAdjacentBlocks() && config.sodiumleafculling$getQuality() == LeafCullingQuality.SOLID;
    }

    public static boolean isDistant(BlockPos pos) {
        return DISTANCE.isDistant(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean shouldCullDistantSide(BlockGetter view, BlockPos pos, BlockPos otherPos, BlockState otherState) {
        if (!(otherState.getBlock() instanceof LeavesBlock)) return false;
        if (DISTANCE.canCullAdjacent(pos.getX(), pos.getY(), pos.getZ(), otherPos.getX(), otherPos.getY(), otherPos.getZ()))
            return true;
        // A neighbor made opaque by the ordinary mode is safe regardless of its distance mesh.
        LeafCullingQuality quality = getQuality();
        return quality.isSolid() && surroundedByLeaves(view, otherPos, quality);
    }

    public static boolean isFacingAir(BlockGetter view, BlockPos pos, Direction facing) {
        return view.getBlockState(pos.relative(facing)).getBlock() instanceof AirBlock;
    }

    /** The caller must have verified that pos contains a leaf block in this view. */
    public static boolean surroundedByLeaves(BlockGetter view, BlockPos pos, LeafCullingQuality quality) {
        boolean isAggressiveMode = quality == LeafCullingQuality.SOLID_AGGRESSIVE;
        LeafOcclusionCache cache = OCCLUSION.get();
        return cache.active ? cache.surrounded(view, pos, isAggressiveMode) :
                checkSurrounded(view, pos, isAggressiveMode, pos.mutable());
    }

    static boolean checkSurrounded(BlockGetter view, BlockPos pos, boolean isAggressiveMode,
                                   BlockPos.MutableBlockPos dirPos) {
        for (Direction dir : VALUES) {
            if (isAggressiveMode && (dir == Direction.DOWN || dir == Direction.UP))
                continue;

            dirPos.set(pos).move(dir);
            BlockState blockstate = view.getBlockState(dirPos);
            if (blockstate.getBlock() instanceof LeavesBlock)
                continue;

            if (blockstate.isSolidRender(/*? if <1.21.2 {*//*view, dirPos*//*?}*/))
                continue;

            return false;
        }

        return true;
    }

    public static boolean shouldCullSide(BlockGetter view, BlockPos pos, Direction facing, int depth) {
        if (depth <= 0)
            return !isFacingAir(view, pos, facing);

        BlockPos.MutableBlockPos checkPos = pos.mutable();
        for (int i = 1; i <= depth; i++) {
            checkPos.move(facing);
            BlockState state = view.getBlockState(checkPos);
            if (state == null || !(state.getBlock() instanceof LeavesBlock))
                return false;
        }

        return true;
    }
}
