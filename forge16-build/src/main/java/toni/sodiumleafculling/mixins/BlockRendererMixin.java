package toni.sodiumleafculling.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import toni.sodiumleafculling.LeafCulling;

@Mixin(value = ChunkRenderRebuildTask.class, remap = false)
public class BlockRendererMixin {
    @Redirect(method = "performBuild", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;get(Lnet/minecraft/client/renderer/RenderType;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;", ordinal = 0))
    private ChunkModelBuffers sodiumleafculling$useSolidLayer(
        ChunkBuildBuffers buffers,
        RenderType layer,
        @Local WorldSlice slice,
        @Local BlockState state,
        @Local BlockPos.Mutable pos
    ) {
        if (state.getBlock() instanceof LeavesBlock && LeafCulling.getQuality().isSolid() && LeafCulling.surroundedByLeaves(slice, pos))
            return buffers.get(RenderType.solid());
        return buffers.get(layer);
    }
}
