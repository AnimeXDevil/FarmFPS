package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.PistonSnap;
import com.farmfps.core.Stats;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Piston snapping.
 *
 * Instead of hooking the renderer (whose signature changes between versions), this hooks the
 * animation progress that the renderer reads. Returning 1.0 draws the moving block at its final
 * position for both extending and retracting pistons. Only rendering is affected; the real
 * block-entity state, collisions and server logic are untouched.
 */
@Mixin(PistonBlockEntity.class)
public abstract class PistonCullMixin {

    /** Feeds the farm detector. Client world only, since the integrated server runs this too. */
    @Inject(
        method = "tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/PistonBlockEntity;)V",
        at = @At("HEAD")
    )
    private static void farmfps$trackPiston(World world, BlockPos pos, BlockState state,
                                            PistonBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient() && FarmConfig.INSTANCE.enabled) {
            Stats.seenPistonTicks++;
            FarmDetector.record(pos.getX() >> 4, pos.getZ() >> 4, FarmDetector.WEIGHT_PISTON);
        }
    }

    /**
     * Ghost mode: the moving block is not drawn at all while the piston moves it; it simply
     * appears at its destination when the move ends. The real block state is unaffected.
     */
    @Inject(method = "getPushedBlock()Lnet/minecraft/block/BlockState;", at = @At("HEAD"), cancellable = true)
    private void farmfps$ghost(CallbackInfoReturnable<BlockState> cir) {
        FarmConfig cfg = FarmConfig.INSTANCE;
        if (cfg.enabled && cfg.hideMovingBlocks) {
            cir.setReturnValue(Blocks.AIR.getDefaultState());
            return;
        }
        if (cfg.pistonMode == 2) {
            BlockPos pos = ((BlockEntity) (Object) this).getPos();
            if (PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
                cir.setReturnValue(Blocks.AIR.getDefaultState());
            }
        }
    }

    @Inject(method = "getProgress(F)F", at = @At("HEAD"), cancellable = true)
    private void farmfps$snapProgress(float tickProgress, CallbackInfoReturnable<Float> cir) {
        BlockPos pos = ((BlockEntity) (Object) this).getPos();
        if (PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
            Stats.pistonSnaps++;
            cir.setReturnValue(PistonSnap.DONE);
        }
    }
}
