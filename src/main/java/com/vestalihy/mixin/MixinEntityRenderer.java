package com.vestalihy.mixin;

import com.vestalihy.client.AimingHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {

    @Inject(method = "getBlockLightLevel", at = @At("HEAD"), cancellable = true)
    protected void onGetBlockLightLevel(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof LivingEntity && AimingHandler.isThermalVisionActive()) {
            cir.setReturnValue(15);
        }
    }
}
