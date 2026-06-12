package com.vestalihy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vestalihy.block.OpticsModuleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import com.vestalihy.block.OpticsModuleBlock;
import org.joml.Quaternionf;

public class OpticsModuleRenderer implements BlockEntityRenderer<OpticsModuleBlockEntity> {

    public OpticsModuleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(OpticsModuleBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        float x = blockEntity.getOffsetX();
        float y = blockEntity.getOffsetY();
        float z = blockEntity.getOffsetZ();

        poseStack.translate(x, y, z);

        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(OpticsModuleBlock.FACING)) {
            float yaw = blockEntity.getYaw();
            float pitch = blockEntity.getPitch();
            if (yaw != 0.0f || pitch != 0.0f) {
                Direction facing = state.getValue(OpticsModuleBlock.FACING);
                float rotYaw = yaw - facing.toYRot();
                
                poseStack.translate(0.5f, 0.5f, 0.5f);
                poseStack.mulPose(new Quaternionf().rotationYXZ(
                        -rotYaw * (float) (Math.PI / 180.0f),
                        -pitch * (float) (Math.PI / 180.0f),
                        0.0f
                ));
                poseStack.translate(-0.5f, -0.5f, -0.5f);
            }
        }

        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        var model = dispatcher.getBlockModel(state);
        
        var renderType = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(state, false);
        var vertexConsumer = bufferSource.getBuffer(renderType);
        
        dispatcher.getModelRenderer().renderModel(poseStack.last(), vertexConsumer, state, model, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
