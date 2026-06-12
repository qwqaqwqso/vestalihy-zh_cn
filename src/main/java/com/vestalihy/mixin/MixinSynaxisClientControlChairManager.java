package com.vestalihy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.verr1.synaxis.foundation.input.ClientControlChairManager", remap = false)
public class MixinSynaxisClientControlChairManager {

    @Inject(method = "isViewLockActive", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vestalihy$onIsViewLockActive(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming() && com.vestalihy.client.AimingHandler.isCommanderScope()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldCancelMouseTurn", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vestalihy$onShouldCancelMouseTurn(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming() && com.vestalihy.client.AimingHandler.isCommanderScope()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldOverrideMountedCamera", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vestalihy$onShouldOverrideMountedCamera(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldOwnCameraRotation", at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
    private static void vestalihy$onShouldOwnCameraRotation(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldCaptureLook", at = @At("HEAD"), cancellable = true, remap = false, require = 0, expect = 0)
    private static void vestalihy$onShouldCaptureLook(CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isReservedKey", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vestalihy$onIsReservedKey(int key, int scancode, CallbackInfoReturnable<Boolean> cir) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.options.keyShift.matches(key, scancode)) {
                cir.setReturnValue(true);
                return;
            }
            if (com.vestalihy.client.Keybinds.THERMAL_VISION_KEY.matches(key, scancode)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "chairFacingAngles", at = @At("RETURN"), cancellable = true, remap = false)
    private static void vestalihy$onChairFacingAngles(CallbackInfoReturnable<Object> cir) {
        Object angles = cir.getReturnValue();
        if (angles != null) {
            try {
                float yaw = 0;
                float pitch = 0;
                
                try {
                    java.lang.reflect.Field yawField = angles.getClass().getDeclaredField("yaw");
                    yawField.setAccessible(true);
                    yaw = yawField.getFloat(angles);
                } catch (NoSuchFieldException e) {
                    java.lang.reflect.Method yawMethod = angles.getClass().getDeclaredMethod("yaw");
                    yawMethod.setAccessible(true);
                    yaw = (float) yawMethod.invoke(angles);
                }

                try {
                    java.lang.reflect.Field pitchField = angles.getClass().getDeclaredField("pitch");
                    pitchField.setAccessible(true);
                    pitch = pitchField.getFloat(angles);
                } catch (NoSuchFieldException e) {
                    java.lang.reflect.Method pitchMethod = angles.getClass().getDeclaredMethod("pitch");
                    pitchMethod.setAccessible(true);
                    pitch = (float) pitchMethod.invoke(angles);
                }

                // Add 180 degrees to the yaw to fix the front/back camera click raycast misalignment
                float correctedYaw = (yaw + 180.0f) % 360.0f;

                java.lang.reflect.Constructor<?> constructor = angles.getClass().getDeclaredConstructor(float.class, float.class);
                constructor.setAccessible(true);
                Object correctedAngles = constructor.newInstance(correctedYaw, pitch);

                cir.setReturnValue(correctedAngles);
            } catch (Throwable t) {
                // Fail-safe: do not crash if reflection fails
            }
        }
    }
}
