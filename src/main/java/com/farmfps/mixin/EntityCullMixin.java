package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps visible dropped items and XP orbs.
 *
 * Hooks the shared {@code shouldRender} visibility check, which every entity renderer goes through,
 * instead of the item/XP renderers' render methods (those signatures differ by version).
 * Hidden entities still exist and tick normally, so hoppers, collection and despawn are unaffected.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityCullMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void farmfps$cap(Entity entity, Frustum frustum, double x, double y, double z,
                             CallbackInfoReturnable<Boolean> cir) {
        int keep;
        if (entity instanceof ItemEntity) {
            keep = EntityBudget.itemKeep;
        } else if (entity instanceof ExperienceOrbEntity) {
            keep = EntityBudget.xpKeep;
        } else {
            return;
        }
        if (keep >= EntityBudget.FULL) {
            return;
        }
        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        double r = FarmConfig.INSTANCE.cullRadius;
        if (dx * dx + dy * dy + dz * dz <= r * r && EntityBudget.isHidden(entity.getId(), keep)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
