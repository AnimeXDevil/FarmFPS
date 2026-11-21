package com.farmfps.mixin;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block-entity data packets feed the farm detector. */
@Mixin(ClientPacketListener.class)
public abstract class BlockEntityUpdateMixin {

    // RETURN, not HEAD: the handler throws on the network thread and re-runs on the client thread.
    @Inject(
        method = "handleBlockEntityData(Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void farmfps$trackBlockEntity(ClientboundBlockEntityDataPacket packet, CallbackInfo ci) {
        if (FarmConfig.INSTANCE.enabled) {
            BlockPos pos = packet.getPos();
            FarmDetector.record(pos.getX() >> 4, pos.getZ() >> 4, FarmDetector.WEIGHT_BLOCK_ENTITY);
        }
    }
}
