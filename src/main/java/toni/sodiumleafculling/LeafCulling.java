package toni.sodiumleafculling;

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
//? if embeddium_modern {
/*import org.embeddedt.embeddium.impl.Embeddium;
*///?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
//?} else {
/*import me.jellysquid.mods.sodium.client.SodiumClientMod;
*///?}

public class LeafCulling {
    private static final Direction[] VALUES = Direction.values();
    public static Component translatable(String key) {
        //? if legacy_component {
        /*return new TranslatableComponent(key);
        *///?} else {
        return Component.translatable(key);
        //?}
    }

    public static LeafCullingQuality getQuality() {
        //? if embeddium_modern {
        /*return ((PerformanceSettingsAccessor) Embeddium.options().performance).sodiumleafculling$getQuality();
        *///?} elif fabric_legacy_options {
        /*return ((PerformanceSettingsAccessor) SodiumClientMod.options().advanced).sodiumleafculling$getQuality();
        *///?} else {
        return ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
        //?}
    }

    public static boolean isFacingAir(BlockGetter view, BlockPos pos, Direction facing) {
        return view.getBlockState(pos.relative(facing)).getBlock() instanceof AirBlock;
    }

    public static boolean surroundedByLeaves(BlockGetter view, BlockPos pos) {
        boolean isAggressiveMode = getQuality() == LeafCullingQuality.SOLID_AGGRESSIVE;
        for (Direction dir : VALUES) {
            if (isAggressiveMode && (dir == Direction.DOWN || dir == Direction.UP))
                continue;

            BlockPos dirPos = pos.relative(dir);
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
        if (isFacingAir(view, pos, facing))
            return false;

        boolean cull = true;
        BlockPos.MutableBlockPos checkPos = pos.mutable();
        for (int i = 1; i <= depth; i++) {
            checkPos.move(facing);
            BlockState state = view.getBlockState(checkPos);
            cull &= state != null && state.getBlock() instanceof LeavesBlock;
        }

        return cull;
    }
}
