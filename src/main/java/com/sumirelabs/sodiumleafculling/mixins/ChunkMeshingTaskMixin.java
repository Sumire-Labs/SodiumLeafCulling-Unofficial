package com.sumirelabs.sodiumleafculling.mixins;

import com.sumirelabs.sodiumleafculling.LeafCulling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if fabric_legacy_renderer {
/*import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
*///?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
*///?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
*///?}

//? if legacy_model_buffers {
/*import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
*///?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.render.chunk.RenderSection;
*///?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
*///?}

@Mixin(value = /*? if fabric_legacy_renderer {*//*ChunkRenderRebuildTask.class*//*?} else {*/ChunkBuilderMeshingTask.class/*?}*/, remap = false)
public class ChunkMeshingTaskMixin {
    @Unique private int sodiumleafculling$sectionX;
    @Unique private int sodiumleafculling$sectionY;
    @Unique private int sodiumleafculling$sectionZ;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sodiumleafculling$captureSection(CallbackInfo ci,
            @Local(argsOnly = true) /*? if legacy_model_buffers {*//*ChunkRenderContainer<?>*//*?} else {*/RenderSection/*?}*/ section) {
        sodiumleafculling$sectionX = section.getChunkX();
        sodiumleafculling$sectionY = section.getChunkY();
        sodiumleafculling$sectionZ = section.getChunkZ();
    }

    @Inject(method = /*? if fabric_legacy_renderer {*//*"performBuild"*//*?} else {*/"execute"/*?}*/, at = @At("HEAD"))
    private void sodiumleafculling$beginBuild(CallbackInfoReturnable<?> cir) {
        LeafCulling.beginOcclusionBuild();
        LeafCulling.DISTANCE.beginBuild(sodiumleafculling$sectionX, sodiumleafculling$sectionY, sodiumleafculling$sectionZ);
    }

    @Inject(method = /*? if fabric_legacy_renderer {*//*"performBuild"*//*?} else {*/"execute"/*?}*/, at = @At("RETURN"))
    private void sodiumleafculling$endBuild(CallbackInfoReturnable<?> cir) {
        LeafCulling.endOcclusionBuild();
        LeafCulling.DISTANCE.endBuild(cir.getReturnValue() != null);
    }
}
