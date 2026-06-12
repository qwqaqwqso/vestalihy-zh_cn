package com.vestalihy.mixin;

import com.vestalihy.client.AimingHandler;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void vestalihy$whiteThermalOverlay(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (AimingHandler.isThermalVisionActive()) {
            // Force full white overlay on all living entities in thermal mode
            // OverlayTexture.u(1.0f) = maximum white, 10 = no hurt tint
            cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(1.0f), 10));
        }
    }
}
