package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F3+B hitboxes are drawn by a debug renderer that ignores the normal "should this entity be
 * drawn" check, so entities hidden by FarmFPS (all drops, filtered items, filtered mobs, fish)
 * still got hitbox lines, which is what made hitboxes so slow near a farm.
 * This skips the hitbox of any entity FarmFPS hides. It lives in its own class and targets the
 * renderer by name, so if the method does not exist in this Minecraft version only this hook is
 * skipped and nothing else is affected.
 */
@Mixin(targets = "net.minecraft.client.render.debug.EntityHitboxDebugRenderer")
public abstract class HitboxFilterMixin {

    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$skipHiddenHitbox(Entity entity, float tickProgress, boolean inLocalServer, CallbackInfo ci) {
        if (HideManager.hidesEntity(entity)) {
            ci.cancel();
        }
    }
}
