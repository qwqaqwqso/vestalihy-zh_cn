package com.vestalihy.mixin;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.vestalihy.client.AimingHandler;
import com.vestalihy.client.render.SableThermalRenderTrigger;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class MixinShaderInstance {

    @Inject(method = "apply", at = @At("HEAD"))
    private void onApplyHead(CallbackInfo ci) {
        ShaderInstance self = (ShaderInstance) (Object) this;
        AbstractUniform uniform = self.getUniform("VestalihyThermalVision");
        if (uniform != null) {
            boolean active = AimingHandler.isThermalVisionActive() && SableThermalRenderTrigger.isRenderingSableSubLevel;
            uniform.set(active ? 1.0f : 0.0f);
        }
    }
}
