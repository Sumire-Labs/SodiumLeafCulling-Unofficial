package toni.sodiumleafculling;

//? if forge && <1.17 {
/*import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

// Forge 1.16.5 predates Jar-in-Jar. Initialize our relocated copy before
// Mixin processes the shared renderer's Local annotations.
public final class LeafCullingMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) { MixinExtrasBootstrap.init(); }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo info) { }
}
*///?}
