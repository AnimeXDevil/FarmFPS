package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the client-side simulation (movement, collisions, water flow) of dropped items that are
 * currently hidden by the cap. Their position still follows the server's sync packets.
 * Runs only for the real client-world copy of the entity, so singleplayer's server-side items
 * keep ticking normally.
 */
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
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null || world.getEntityById(self.getId()) != self) {
            return;
        }
        Stats.itemTicksSkipped++;
        ci.cancel();
    }
}
