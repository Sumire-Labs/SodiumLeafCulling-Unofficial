package com.sumirelabs.sodiumleafculling;

//? if dual_renderer {
/*import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class RendererMixinPlugin implements IMixinConfigPlugin {
    private boolean embeddiumConfig;

    @Override
    public void onLoad(String mixinPackage) {
        embeddiumConfig = mixinPackage.endsWith(".embeddium.mixins");
    }

    @Override
    public List<String> getMixins() {
        if (!FMLEnvironment.dist.isClient()) return List.of();
        LoadingModList mods = LoadingModList.get();
        boolean hasEmbeddium = mods.getModFileById("embeddium") != null;
        if (embeddiumConfig != hasEmbeddium ||
                (!hasEmbeddium && mods.getModFileById("sodium") == null)) {
            return List.of();
        }
        return rendererMixins(hasEmbeddium);
    }

    static List<String> rendererMixins(boolean embeddium) {
        List<String> mixins = new ArrayList<>(List.of("BlockOcclusionCacheMixin", "BlockRendererMixin",
                "ChunkMeshingTaskMixin", "WorldRendererMixin", "SodiumGameOptionPagesMixin"));
        if (!embeddium) mixins.add("AbstractBlockRenderContextAccessor");
        return mixins;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
}
*///?}
