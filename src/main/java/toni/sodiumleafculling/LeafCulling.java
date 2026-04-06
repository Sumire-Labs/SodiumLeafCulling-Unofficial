package toni.sodiumleafculling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.LeavesBlock;
#if AFTER_21_1
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
#else
import me.jellysquid.mods.sodium.client.SodiumClientMod;
#endif

public class LeafCulling {
    private static final Direction[] VALUES = Direction.values();

    public static boolean isFacingAir(BlockGetter view, BlockPos pos, Direction facing) {
        return view.getBlockState(pos.relative(facing)).getBlock() instanceof AirBlock;
    }

    public static boolean surroundedByLeaves(BlockGetter view, BlockPos pos) {
        var isAggressiveMode = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality() == LeafCullingQuality.SOLID_AGGRESSIVE;
        for (Direction dir : VALUES) {
            if (isAggressiveMode && (dir == Direction.DOWN || dir == Direction.UP))
                continue;

            var dirPos = pos.relative(dir);
            var blockstate = view.getBlockState(dirPos);
            if (blockstate.getBlock() instanceof LeavesBlock)
                continue;

            if (blockstate.isSolidRender(#if AFTER_26_1_1 #else view, pos #endif))
                continue;

            return false;
        }

        return true;
    }

    public static boolean shouldCullSide(BlockGetter view, BlockPos pos, Direction facing, int depth) {
        if (isFacingAir(view, pos, facing))
            return false;

        var cull = true;
        var checkPos = pos.mutable();
        for (int i = 1; i <= depth; i++) {
            checkPos.move(facing);
            var state = view.getBlockState(checkPos);
            cull &= state != null && state.getBlock() instanceof LeavesBlock;
        }

        return cull;
    }
}
