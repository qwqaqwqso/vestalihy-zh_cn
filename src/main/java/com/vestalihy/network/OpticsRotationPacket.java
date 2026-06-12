package com.vestalihy.network;

import com.vestalihy.Vestalihy;
import com.vestalihy.block.OpticsModuleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpticsRotationPacket(BlockPos pos, float yaw, float pitch) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpticsRotationPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "optics_rotation"));

    public static final StreamCodec<FriendlyByteBuf, OpticsRotationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpticsRotationPacket::pos,
            ByteBufCodecs.FLOAT,
            OpticsRotationPacket::yaw,
            ByteBufCodecs.FLOAT,
            OpticsRotationPacket::pitch,
            OpticsRotationPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static double getDistanceSqrSafely(ServerPlayer player, BlockPos pos) {
        net.minecraft.world.phys.Vec3 blockWorldPos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        if (com.vestalihy.compat.SableHelper.isAvailable()) {
            Object subLevel = com.vestalihy.compat.SableHelper.getSubLevelManagingPos(player.level(), pos);
            if (subLevel != null) {
                blockWorldPos = com.vestalihy.compat.SableHelper.sublevelToWorld(subLevel, blockWorldPos);
            }
        }
        return player.position().distanceToSqr(blockWorldPos);
    }

    public static void handle(final OpticsRotationPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (getDistanceSqrSafely(player, payload.pos()) > 256) return;

                BlockEntity be = com.vestalihy.compat.SableHelper.isAvailable()
                        ? com.vestalihy.compat.SableHelper.getBlockEntitySafely(player.level(), payload.pos())
                        : player.level().getBlockEntity(payload.pos());
                if (be instanceof OpticsModuleBlockEntity optics) {
                    optics.setRotation(payload.yaw(), payload.pitch());
                }
            }
        });
    }
}
