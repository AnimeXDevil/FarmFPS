package com.farmfps.mixin;

import com.farmfps.client.VisualLock;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Redirects the light-engine call inside LevelChunk.setBlockState so it can be skipped. */
@Mixin(LevelChunk.class)
public abstract class ChunkLightMixin {

    @Redirect(
        method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;checkBlock(Lnet/minecraft/core/BlockPos;)V"),
        require = 0
    )
    private void farmfps$checkBlock(LevelLightEngine provider, BlockPos pos) {
        Stats.seenLightChecks++;
        if (VisualLock.suppressLight && Minecraft.getInstance().isSameThread()) {
            Stats.lightSkipped++;
            return;
        }
        provider.checkBlock(pos);
    }
}
