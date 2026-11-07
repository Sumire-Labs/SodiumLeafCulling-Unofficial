package com.sumirelabs.sodiumleafculling.mixins;

import com.sumirelabs.sodiumleafculling.LeafCullingConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if embeddium_modern {
/*import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
*///?} elif sodium_modern_config {
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
//?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
*///?} else {
/*import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
*///?}

@Mixin(value = /*? if embeddium_modern {*//*EmbeddiumOptions.class*//*?} elif sodium_modern_config {*/SodiumOptions.class/*?} else {*//*SodiumGameOptions.class*//*?}*/, remap = false)
public class RendererOptionsLoadMixin {
    // Import the old keys before the renderer can rewrite its own config.
    @Inject(method = /*? if sodium_modern_config {*/"loadFromDisk"/*?} else {*//*"load"*//*?}*/, at = @At("HEAD"))
    private static void sodiumleafculling$migrateOptions(CallbackInfoReturnable<?> ci) {
        LeafCullingConfig.get();
    }
}
