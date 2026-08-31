package toni.sodiumleafculling;

import net.minecraft.network.chat.Component;
//? if embeddium_modern {
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
//?} elif sodium_caffeine {
import net.caffeinemc.mods.sodium.client.gui.options.TextProvider;
//?} else {
/*import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
*///?}

public enum LeafCullingQuality implements TextProvider {
    NONE("options.leaf_culling.none"),
    HOLLOW("options.leaf_culling.hollow"),
    SOLID("options.leaf_culling.solid"),
    SOLID_AGGRESSIVE("options.leaf_culling.solid_aggressive");

    private final String name;

    LeafCullingQuality(String name) {
        this.name = name;
    }

    @Override
    public /*? if string_options {*/String/*?} else {*//*Component*//*?}*/ getLocalizedName() {
        //? if string_options {
        return this.name;
        //?} else {
        /*return LeafCulling.translatable(this.name);
        *///?}
    }

    public boolean isSolid() {
        return this == SOLID || this == SOLID_AGGRESSIVE;
    }
}
