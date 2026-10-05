package toni.sodiumleafculling.mixins;

//? if sodium_caffeine {
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if sodium_modern_renderer {
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
//?} else {
/*import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
*///?}

@Mixin(value = AbstractBlockRenderContext.class, remap = false, priority = 100)
public interface AbstractBlockRenderContextAccessor {
    @Accessor
    BlockState getState();

    @Accessor
    LevelSlice getSlice();

    @Accessor
    BlockPos getPos();
}
//?}
