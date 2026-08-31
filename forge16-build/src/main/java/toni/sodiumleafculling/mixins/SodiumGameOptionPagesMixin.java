package toni.sodiumleafculling.mixins;

import java.util.List;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import toni.sodiumleafculling.LeafCullingQuality;
import toni.sodiumleafculling.PerformanceSettingsAccessor;

@Mixin(value = SodiumGameOptionPages.class, remap = false)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final SodiumOptionsStorage leafcullingOpts = new SodiumOptionsStorage();

    @Inject(method = "performance", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void inject$leafcullingoption(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups) {
        groups.add(OptionGroup.createBuilder()
            .add(OptionImpl.createBuilder(LeafCullingQuality.class, leafcullingOpts)
                .setName("sodiumleafculling.options.leaf_culling.name")
                .setTooltip("sodiumleafculling.options.leaf_culling.tooltip")
                .setControl(option -> new CyclingControl<>(option, LeafCullingQuality.class))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setQuality(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$getQuality())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build())
            .build());
    }
}
