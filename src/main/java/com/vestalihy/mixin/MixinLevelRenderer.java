package com.vestalihy.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.vestalihy.client.AimingHandler;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"
        )
    )
    private void onDrawSkyBuffers(VertexBuffer instance, Matrix4f pose, Matrix4f projection, ShaderInstance shader) {
        if (!AimingHandler.isThermalVisionActive()) {
            instance.drawWithShader(pose, projection, shader);
        }
    }
}
