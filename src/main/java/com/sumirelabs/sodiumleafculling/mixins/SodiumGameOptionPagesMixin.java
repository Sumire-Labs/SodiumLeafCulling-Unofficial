package com.sumirelabs.sodiumleafculling.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import com.sumirelabs.sodiumleafculling.LeafCulling;
import com.sumirelabs.sodiumleafculling.LeafCullingQuality;
import com.sumirelabs.sodiumleafculling.PerformanceSettingsAccessor;
import com.sumirelabs.sodiumleafculling.DistanceLeafCulling;
import java.util.List;

//? if sodium_modern_config {
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.resources.Identifier;
//?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.gui.EmbeddiumGameOptionPages;
import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.impl.gui.options.storage.EmbeddiumOptionsStorage;
*///?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.gui.options.*;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
*///?} else {
/*import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
*///?}

//? if sodium_modern_config {
@Mixin(value = SodiumConfigBuilder.class, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Shadow @Final private StorageEventHandler sodiumStorage;
    @Shadow @Final private SodiumOptions sodiumOpts;

    @Inject(method = "buildPerformancePage", at = @At("RETURN"))
    private void inject$leafcullingoption(CallbackInfoReturnable<OptionPageBuilder> cir, @Local(argsOnly = true) ConfigBuilder builder) {
        // Sodium 0.9.3 adds a VanillaOptions argument. Capture only the builder
        // so this callback also accepts the earlier one-argument method.
        cir.getReturnValue().addOptionGroup(
            builder.createOptionGroup()
                .addOption(
                    builder.createEnumOption(
                        Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.leaf_culling"),
                        LeafCullingQuality.class)
                        .setStorageHandler(this.sodiumStorage)
                        .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.name"))
                        .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.tooltip"))
                        .setDefaultValue(LeafCullingQuality.SOLID)
                        .setBinding(
                            value -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$setQuality(value),
                            () -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$getQuality())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                    builder.createBooleanOption(Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.distance_leaf_culling"))
                        .setStorageHandler(this.sodiumStorage)
                        .setName(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.name"))
                        .setTooltip(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.tooltip"))
                        .setDefaultValue(false)
                        .setBinding(
                            value -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$setDistanceCullingEnabled(value),
                            () -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$isDistanceCullingEnabled())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
                .addOption(
                    builder.createIntegerOption(Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.leaf_culling_distance"))
                        .setStorageHandler(this.sodiumStorage)
                        .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.name"))
                        .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.tooltip"))
                        .setDefaultValue(DistanceLeafCulling.DEFAULT_DISTANCE)
                        .setRange(DistanceLeafCulling.MIN_DISTANCE, DistanceLeafCulling.MAX_DISTANCE, 1)
                        .setBinding(
                            value -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$setDistance(value),
                            () -> ((PerformanceSettingsAccessor) this.sodiumOpts.performance).sodiumleafculling$getDistance())
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                )
        );
    }
}
//?} elif embeddium_modern {
/*@Mixin(value = EmbeddiumGameOptionPages.class, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final EmbeddiumOptionsStorage leafcullingOpts = new EmbeddiumOptionsStorage();

    @Inject(method = "performance", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void inject$leafcullingoption(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups) {
        groups.add(OptionGroup.createBuilder()
            .add(OptionImpl.createBuilder(LeafCullingQuality.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.tooltip"))
                .setControl(option -> new CyclingControl<>(option, LeafCullingQuality.class))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setQuality(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$getQuality())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(boolean.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setDistanceCullingEnabled(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$isDistanceCullingEnabled())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(int.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.tooltip"))
                .setControl(option -> new SliderControl(option, DistanceLeafCulling.MIN_DISTANCE,
                    DistanceLeafCulling.MAX_DISTANCE, 1, ControlValueFormatter.number()))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setDistance(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$getDistance())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            ).build()
        );
    }
}
*///?} elif fabric_legacy_options {
/*@Mixin(value = SodiumGameOptionPages.class, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final SodiumOptionsStorage leafcullingOpts = new SodiumOptionsStorage();

    @Inject(method = "advanced", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void inject$leafcullingoption(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups) {
        groups.add(OptionGroup.createBuilder()
            .add(OptionImpl.createBuilder(LeafCullingQuality.class, leafcullingOpts)
                .setName("sodiumleafculling.options.leaf_culling.name")
                .setTooltip("sodiumleafculling.options.leaf_culling.tooltip")
                .setControl(option -> new CyclingControl<>(option, LeafCullingQuality.class))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$setQuality(value),
                    opts -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$getQuality())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(boolean.class, leafcullingOpts)
                .setName("sodiumleafculling.options.distance_leaf_culling.name")
                .setTooltip("sodiumleafculling.options.distance_leaf_culling.tooltip")
                .setControl(TickBoxControl::new)
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$setDistanceCullingEnabled(value),
                    opts -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$isDistanceCullingEnabled())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(int.class, leafcullingOpts)
                .setName("sodiumleafculling.options.leaf_culling_distance.name")
                .setTooltip("sodiumleafculling.options.leaf_culling_distance.tooltip")
                .setControl(option -> new SliderControl(option, DistanceLeafCulling.MIN_DISTANCE,
                    DistanceLeafCulling.MAX_DISTANCE, 1, ControlValueFormatter.number()))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$setDistance(value),
                    opts -> ((PerformanceSettingsAccessor) opts.advanced).sodiumleafculling$getDistance())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            ).build()
        );
    }
}
*///?} else {
/*@Mixin(value = SodiumGameOptionPages.class, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final SodiumOptionsStorage leafcullingOpts = new SodiumOptionsStorage();

    @Inject(method = "performance", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void inject$leafcullingoption(CallbackInfoReturnable<OptionPage> cir, List<OptionGroup> groups) {
        groups.add(OptionGroup.createBuilder()
            .add(OptionImpl.createBuilder(LeafCullingQuality.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.tooltip"))
                .setControl(option -> new CyclingControl<>(option, LeafCullingQuality.class))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setQuality(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$getQuality())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(boolean.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setDistanceCullingEnabled(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$isDistanceCullingEnabled())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            )
            .add(OptionImpl.createBuilder(int.class, leafcullingOpts)
                .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.name"))
                .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.tooltip"))
                .setControl(option -> new SliderControl(option, DistanceLeafCulling.MIN_DISTANCE,
                    DistanceLeafCulling.MAX_DISTANCE, 1, ControlValueFormatter.number()))
                .setBinding(
                    (opts, value) -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$setDistance(value),
                    opts -> ((PerformanceSettingsAccessor) opts.performance).sodiumleafculling$getDistance())
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build()
            ).build()
        );
    }
}
*///?}
