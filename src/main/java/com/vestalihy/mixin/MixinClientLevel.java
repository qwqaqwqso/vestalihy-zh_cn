package com.vestalihy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.vestalihy.client.AimingHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void onGetSkyColor(Vec3 pPos, float pPartialTick, CallbackInfoReturnable<Vec3> cir) {
        if (AimingHandler.isThermalVisionActive()) {
            cir.setReturnValue(Vec3.ZERO);
        }
    }

    @Inject(method = "getStarBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetStarBrightness(float pPartialTick, CallbackInfoReturnable<Float> cir) {
        if (AimingHandler.isThermalVisionActive()) {
            cir.setReturnValue(0.0F);
        }
    }
}
