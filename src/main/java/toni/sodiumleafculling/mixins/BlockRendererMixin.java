package toni.sodiumleafculling.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import toni.sodiumleafculling.LeafCulling;
import toni.sodiumleafculling.LeafCullingQuality;

//? if fabric_legacy_renderer {
/*import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.renderer.RenderType;
*///?} elif sodium_modern_renderer {
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
//?} elif sodium_frapi_chunk_layer {
/*import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
*///?} elif fabric {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
*///?} elif embeddium {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.minecraft.client.renderer.RenderType;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
*///?}

//? if legacy_model_buffers {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
*///?} elif fabric_legacy_renderer {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
*///?}

//? if embeddium_modern {
/*import org.embeddedt.embeddium.api.render.chunk.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockRenderer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.DefaultMaterials;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.RenderType;
*///?}

//? if sodium_chunk_layer {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?}

@Mixin(value = /*? if fabric_legacy_renderer {*//*ChunkRenderRebuildTask.class*//*?} else {*/BlockRenderer.class/*?}*/, remap = false, priority = 100)
public abstract class BlockRendererMixin {

    //? if fabric_legacy_renderer {
    /*@Redirect(
        method = "performBuild",
        at = @At(
            value = "INVOKE",
            target = /^? if legacy_model_buffers {^//^"Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;get(Lnet/minecraft/client/renderer/RenderType;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;"^//^?} else {^/"Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;get(Lnet/minecraft/client/renderer/RenderType;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;"/^?}^/,
            ordinal = 0
        )
    )
    private /^? if legacy_model_buffers {^//^ChunkModelBuffers^//^?} else {^/ChunkModelBuilder/^?}^/ sodiumleafculling$useSolidLayer(
        ChunkBuildBuffers buffers,
        RenderType layer,
        @Local WorldSlice slice,
        @Local BlockState state,
        @Local(ordinal = 0) BlockPos.MutableBlockPos pos
    ) {
        if (state.getBlock() instanceof LeavesBlock && LeafCulling.getQuality().isSolid() && LeafCulling.surroundedByLeaves(slice, pos))
            return buffers.get(RenderType.solid());

        return buffers.get(layer);
    }
    *///?} elif sodium_chunk_layer {
    /*@Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
        )
    )
    private ChunkSectionLayer sodiumleafculling$useSolidLayer(BlockState state) {
        ChunkSectionLayer original = ItemBlockRenderTypes.getChunkRenderType(state);
        if (!(state.getBlock() instanceof LeavesBlock))
            return original;

        AbstractBlockRenderContextAccessor ctx = (AbstractBlockRenderContextAccessor) this;
        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos()))
            return ChunkSectionLayer.SOLID;

        return original;
    }
    *///?} elif sodium_modern_renderer {
    // Sodium 0.8: BlockRenderer extends AbstractBlockRenderContext
    // Use accessor to reach parent class fields (slice, pos)
    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;forceOpaque(ZLnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean inject$forceOpaque(boolean cutoutLeaves, BlockState state) {
        boolean original = net.minecraft.client.renderer.block.ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
        if (!original && state.getBlock() instanceof LeavesBlock) {
            AbstractBlockRenderContextAccessor ctx = (AbstractBlockRenderContextAccessor) this;
            LeafCullingQuality quality = LeafCulling.getQuality();
            if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos())) {
                return true;
            }
        }
        return original;
    }
    //?} elif sodium_frapi_chunk_layer {
    /*@ModifyVariable(method = "processQuad", at = @At("STORE"))
    private ChunkSectionLayer sodiumleafculling$useSolidLayer(ChunkSectionLayer layer, MutableQuadViewImpl quad) {
        AbstractBlockRenderContextAccessor ctx = (AbstractBlockRenderContextAccessor) this;
        if (!(ctx.getState().getBlock() instanceof LeavesBlock))
            return layer;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos()))
            return ChunkSectionLayer.SOLID;

        return layer;
    }
    *///?} elif sodium_caffeine {
    /*@ModifyVariable(method = "processQuad", at = @At("STORE"))
    private BlendMode inject$processQuad(BlendMode blendMode, MutableQuadViewImpl quad) {
        AbstractBlockRenderContextAccessor ctx = (AbstractBlockRenderContextAccessor) this;
        if (!(ctx.getState().getBlock() instanceof LeavesBlock))
            return blendMode;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos()))
        {
            return BlendMode.SOLID;
        }

        return blendMode;
    }
    *///?} elif fabric {
    /*@Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forBlockState(Lnet/minecraft/world/level/block/state/BlockState;)Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;"
        )
    )
    private Material sodiumleafculling$useSolidMaterial(
        BlockState state,
        @Local(argsOnly = true) BlockRenderContext ctx
    ) {
        Material original = DefaultMaterials.forBlockState(state);
        if (!(state.getBlock() instanceof LeavesBlock))
            return original;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.world(), ctx.pos()))
            return DefaultMaterials.SOLID;

        return original;
    }
    *///?} elif embeddium {
    /*@Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forRenderLayer(Lnet/minecraft/client/renderer/RenderType;)Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;"
        )
    )
    private Material sodiumleafculling$useSolidMaterial(
        RenderType renderLayer,
        @Local(argsOnly = true) BlockRenderContext ctx
    ) {
        Material original = DefaultMaterials.forRenderLayer(renderLayer);
        if (!(ctx.state().getBlock() instanceof LeavesBlock))
            return original;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.localSlice(), ctx.pos()))
            return DefaultMaterials.SOLID;

        return original;
    }
    *///?} elif embeddium_modern {
    /*@Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/DefaultMaterials;forRenderLayer(Lnet/minecraft/client/renderer/RenderType;)Lorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;"
        )
    )
    private Material sodiumleafculling$useSolidMaterial(
        RenderType renderLayer,
        @Local(argsOnly = true) BlockRenderContext ctx
    ) {
        Material original = DefaultMaterials.forRenderLayer(renderLayer);
        if (!(ctx.state().getBlock() instanceof LeavesBlock))
            return original;

        LeafCullingQuality quality = LeafCulling.getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.localSlice(), ctx.pos()))
            return DefaultMaterials.SOLID;

        return original;
    }
    *///?}
}
