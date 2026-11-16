package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Second, independent layer for the dropped-item cap, hooked on the client world itself instead
 * of the network handler: it works even if the packet hook does not apply on this version.
 *  - addEntity: refuses new dropped items once the client holds the configured number near you.
 *    Only used while the packet hook has not seen any item packets, so items are never counted twice.
 *  - tickEntity: skips the client-side simulation of dropped items that are currently hidden.
 */
@Mixin(ClientWorld.class)
public abstract class ItemWorldMixin {

    @Inject(method = "addEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$capAdd(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ItemEntity) || Stats.seenItemPackets != 0) {
            return;
        }
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.capItems || !c.aggressiveItems) {
            return;
        }
        if (EntityBudget.shouldDropItemSpawn(entity.getX(), entity.getY(), entity.getZ())) {
            Stats.spawnsDropped++;
            ci.cancel();
        }
    }

    @Inject(method = "tickEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$skipHiddenItem(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ItemEntity)) {
            return;
        }
        int keep = EntityBudget.itemKeep;
        if (keep >= EntityBudget.FULL || !FarmConfig.INSTANCE.aggressiveItems) {
            return;
        }
        if (EntityBudget.isHidden(entity.getId(), keep)
            && EntityBudget.inRange(entity.getX(), entity.getY(), entity.getZ())) {
            Stats.itemTicksSkipped++;
            ci.cancel();
        }
    }
}
