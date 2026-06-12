package com.vestalihy.mixin;

import com.vestalihy.client.AimingHandler;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.Arrays;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private Vec3 position;
    @Shadow @Final private BlockPos.MutableBlockPos blockPosition;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private float yRot;
    @Shadow private float xRot;

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "setup", at = @At("RETURN"))
    private void vestalihy$onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        if (AimingHandler.isAiming()) {
            LOGGER.info("[aim-debug] MixinCamera.setup: aiming active partialTicks={}", partialTicks);
            Vec3 globalPos = AimingHandler.getAbsoluteWorldCameraPosition(partialTicks);
            Quaternionf globalRot = AimingHandler.getAbsoluteWorldCameraRotation(partialTicks);

            LOGGER.info("[aim-debug] MixinCamera.setup: got globalPos={} globalRot={}", globalPos, globalRot);

            if (globalPos != null && globalRot != null) {
                this.position = globalPos;
                this.blockPosition.set(globalPos.x, globalPos.y, globalPos.z);
                this.rotation.set(globalRot);

                this.forwards.set(0.0F, 0.0F, -1.0F).rotate(globalRot);
                this.up.set(0.0F, 1.0F, 0.0F).rotate(globalRot);
                this.left.set(-1.0F, 0.0F, 0.0F).rotate(globalRot);

                Vector3f euler = globalRot.getEulerAnglesYXZ(new Vector3f());
                this.yRot = -180.0f - (float) Math.toDegrees(euler.y);
                this.xRot = (float) -Math.toDegrees(euler.x);
                LOGGER.info("[aim-debug] MixinCamera.setup: applied rotation yRot={} xRot={} euler={}", this.yRot, this.xRot, euler);
            } else {
                LOGGER.info("[aim-debug] MixinCamera.setup: globalPos or globalRot was null (globalPos={}, globalRot={})", globalPos, globalRot);
            }
        }
    }

    @Inject(
        method = "synaxis$applyCameraBlockPose(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZFLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0,
        expect = 0
    )
    private void vestalihy$cancelSynaxisTransform(CallbackInfo ci) {
        if (com.vestalihy.client.AimingHandler.isAiming()) {
            ci.cancel();
        }
    }
}