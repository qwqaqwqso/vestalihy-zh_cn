package com.vestalihy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vestalihy.Vestalihy;
import com.vestalihy.entity.PturEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PturRenderer extends EntityRenderer<PturEntity> {

    private static final ResourceLocation JSON_LOC = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "models/block/ptur.json");
    private static final ResourceLocation TEXTURE  = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "textures/entity/ptur.png");

    private BbModelRenderer model;

    public PturRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = BbModelRenderer.load(context.getResourceManager(), JSON_LOC);
    }

    @Override
    public void render(PturEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        float yRotO = Mth.wrapDegrees(entity.yRotO);
        float yRotCurrent = Mth.wrapDegrees(entity.getYRot());

        if (yRotCurrent - yRotO < -180.0F) {
            yRotO -= 360.0F;
        } else if (yRotCurrent - yRotO > 180.0F) {
            yRotO += 360.0F;
        }

        float yRot = Mth.lerp(partialTick, yRotO, yRotCurrent);
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Вращаем: нос модели смотрит в -Z (север в BB), MC yaw 0 = юг → rotate -yRot + 180
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + 180.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        // Центрируем модель по XY (центр тела 8.0, 2.5), нос смотрит в +Z → Z-смещение не нужно
        // Z: тело от -16 до 25 — нос на +Z, хвост на -Z, центр по длине = 4.5 px
        poseStack.translate(-8.0f / 16f, -2.5f / 16f, -4.5f / 16f);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.render(poseStack, consumer, packedLight);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PturEntity entity) {
        return TEXTURE;
    }
}
