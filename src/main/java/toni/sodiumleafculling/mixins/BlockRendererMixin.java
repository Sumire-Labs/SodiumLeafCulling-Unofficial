package toni.sodiumleafculling.mixins;


import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import toni.sodiumleafculling.LeafCulling;
import toni.sodiumleafculling.PerformanceSettingsAccessor;
import java.util.List;

#if AFTER_21_11
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
#elif AFTER_21_1
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
#else
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;

import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
#endif

@Mixin(value = BlockRenderer.class, remap = false, priority = 100)
public abstract class BlockRendererMixin {

    #if AFTER_21_11
    // Sodium 0.8: BlockRenderer extends AbstractBlockRenderContext
    // Use accessor to reach parent class fields (slice, pos)
    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;forceOpaque(ZLnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean inject$forceOpaque(boolean cutoutLeaves, BlockState state) {
        boolean original = net.minecraft.client.renderer.block.ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
        if (!original && state.getBlock() instanceof LeavesBlock) {
            var ctx = (AbstractBlockRenderContextAccessor) this;
            var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
            if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos())) {
                return true;
            }
        }
        return original;
    }
    #elif AFTER_21_1
    @ModifyVariable(method = "processQuad", at = @At("STORE"))
    private BlendMode inject$processQuad(BlendMode blendMode, MutableQuadViewImpl quad) {
        var ctx = (AbstractBlockRenderContextAccessor) this;
        if (!(ctx.getState().getBlock() instanceof LeavesBlock))
            return blendMode;

        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos()))
        {
            return BlendMode.SOLID;
        }

        return blendMode;
    }
    #else

    @Shadow protected abstract List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face);

    @Shadow protected abstract boolean isFaceVisible(BlockRenderContext ctx, Direction face);

    @Shadow protected abstract void renderQuadList(BlockRenderContext ctx, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset, ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace);

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;getGeometry(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lnet/minecraft/core/Direction;)Ljava/util/List;"))
    private List<BakedQuad> redirect(
            BlockRenderer instance,
            BlockRenderContext ctx,
            Direction face,
            @Local ChunkBuildBuffers buffers,
            @Local LightPipeline lighter,
            @Local ColorProvider<BlockState> colorizer,
            @Local Vec3 renderOffset
    ) {
        if (!(ctx.state().getBlock() instanceof LeavesBlock))
            return getGeometry(ctx, face);

        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(#if FABRIC ctx.world() #else ctx.localSlice() #endif, ctx.pos()))
        {
            var renderLayer = ctx.renderLayer();
            ctx.update(ctx.pos(), new BlockPos((int) ctx.origin().x(), (int) ctx.origin().y(), (int)ctx.origin().z()), ctx.state(), ctx.model(), ctx.seed(), ctx.modelData(), RenderType.solid());

            List<BakedQuad> quads = this.getGeometry(ctx, face);
            var leafmaterial = DefaultMaterials.forRenderLayer(ctx.renderLayer());
            var leafmeshBuilder = buffers.get(leafmaterial);

            if (!quads.isEmpty() && isFaceVisible(ctx, face)) {
                renderQuadList(ctx, leafmaterial, lighter, colorizer, renderOffset, leafmeshBuilder, quads, face);
            }

            ctx.update(ctx.pos(), new BlockPos((int) ctx.origin().x(), (int) ctx.origin().y(), (int)ctx.origin().z()), ctx.state(), ctx.model(), ctx.seed(), ctx.modelData(), renderLayer);
        }

        return getGeometry(ctx, face);
    }
    #endif
}
