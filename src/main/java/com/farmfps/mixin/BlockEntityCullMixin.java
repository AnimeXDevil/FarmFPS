package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance culling for block entities (chests, hoppers, signs, moving pistons...). Only inside
 * chunks detected as active farms, and only beyond the configured distance, so normal bases
 * look exactly as in vanilla.
 */
@Mixin(BlockEntityRenderer.class)
public interface BlockEntityCullMixin {

    @Inject(
        method = "isInRenderDistance(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/util/math/Vec3d;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void farmfps$cull(BlockEntity blockEntity, Vec3d cameraPos, CallbackInfoReturnable<Boolean> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.blockEntityCull) {
            return;
        }
        BlockPos pos = blockEntity.getPos();
        if (!FarmDetector.shouldAffect(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        double dx = pos.getX() + 0.5 - cameraPos.x;
        double dy = pos.getY() + 0.5 - cameraPos.y;
        double dz = pos.getZ() + 0.5 - cameraPos.z;
        double d = c.blockEntityDistance;
        if (dx * dx + dy * dy + dz * dz > d * d) {
            Stats.blockEntitiesCulled++;
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
