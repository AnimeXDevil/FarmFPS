package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Second layer for the item cap, hooked on the client level itself. */
@Mixin(ClientLevel.class)
public abstract class ItemWorldMixin {

    @Inject(method = "addEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
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

    @Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$skipHiddenItem(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ItemEntity)) {
            return;
        }
        if (FarmConfig.INSTANCE.enabled && FarmConfig.INSTANCE.hideAllItems) {
            Stats.itemTicksSkipped++;
            ci.cancel();
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
