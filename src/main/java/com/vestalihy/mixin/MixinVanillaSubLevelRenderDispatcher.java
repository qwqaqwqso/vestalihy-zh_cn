package com.vestalihy.mixin;

import com.vestalihy.client.AimingHandler;
import com.vestalihy.client.render.SableThermalRenderTrigger;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VanillaSubLevelRenderDispatcher.class)
public abstract class MixinVanillaSubLevelRenderDispatcher {

    @Inject(method = "renderSectionLayer", at = @At("HEAD"), remap = false)
    private void onRenderSectionLayerHead(CallbackInfo ci) {
        if (AimingHandler.isThermalVisionActive()) {
            SableThermalRenderTrigger.isRenderingSableSubLevel = true;
        }
    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"), remap = false)
    private void onRenderSectionLayerReturn(CallbackInfo ci) {
        SableThermalRenderTrigger.isRenderingSableSubLevel = false;
    }

    @Inject(method = "renderAfterSections", at = @At("HEAD"), remap = false)
    private void onRenderAfterSectionsHead(CallbackInfo ci) {
        if (AimingHandler.isThermalVisionActive()) {
            SableThermalRenderTrigger.isRenderingSableSubLevel = true;
        }
    }

    @Inject(method = "renderAfterSections", at = @At("RETURN"), remap = false)
    private void onRenderAfterSectionsReturn(CallbackInfo ci) {
        SableThermalRenderTrigger.isRenderingSableSubLevel = false;
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"), remap = false)
    private void onRenderBlockEntitiesHead(CallbackInfo ci) {
        if (AimingHandler.isThermalVisionActive()) {
            SableThermalRenderTrigger.isRenderingSableSubLevel = true;
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("RETURN"), remap = false)
    private void onRenderBlockEntitiesReturn(CallbackInfo ci) {
        SableThermalRenderTrigger.isRenderingSableSubLevel = false;
    }
}
