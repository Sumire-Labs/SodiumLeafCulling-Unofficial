package toni.sodiumleafculling.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import toni.sodiumleafculling.LeafCulling;
import toni.sodiumleafculling.LeafCullingQuality;

//? if sodium_modern_renderer {
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
//?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
*///?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockOcclusionCache;
*///?} elif fabric_legacy_renderer {
/*import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
*///?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
*///?}

//? if >=26.1 {
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} elif sodium_modern_renderer {
/*import net.minecraft.world.level.BlockAndTintGetter;
*///?}

//? if sodium_modern_renderer {
@Mixin(value = AbstractBlockRenderContext.class, remap = false, priority = 100)
public class BlockOcclusionCacheMixin {

    @Shadow protected BlockState state;
    @Shadow protected BlockPos pos;
    @Shadow protected BlockAndTintGetter level;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void inject$shouldDrawSide(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (state != null && state.getBlock() instanceof LeavesBlock) {
            LeafCullingQuality quality = LeafCulling.getQuality();
            if (quality == LeafCullingQuality.HOLLOW) {
                boolean skipRendering = LeafCulling.shouldCullSide(level, pos, facing, 2);
                if (skipRendering) {
                    cir.setReturnValue(false);
                    return;
                }
            }

            if (quality.isSolid()) {
                BlockPos otherPos = pos.relative(facing);
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.getBlock() instanceof LeavesBlock) {
                    boolean cullSelf = LeafCulling.surroundedByLeaves(level, pos);
                    boolean cullOther = LeafCulling.surroundedByLeaves(level, otherPos);

                    if ((!cullSelf && cullOther) || (cullSelf && cullOther)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }
}
//?} else {
/*@Mixin(value = BlockOcclusionCache.class, priority = 100)
public class BlockOcclusionCacheMixin {

    @Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;skipRendering(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void inject$shouldDrawSide(BlockState selfState, BlockGetter view, BlockPos selfPos, Direction facing, CallbackInfoReturnable<Boolean> cir, BlockPos.MutableBlockPos otherPos, BlockState otherState) {
         if (selfState.getBlock() instanceof LeavesBlock) {
             LeafCullingQuality quality = LeafCulling.getQuality();
             if (quality == LeafCullingQuality.HOLLOW) {
                 boolean skipRendering = LeafCulling.shouldCullSide(view, selfPos, facing, 2);
                 if (skipRendering) {
                     cir.setReturnValue(false);
                     return;
                 }
             }

             if (otherState.getBlock() instanceof LeavesBlock && quality.isSolid()) {
                 boolean cullSelf = LeafCulling.surroundedByLeaves(view, selfPos);
                 boolean cullOther = LeafCulling.surroundedByLeaves(view, otherPos);

                 if (!cullSelf && cullOther) {
                     cir.setReturnValue(false);
                     return;
                 }

                 if (cullSelf && cullOther) {
                     cir.setReturnValue(false);
                     return;
                 }
             }
         }
    }
}
*///?}
