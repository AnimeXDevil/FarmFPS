package com.farmfps.mixin;

import com.farmfps.core.EntityBudget;
import com.farmfps.core.Stats;
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
 * Hides excess dropped items and XP orbs that already exist on the client. Hooks the shared
 * {@code shouldRender} check, which every entity renderer goes through. Hidden entities keep
 * existing; they are just not drawn.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityCullMixin {

    @Inject(
        method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
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
        if (EntityBudget.isHidden(entity.getId(), keep)
            && EntityBudget.inRange(entity.getX(), entity.getY(), entity.getZ())) {
            Stats.rendersHidden++;
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
