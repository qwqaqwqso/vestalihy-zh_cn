package com.vestalihy.mixin;

import com.vestalihy.client.AimingHandler;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void vestalihy$onTurnPlayer(CallbackInfo ci) {
        // Do not cancel player turning so that mouse movement still updates player rotation
        // for РУС / joystick / aircraft control mods while aiming.
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void vestalihy$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (AimingHandler.isAiming()) {
            LOGGER.info("[aim-debug] MixinMouseHandler.onScroll: yOffset={} window={}", yOffset, window);
            AimingHandler.handleMouseScrollDirect(yOffset);
            ci.cancel();
        }
    }
}
