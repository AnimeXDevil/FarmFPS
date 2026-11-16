package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
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
        if (entity instanceof ItemEntity item) {
            if (farmfps$hideDrop(item)) {
                Stats.rendersHidden++;
                cir.setReturnValue(Boolean.FALSE);
                return;
            }
            keep = EntityBudget.itemKeep;
        } else if (entity instanceof ExperienceOrbEntity) {
            keep = EntityBudget.xpKeep;
        } else {
            farmfps$cullFarEntity(entity, x, y, z, cir);
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

    /**
     * Farm mode: hide all drops, or only the filtered item types. With hitboxes on (F3+B) drops are
     * drawn normally so they can be inspected.
     */
    private static boolean farmfps$hideDrop(ItemEntity item) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !(c.hideAllItems || c.hideFilteredItems)) {
            return false;
        }
        if (MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes()) {
            return false;
        }
        return c.hideAllItems || HideManager.isItemHidden(item.getStack().getItem());
    }

    /** Far mobs, minecarts, item frames and the like inside farm chunks (players are never hidden). */
    private static void farmfps$cullFarEntity(Entity entity, double x, double y, double z,
                                              CallbackInfoReturnable<Boolean> cir) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.entityCull || entity instanceof PlayerEntity) {
            return;
        }
        double ex = entity.getX();
        double ez = entity.getZ();
        if (!FarmDetector.shouldAffect(((int) Math.floor(ex)) >> 4, ((int) Math.floor(ez)) >> 4)) {
            return;
        }
        double dx = ex - x;
        double dy = entity.getY() - y;
        double dz = ez - z;
        double d = c.entityRenderDistance;
        if (dx * dx + dy * dy + dz * dz > d * d) {
            Stats.entitiesCulled++;
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
