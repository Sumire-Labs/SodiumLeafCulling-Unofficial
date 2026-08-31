package toni.sodiumleafculling;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.AirBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public final class LeafCulling {
    private static final Direction[] VALUES = Direction.values();

    private LeafCulling() {
    }

    public static LeafCullingQuality getQuality() {
        return ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
    }

    public static boolean isFacingAir(IBlockReader view, BlockPos pos, Direction facing) {
        return view.getBlockState(pos.relative(facing)).getBlock() instanceof AirBlock;
    }

    public static boolean surroundedByLeaves(IBlockReader view, BlockPos pos) {
        boolean aggressive = getQuality() == LeafCullingQuality.SOLID_AGGRESSIVE;
        for (Direction direction : VALUES) {
            if (aggressive && (direction == Direction.DOWN || direction == Direction.UP))
                continue;

            BlockPos otherPos = pos.relative(direction);
            BlockState state = view.getBlockState(otherPos);
            if (state.getBlock() instanceof LeavesBlock || state.isSolidRender(view, otherPos))
                continue;

            return false;
        }
        return true;
    }

    public static boolean shouldCullSide(IBlockReader view, BlockPos pos, Direction facing, int depth) {
        if (isFacingAir(view, pos, facing))
            return false;

        boolean cull = true;
        BlockPos.Mutable checkPos = pos.mutable();
        for (int i = 1; i <= depth; i++) {
            checkPos.move(facing);
            BlockState state = view.getBlockState(checkPos);
            cull &= state.getBlock() instanceof LeavesBlock;
        }
        return cull;
    }
}
