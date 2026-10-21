package com.sumirelabs.sodiumleafculling.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.sumirelabs.sodiumleafculling.LeafCulling;
import com.sumirelabs.sodiumleafculling.PerformanceSettingsAccessor;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if embeddium_modern {
/*import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
*///?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
*///?}

@Mixin(value = /*? if embeddium_modern {*//*EmbeddiumWorldRenderer.class*//*?} else {*/SodiumWorldRenderer.class/*?}*/, remap = false)
public abstract class WorldRendererMixin {
    @Shadow private int renderDistance;

    @Shadow
    public abstract void scheduleRebuildForChunk(int x, int y, int z, boolean important);

    @Inject(method = {"setupTerrain", "updateChunks"}, at = @At("HEAD"))
    private void sodiumleafculling$updateDistance(CallbackInfo ci, @Local(argsOnly = true) Camera camera) {
        Vec3 pos = camera./*? if >=1.21.11 {*/position()/*?} else {*//*getPosition()*//*?}*/;
        PerformanceSettingsAccessor settings = LeafCulling.settings();
        LeafCulling.DISTANCE.update(settings.sodiumleafculling$isDistanceCullingEnabled(),
                settings.sodiumleafculling$getDistance(), pos.x, pos.y, pos.z, renderDistance,
                System.nanoTime(), (x, y, z) -> scheduleRebuildForChunk(x, y, z, false));
    }

    @Inject(method = {"setLevel", "setWorld", "reload"}, at = @At("HEAD"))
    private void sodiumleafculling$resetDistance(CallbackInfo ci) {
        LeafCulling.DISTANCE.reset();
    }
}
