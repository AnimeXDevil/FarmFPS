package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips the client-side simulation of dropped items that are currently hidden by the cap. */
@Mixin(ItemEntity.class)
public abstract class ItemTickMixin {

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void farmfps$skipHiddenTick(CallbackInfo ci) {
        int keep = EntityBudget.itemKeep;
        if (keep >= EntityBudget.FULL || !FarmConfig.INSTANCE.aggressiveItems) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        if (!EntityBudget.isHidden(self.getId(), keep)
            || !EntityBudget.inRange(self.getX(), self.getY(), self.getZ())) {
            return;
        }
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || world.getEntity(self.getId()) != self) {
            return;
        }
        Stats.itemTicksSkipped++;
        ci.cancel();
    }
}
