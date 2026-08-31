package toni.sodiumleafculling;

import me.jellysquid.mods.sodium.client.gui.options.TextProvider;

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
    public String getLocalizedName() {
        return this.name;
    }

    public boolean isSolid() {
        return this == SOLID || this == SOLID_AGGRESSIVE;
    }
}
