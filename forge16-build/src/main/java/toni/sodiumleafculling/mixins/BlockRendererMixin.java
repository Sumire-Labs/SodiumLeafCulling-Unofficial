package toni.sodiumleafculling.mixins;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import toni.sodiumleafculling.LeafCulling;

@Mixin(value = ChunkRenderRebuildTask.class, remap = false)
public class BlockRendererMixin {
    @Redirect(method = "performBuild", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer;renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/model/IBakedModel;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuffers;ZJLnet/minecraftforge/client/model/data/IModelData;)Z"))
    private boolean sodiumleafculling$useSolidLayer(
        BlockRenderer renderer,
        IBlockDisplayReader view,
        BlockState state,
        BlockPos pos,
        IBakedModel model,
        ChunkModelBuffers modelBuffers,
        boolean cull,
        long seed,
        IModelData modelData,
        ChunkRenderCacheLocal cache,
        ChunkBuildBuffers buffers,
        CancellationSource cancellationSource
    ) {
        if (state.getBlock() instanceof LeavesBlock && LeafCulling.getQuality().isSolid() && LeafCulling.surroundedByLeaves(view, pos))
            modelBuffers = buffers.get(RenderType.solid());
        return renderer.renderModel(view, state, pos, model, modelBuffers, cull, seed, modelData);
    }
}
