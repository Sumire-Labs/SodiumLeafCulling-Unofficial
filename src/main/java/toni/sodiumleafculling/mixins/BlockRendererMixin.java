package toni.sodiumleafculling.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import toni.sodiumleafculling.LeafCulling;
import toni.sodiumleafculling.PerformanceSettingsAccessor;

//? if sodium_modern_renderer {
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
//?} elif sodium_frapi_chunk_layer {
/*import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
*///?} elif fabric {
/*import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
*///?} elif embeddium {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.minecraft.client.renderer.RenderType;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
*///?}

//? if sodium_chunk_layer {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
*///?}

@Mixin(value = BlockRenderer.class, remap = false, priority = 100)
public abstract class BlockRendererMixin {

    //? if sodium_chunk_layer {
    /*@Redirect(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
        )
    )
    private ChunkSectionLayer sodiumleafculling$useSolidLayer(BlockState state) {
        var original = ItemBlockRenderTypes.getChunkRenderType(state);
        if (!(state.getBlock() instanceof LeavesBlock))
            return original;

        var ctx = (AbstractBlockRenderContextAccessor) this;
        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
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
            var ctx = (AbstractBlockRenderContextAccessor) this;
            var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
            if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos())) {
                return true;
            }
        }
        return original;
    }
    //?} elif sodium_frapi_chunk_layer {
    /*@ModifyVariable(method = "processQuad", at = @At("STORE"))
    private ChunkSectionLayer sodiumleafculling$useSolidLayer(ChunkSectionLayer layer, MutableQuadViewImpl quad) {
        var ctx = (AbstractBlockRenderContextAccessor) this;
        if (!(ctx.getState().getBlock() instanceof LeavesBlock))
            return layer;

        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.getSlice(), ctx.getPos()))
            return ChunkSectionLayer.SOLID;

        return layer;
    }
    *///?} elif sodium_caffeine {
    /*@ModifyVariable(method = "processQuad", at = @At("STORE"))
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
        var original = DefaultMaterials.forBlockState(state);
        if (!(state.getBlock() instanceof LeavesBlock))
            return original;

        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
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
        var original = DefaultMaterials.forRenderLayer(renderLayer);
        if (!(ctx.state().getBlock() instanceof LeavesBlock))
            return original;

        var quality = ((PerformanceSettingsAccessor) SodiumClientMod.options().performance).sodiumleafculling$getQuality();
        if (quality.isSolid() && LeafCulling.surroundedByLeaves(ctx.localSlice(), ctx.pos()))
            return DefaultMaterials.SOLID;

        return original;
    }
    *///?}
}
