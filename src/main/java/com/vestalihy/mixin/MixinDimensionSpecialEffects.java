package com.vestalihy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.vestalihy.client.AimingHandler;
import net.minecraft.client.renderer.DimensionSpecialEffects;

@Mixin(DimensionSpecialEffects.class)
public abstract class MixinDimensionSpecialEffects {

    @Inject(method = "getSunriseColor", at = @At("HEAD"), cancellable = true)
    private void onGetSunriseColor(float pTimeOfDay, float pPartialTick, CallbackInfoReturnable<float[]> cir) {
        if (AimingHandler.isThermalVisionActive()) {
            cir.setReturnValue(null);
        }
    }
}
