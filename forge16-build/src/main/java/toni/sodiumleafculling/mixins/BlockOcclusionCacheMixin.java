package toni.sodiumleafculling.mixins;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import toni.sodiumleafculling.LeafCulling;
import toni.sodiumleafculling.LeafCullingQuality;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public class BlockOcclusionCacheMixin {
    @Inject(method = "shouldDrawSide", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;skipRendering(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/Direction;)Z", remap = true), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void inject$shouldDrawSide(BlockState selfState, IBlockReader view, BlockPos selfPos, Direction facing, CallbackInfoReturnable<Boolean> cir, BlockPos.Mutable otherPos, BlockState otherState) {
        if (!(selfState.getBlock() instanceof LeavesBlock))
            return;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality == LeafCullingQuality.HOLLOW && LeafCulling.shouldCullSide(view, selfPos, facing, 2)) {
            cir.setReturnValue(false);
            return;
        }

        if (otherState.getBlock() instanceof LeavesBlock && quality.isSolid()) {
            boolean cullSelf = LeafCulling.surroundedByLeaves(view, selfPos);
            boolean cullOther = LeafCulling.surroundedByLeaves(view, otherPos);
            if (cullOther) {
                cir.setReturnValue(false);
            }
        }
    }
}
