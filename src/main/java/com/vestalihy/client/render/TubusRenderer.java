package com.vestalihy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vestalihy.Vestalihy;
import com.vestalihy.entity.TubusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class TubusRenderer extends EntityRenderer<TubusEntity> {

    private static final ResourceLocation JSON_LOADED = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "models/block/tubus.json");
    // Для пустого тубуса используем ту же модель (разные текстуры)
    private static final ResourceLocation JSON_EMPTY  = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "models/block/tubus.json");

    private static final ResourceLocation TEXTURE_LOADED = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "textures/entity/tubus.png");
    private static final ResourceLocation TEXTURE_EMPTY  = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "textures/entity/tubus_empty.png");

    private BbModelRenderer modelLoaded;
    private BbModelRenderer modelEmpty;

    public TubusRenderer(EntityRendererProvider.Context context) {
        super(context);
        reloadModels(context.getResourceManager());
    }

    public void reloadModels(net.minecraft.server.packs.resources.ResourceManager rm) {
        modelLoaded = BbModelRenderer.load(rm, JSON_LOADED);
        modelEmpty  = BbModelRenderer.load(rm, JSON_EMPTY);
    }

    @Override
    public void render(TubusEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        BbModelRenderer model    = entity.isEmpty() ? modelEmpty : modelLoaded;
        ResourceLocation texture = entity.isEmpty() ? TEXTURE_EMPTY : TEXTURE_LOADED;

        poseStack.pushPose();

        Direction facing = entity.getFacing();
        // Вращаем вокруг центра хитбокса (0,0,0 в пространстве рендера)
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180f));
        // X: центр = 8.0 px
        // Y: поднимаем модель — центр хитбокса (0.275) совпадает с центром модели (4.5/16=0.281)
        // Z: центр по длине без наклонного элемента = 7.525 px
        poseStack.translate(-8.0f / 16f, -4.5f / 16f + 0.275f, -7.525f / 16f);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.render(poseStack, consumer, packedLight);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TubusEntity entity) {
        return entity.isEmpty() ? TEXTURE_EMPTY : TEXTURE_LOADED;
    }
}
