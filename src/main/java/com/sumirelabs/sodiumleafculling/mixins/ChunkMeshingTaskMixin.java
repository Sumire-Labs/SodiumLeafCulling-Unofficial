package com.sumirelabs.sodiumleafculling.mixins;

import com.sumirelabs.sodiumleafculling.LeafCulling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if fabric_legacy_renderer {
/*import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
*///?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
*///?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
*///?}

@Mixin(value = /*? if fabric_legacy_renderer {*//*ChunkRenderRebuildTask.class*//*?} else {*/ChunkBuilderMeshingTask.class/*?}*/, remap = false)
public class ChunkMeshingTaskMixin {
    @Inject(method = /*? if fabric_legacy_renderer {*//*"performBuild"*//*?} else {*/"execute"/*?}*/, at = @At("HEAD"))
    private void sodiumleafculling$beginBuild(CallbackInfoReturnable<?> cir) {
        LeafCulling.DISTANCE.beginBuild();
    }

    @Inject(method = /*? if fabric_legacy_renderer {*//*"performBuild"*//*?} else {*/"execute"/*?}*/, at = @At("RETURN"))
    private void sodiumleafculling$endBuild(CallbackInfoReturnable<?> cir) {
        LeafCulling.DISTANCE.endBuild(cir.getReturnValue() != null);
    }
}
