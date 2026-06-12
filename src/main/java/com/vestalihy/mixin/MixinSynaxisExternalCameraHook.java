package com.vestalihy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.verr1.synaxis.foundation.camera.client.ExternalCameraHook", remap = false)
public class MixinSynaxisExternalCameraHook {

    @Inject(method = "disableSynaxisCameraTransform", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vestalihy$onDisableTransform(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            cir.setReturnValue(true);
        }
    }
}
