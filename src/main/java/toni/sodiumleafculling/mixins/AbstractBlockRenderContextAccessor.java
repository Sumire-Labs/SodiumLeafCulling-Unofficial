package toni.sodiumleafculling.mixins;



import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

#if AFTER_26_1_1
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
#elif AFTER_21_1
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
#else
import me.jellysquid.mods.sodium.client.SodiumClientMod;
#endif

@Mixin(value = #if AFTER_21_1 AbstractBlockRenderContext.class #else SodiumClientMod.class #endif, remap = false, priority = 100)
public interface AbstractBlockRenderContextAccessor {
    #if AFTER_21_1
    @Accessor
    BlockState getState();

    @Accessor
    LevelSlice getSlice();

    @Accessor
    BlockPos getPos();
    #endif
}

