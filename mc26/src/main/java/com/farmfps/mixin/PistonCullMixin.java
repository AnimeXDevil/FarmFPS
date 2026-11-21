package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.PistonSnap;
import com.farmfps.core.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Piston snapping: progress is reported as finished so the moving block is drawn at its destination. */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonCullMixin {

    /** Feeds the farm detector. Client level only (the integrated server runs this too). */
    @Inject(
        method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;)V",
        at = @At("HEAD")
    )
    private static void farmfps$trackPiston(Level world, BlockPos pos, BlockState state,
                                            PistonMovingBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClientSide() && FarmConfig.INSTANCE.enabled) {
            Stats.seenPistonTicks++;
            FarmDetector.record(pos.getX() >> 4, pos.getZ() >> 4, FarmDetector.WEIGHT_PISTON);
        }
    }

    /** Ghost mode: the moving block is not drawn while the piston moves it. */
    @Inject(method = "getMovedState()Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void farmfps$ghost(CallbackInfoReturnable<BlockState> cir) {
        FarmConfig cfg = FarmConfig.INSTANCE;
        if (cfg.enabled && cfg.hideMovingBlocks) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            return;
        }
        if (cfg.pistonMode == 2) {
            BlockPos pos = ((BlockEntity) (Object) this).getBlockPos();
            if (PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
                cir.setReturnValue(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Inject(method = "getProgress(F)F", at = @At("HEAD"), cancellable = true)
    private void farmfps$snapProgress(float tickProgress, CallbackInfoReturnable<Float> cir) {
        BlockPos pos = ((BlockEntity) (Object) this).getBlockPos();
        if (PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
            Stats.pistonSnaps++;
            cir.setReturnValue(PistonSnap.DONE);
        }
    }
}
