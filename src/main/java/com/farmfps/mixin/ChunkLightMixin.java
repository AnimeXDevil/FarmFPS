package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Light skipping that works with Starlight / ScalableLux. Those mods replace the light engine
 * behind LightingProvider, so hooking inside it can be bypassed. This instead intercepts the call
 * made by WorldChunk.setBlockState before it reaches any light engine.
 */
@Mixin(WorldChunk.class)
public abstract class ChunkLightMixin {

    @Redirect(
        method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Lnet/minecraft/block/BlockState;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/light/LightingProvider;checkBlock(Lnet/minecraft/util/math/BlockPos;)V"),
        require = 0
    )
    private void farmfps$checkBlock(LightingProvider provider, BlockPos pos) {
        Stats.seenLightChecks++;
        if (VisualLock.suppressLight && MinecraftClient.getInstance().isOnThread()) {
            Stats.lightSkipped++;
            return;
        }
        provider.checkBlock(pos);
    }
}
