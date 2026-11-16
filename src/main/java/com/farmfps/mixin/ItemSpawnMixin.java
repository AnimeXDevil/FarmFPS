package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packet-level item cap. Once the client already holds the configured number of dropped items
 * near the player, further item spawn packets are ignored, so the extra entities are never
 * created, ticked, tracked or drawn. The server still has them: hoppers collect them and you can
 * still pick them up. Other packets that mention an unknown entity id are ignored by vanilla.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ItemSpawnMixin {

    @Inject(
        method = "onEntitySpawn(Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$capItemSpawns(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.capItems || !c.aggressiveItems) {
            return;
        }
        if (packet.getEntityType() != EntityType.ITEM) {
            return;
        }
        // The packet handler runs once on the network thread and again on the client thread.
        // Decide only on the client-thread pass.
        if (!MinecraftClient.getInstance().isOnThread()) {
            return;
        }
        Stats.seenItemPackets++;
        if (EntityBudget.shouldDropItemSpawn(packet.getX(), packet.getY(), packet.getZ())) {
            Stats.spawnsDropped++;
            ci.cancel();
        }
    }
}
