package com.sumirelabs.sodiumleafculling.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.sumirelabs.sodiumleafculling.LeafCullingQuality;
import com.sumirelabs.sodiumleafculling.PerformanceSettingsAccessor;
import com.sumirelabs.sodiumleafculling.DistanceLeafCulling;
//? if embeddium_modern {
/*import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
*///?} elif sodium_modern_config {
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
//?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
*///?} else {
/*import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
*///?}

@Mixin(value = /*? if embeddium_modern {*//*EmbeddiumOptions.PerformanceSettings.class*//*?} elif sodium_modern_config {*/SodiumOptions.PerformanceSettings.class/*?} elif fabric_legacy_options {*//*SodiumGameOptions.AdvancedSettings.class*//*?} else {*//*SodiumGameOptions.PerformanceSettings.class*//*?}*/, remap = false, priority = 100)
public class PerformanceSettingsMixin implements PerformanceSettingsAccessor {
    @Unique
    public LeafCullingQuality leafCullingQuality = LeafCullingQuality.SOLID;

    @Unique
    public boolean distanceLeafCulling = false;

    @Unique
    public int leafCullingDistance = DistanceLeafCulling.DEFAULT_DISTANCE;

    public LeafCullingQuality sodiumleafculling$getQuality() {
        return leafCullingQuality;
    }

    public void sodiumleafculling$setQuality(LeafCullingQuality value) {
        leafCullingQuality = value;
    }

    public boolean sodiumleafculling$isDistanceCullingEnabled() {
        return distanceLeafCulling;
    }

    public void sodiumleafculling$setDistanceCullingEnabled(boolean value) {
        distanceLeafCulling = value;
    }

    public int sodiumleafculling$getDistance() {
        return DistanceLeafCulling.clampDistance(leafCullingDistance);
    }

    public void sodiumleafculling$setDistance(int value) {
        leafCullingDistance = DistanceLeafCulling.clampDistance(value);
    }
}
