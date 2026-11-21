package com.farmfps.mixin;

import com.farmfps.client.HideManager;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entities FarmFPS hides do not get a debug hitbox either. */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class HitboxFilterMixin {

    @Inject(method = "showHitboxes(Lnet/minecraft/world/entity/Entity;FZ)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmfps$skipHiddenHitbox(Entity entity, float partialTick, boolean inLocalServer, CallbackInfo ci) {
        if (HideManager.hidesEntity(entity)) {
            ci.cancel();
        }
    }
}
