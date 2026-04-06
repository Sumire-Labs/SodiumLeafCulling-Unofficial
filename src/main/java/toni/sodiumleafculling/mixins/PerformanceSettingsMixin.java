package toni.sodiumleafculling.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import toni.sodiumleafculling.LeafCullingQuality;
import toni.sodiumleafculling.PerformanceSettingsAccessor;
#if AFTER_21_11
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
#elif AFTER_21_1
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
#else
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
#endif

@Mixin(value = #if AFTER_21_11 SodiumOptions.PerformanceSettings.class #else SodiumGameOptions.PerformanceSettings.class #endif, remap = false, priority = 100)
public class PerformanceSettingsMixin implements PerformanceSettingsAccessor {
    @Unique
    public LeafCullingQuality leafCullingQuality = LeafCullingQuality.SOLID_AGGRESSIVE;

    public LeafCullingQuality sodiumleafculling$getQuality() {
        return leafCullingQuality;
    }

    public void sodiumleafculling$setQuality(LeafCullingQuality value) {
        leafCullingQuality = value;
    }
}
