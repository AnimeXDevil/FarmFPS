package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds block-entity update packets into the farm detector. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class BlockEntityUpdateMixin {

    // RETURN, not HEAD: the handler throws on the network thread and re-runs on the client thread,
    // so RETURN fires exactly once, on the client thread.
    @Inject(
        method = "onBlockEntityUpdate(Lnet/minecraft/network/packet/s2c/play/BlockEntityUpdateS2CPacket;)V",
        at = @At("RETURN")
    )
    private void farmfps$trackBlockEntity(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
        if (FarmConfig.INSTANCE.enabled) {
            BlockPos pos = packet.getPos();
            FarmDetector.record(pos.getX() >> 4, pos.getZ() >> 4, FarmDetector.WEIGHT_BLOCK_ENTITY);
        }
    }
}
