package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.PistonSnap;
import net.minecraft.block.BlockState;
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
    @Inject(method = "tick", at = @At("HEAD"))
    private static void farmfps$trackPiston(World world, BlockPos pos, BlockState state,
                                            PistonBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient() && FarmConfig.INSTANCE.enabled) {
            FarmDetector.record(pos.getX() >> 4, pos.getZ() >> 4, FarmDetector.WEIGHT_PISTON);
        }
    }

    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true)
    private void farmfps$snapProgress(float tickProgress, CallbackInfoReturnable<Float> cir) {
        BlockPos pos = ((BlockEntity) (Object) this).getPos();
        if (PistonSnap.shouldSnap(pos.getX() >> 4, pos.getZ() >> 4)) {
            cir.setReturnValue(PistonSnap.DONE);
        }
    }
}
