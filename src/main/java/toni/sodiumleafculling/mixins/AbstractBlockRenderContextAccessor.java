package toni.sodiumleafculling.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if sodium_modern_renderer {
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
//?} elif sodium_caffeine {
/*import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
*///?} else {
/*import me.jellysquid.mods.sodium.client.SodiumClientMod;
*///?}

@Mixin(value = /*? if sodium_caffeine {*/AbstractBlockRenderContext.class/*?} else {*//*SodiumClientMod.class*//*?}*/, remap = false, priority = 100)
public interface AbstractBlockRenderContextAccessor {
    //? if sodium_caffeine {
    @Accessor
    BlockState getState();

    @Accessor
    LevelSlice getSlice();

    @Accessor
    BlockPos getPos();
    //?}
}
