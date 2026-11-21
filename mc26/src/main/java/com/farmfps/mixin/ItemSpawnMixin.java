package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Packet-level item cap: extra dropped-item spawn packets are ignored on the client. */
@Mixin(ClientPacketListener.class)
public abstract class ItemSpawnMixin {

    private static EntityType<?> farmfps$itemType;

    /** EntityType.ITEM is not a field name in 26.2, so resolve it once by registry id. */
    private static EntityType<?> farmfps$itemType() {
        EntityType<?> t = farmfps$itemType;
        if (t == null) {
            for (EntityType<?> e : BuiltInRegistries.ENTITY_TYPE) {
                if (BuiltInRegistries.ENTITY_TYPE.getKey(e).toString().equals("minecraft:item")) {
                    t = e;
                    farmfps$itemType = e;
                    break;
                }
            }
        }
        return t;
    }

    @Inject(
        method = "handleAddEntity(Lnet/minecraft/network/protocol/game/ClientboundAddEntityPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void farmfps$capItemSpawns(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.capItems || !c.aggressiveItems) {
            return;
        }
        if (packet.getType() != farmfps$itemType()) {
            return;
        }
        // Runs on the network thread and again on the client thread; decide on the client thread only.
        if (!Minecraft.getInstance().isSameThread()) {
            return;
        }
        Stats.seenItemPackets++;
        if (EntityBudget.shouldDropItemSpawn(packet.getX(), packet.getY(), packet.getZ())) {
            Stats.spawnsDropped++;
            ci.cancel();
        }
    }
}
