package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Distance culling and hiding for block entities. Only inside detected farm chunks. */
@Mixin(BlockEntityRenderer.class)
public interface BlockEntityCullMixin {

    @Inject(
        method = "shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void farmfps$cull(BlockEntity blockEntity, Vec3 cameraPos, CallbackInfoReturnable<Boolean> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (c.enabled && HideManager.containersActive
            && HideManager.isContainerHidden(blockEntity.getBlockState().getBlock())) {
            Stats.blockEntitiesCulled++;
            cir.setReturnValue(Boolean.FALSE);
            return;
        }
        if (c.enabled && c.hideRedstoneBlocks && blockEntity instanceof PistonMovingBlockEntity) {
            Stats.blockEntitiesCulled++;
            cir.setReturnValue(Boolean.FALSE);
            return;
        }
        if (!c.enabled || !c.blockEntityCull) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
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
