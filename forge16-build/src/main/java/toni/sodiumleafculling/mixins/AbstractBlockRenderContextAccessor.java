package toni.sodiumleafculling.mixins;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SodiumClientMod.class, remap = false)
public interface AbstractBlockRenderContextAccessor {
}
