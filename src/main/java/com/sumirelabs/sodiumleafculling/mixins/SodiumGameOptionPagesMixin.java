package com.sumirelabs.sodiumleafculling.mixins;

import com.sumirelabs.sodiumleafculling.DistanceLeafCulling;
import com.sumirelabs.sodiumleafculling.LeafCulling;
import com.sumirelabs.sodiumleafculling.LeafCullingConfig;
import com.sumirelabs.sodiumleafculling.LeafCullingQuality;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

//? if sodium_modern_config {
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} elif embeddium_modern {
/*import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.api.options.control.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.caffeinemc.mods.sodium.client.gui.options.*;
import net.caffeinemc.mods.sodium.client.gui.options.control.*;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
*///?} else {
/*import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.*;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
*///?}
//? if !sodium_modern_config && !embeddium_modern {
/*import com.sumirelabs.sodiumleafculling.EnabledOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

//? if sodium_modern_config {
@Mixin(value = SodiumConfigBuilder.class, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final StorageEventHandler leafcullingStorage = () -> LeafCullingConfig.get().save();

    @Inject(method = "registerConfigLate", at = @At("RETURN"))
    private void inject$leafcullingoption(ConfigBuilder builder, CallbackInfo ci) {
        builder.registerModOptions("seleafculling").setName("Leaf Culling").addPage(sodiumleafculling$createPage(builder));
    }

    @Unique
    private static OptionPageBuilder sodiumleafculling$createPage(ConfigBuilder builder) {
        LeafCullingConfig config = LeafCullingConfig.get();
        Identifier distanceToggle = Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.distance_leaf_culling");
        return builder.createOptionPage()
            .setName(LeafCulling.translatable("sodiumleafculling.options.page"))
            .addOptionGroup(builder.createOptionGroup()
                .addOption(builder.createEnumOption(
                        Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.leaf_culling"), LeafCullingQuality.class)
                    .setStorageHandler(leafcullingStorage)
                    .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.name"))
                    .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling.tooltip"))
                    .setDefaultValue(LeafCullingQuality.SOLID)
                    .setBinding(config::sodiumleafculling$setQuality, config::sodiumleafculling$getQuality)
                    .setImpact(OptionImpact.MEDIUM)
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
                .addOption(builder.createBooleanOption(distanceToggle)
                    .setStorageHandler(leafcullingStorage)
                    .setName(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.name"))
                    .setTooltip(LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.tooltip"))
                    .setDefaultValue(false)
                    .setBinding(config::sodiumleafculling$setDistanceCullingEnabled, config::sodiumleafculling$isDistanceCullingEnabled)
                    .setImpact(OptionImpact.MEDIUM)
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
                .addOption(builder.createIntegerOption(
                        Identifier.fromNamespaceAndPath("sodiumleafculling", "performance.leaf_culling_distance"))
                    .setStorageHandler(leafcullingStorage)
                    .setName(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.name"))
                    .setTooltip(LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.tooltip"))
                    .setDefaultValue(DistanceLeafCulling.DEFAULT_DISTANCE)
                    .setRange(DistanceLeafCulling.MIN_DISTANCE, DistanceLeafCulling.MAX_DISTANCE, 1)
                    .setValueFormatter(value -> net.minecraft.network.chat.Component.literal(Integer.toString(value)))
                    .setEnabledProvider(state -> state.readBooleanOption(distanceToggle), distanceToggle)
                    .setBinding(config::sodiumleafculling$setDistance, config::sodiumleafculling$getDistance)
                    .setImpact(OptionImpact.MEDIUM)
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
                .addOption(builder.createBooleanOption(
                        Identifier.fromNamespaceAndPath("sodiumleafculling", "cull_adjacent_blocks"))
                    .setStorageHandler(leafcullingStorage)
                    .setName(LeafCulling.translatable("sodiumleafculling.options.cull_adjacent_blocks.name"))
                    .setTooltip(LeafCulling.translatable("sodiumleafculling.options.cull_adjacent_blocks.tooltip"))
                    .setDefaultValue(false)
                    .setBinding(config::setCullAdjacentBlocks, config::cullAdjacentBlocks)
                    .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)));
    }
}
//?} else {
/*@Mixin(value = /^? if embeddium_modern {^//^EmbeddiumVideoOptionsScreen.class^//^?} else {^/SodiumOptionsGUI.class/^?}^/, remap = false, priority = 100)
public class SodiumGameOptionPagesMixin {
    @Unique
    private static final OptionStorage<LeafCullingConfig> leafcullingStorage = new OptionStorage<LeafCullingConfig>() {
        @Override public LeafCullingConfig getData() { return LeafCullingConfig.get(); }
        @Override public void save() { getData().save(); }
    };

    //? if embeddium_modern {
    /^@Inject(method = "makePages", at = @At("RETURN"), cancellable = true)
    private static void inject$leafcullingoption(CallbackInfoReturnable<List<OptionPage>> cir) {
        List<OptionPage> withLeafCulling = new ArrayList<>(cir.getReturnValue());
        withLeafCulling.add(sodiumleafculling$createPage());
        cir.setReturnValue(withLeafCulling);
    }
    ^///?} else {
    @Shadow @Final @Mutable private List<OptionPage> pages;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void inject$leafcullingoption(CallbackInfo ci) {
        pages = new ArrayList<>(pages);
        pages.add(sodiumleafculling$createPage());
    }
    //?}

    @Unique
    private static OptionPage sodiumleafculling$createPage() {
        Option<Boolean> distanceToggle = OptionImpl.createBuilder(boolean.class, leafcullingStorage)
            //? if embeddium_modern {
            /^.setId(ResourceLocation.fromNamespaceAndPath("seleafculling", "distance_leaf_culling"))
            ^///?}
            .setName(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.distance_leaf_culling.name"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.name")/^?}^/)
            .setTooltip(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.distance_leaf_culling.tooltip"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.distance_leaf_culling.tooltip")/^?}^/)
            .setControl(TickBoxControl::new)
            .setBinding(LeafCullingConfig::sodiumleafculling$setDistanceCullingEnabled, LeafCullingConfig::sodiumleafculling$isDistanceCullingEnabled)
            .setImpact(OptionImpact.MEDIUM)
            .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
            .build();

        OptionGroup group = OptionGroup.createBuilder()
            //? if embeddium_modern {
            /^.setId(ResourceLocation.fromNamespaceAndPath("seleafculling", "leaf_culling"))
            ^///?}
            .add(OptionImpl.createBuilder(LeafCullingQuality.class, leafcullingStorage)
                //? if embeddium_modern {
                /^.setId(ResourceLocation.fromNamespaceAndPath("seleafculling", "leaf_culling_quality"))
                ^///?}
                .setName(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.leaf_culling.name"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.leaf_culling.name")/^?}^/)
                .setTooltip(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.leaf_culling.tooltip"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.leaf_culling.tooltip")/^?}^/)
                .setControl(option -> new CyclingControl<>(option, LeafCullingQuality.class))
                .setBinding(LeafCullingConfig::sodiumleafculling$setQuality, LeafCullingConfig::sodiumleafculling$getQuality)
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build())
            .add(distanceToggle)
            .add(OptionImpl.createBuilder(int.class, leafcullingStorage)
                //? if embeddium_modern {
                /^.setId(ResourceLocation.fromNamespaceAndPath("seleafculling", "leaf_culling_distance"))
                .setEnabledPredicate(distanceToggle::getValue)
                .setControl(option -> new SliderControl(option, DistanceLeafCulling.MIN_DISTANCE,
                    DistanceLeafCulling.MAX_DISTANCE, 1, ControlValueFormatter.number()))
                ^///?} else {
                .setControl(option -> new SliderControl(new EnabledOption<>(option, distanceToggle::getValue),
                    DistanceLeafCulling.MIN_DISTANCE, DistanceLeafCulling.MAX_DISTANCE, 1, ControlValueFormatter.number()))
                //?}
                .setName(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.leaf_culling_distance.name"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.name")/^?}^/)
                .setTooltip(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.leaf_culling_distance.tooltip"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.leaf_culling_distance.tooltip")/^?}^/)
                .setBinding(LeafCullingConfig::sodiumleafculling$setDistance, LeafCullingConfig::sodiumleafculling$getDistance)
                .setImpact(OptionImpact.MEDIUM)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build())
            .add(OptionImpl.createBuilder(boolean.class, leafcullingStorage)
                //? if embeddium_modern {
                /^.setId(ResourceLocation.fromNamespaceAndPath("seleafculling", "cull_adjacent_blocks"))
                ^///?}
                .setName(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.cull_adjacent_blocks.name"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.cull_adjacent_blocks.name")/^?}^/)
                .setTooltip(/^? if fabric_legacy_options {^//^"sodiumleafculling.options.cull_adjacent_blocks.tooltip"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.cull_adjacent_blocks.tooltip")/^?}^/)
                .setControl(TickBoxControl::new)
                .setBinding(LeafCullingConfig::setCullAdjacentBlocks, LeafCullingConfig::cullAdjacentBlocks)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .build())
            .build();
        return new OptionPage(
            //? if embeddium_modern {
            /^group.getId(),
            ^///?}
            /^? if fabric_legacy_options {^//^"Leaf Culling"^//^?} else {^/LeafCulling.translatable("sodiumleafculling.options.page")/^?}^/,
            ImmutableList.of(group));
    }
}
*///?}
