package com.vestalihy.network;

import com.vestalihy.Vestalihy;
import com.vestalihy.block.AimingBlockEntity;
import com.vestalihy.block.CommanderScopeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AimingPacket(BlockPos cannonMountPos, BlockPos aimingBlockPos, boolean startAiming) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AimingPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "aiming"));

    public static final StreamCodec<FriendlyByteBuf, AimingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
            AimingPacket::cannonMountPosOpt,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
            AimingPacket::aimingBlockPosOpt,
            ByteBufCodecs.BOOL,
            AimingPacket::startAiming,
            AimingPacket::new
    );

    private java.util.Optional<BlockPos> cannonMountPosOpt() {
        return java.util.Optional.ofNullable(cannonMountPos);
    }

    private java.util.Optional<BlockPos> aimingBlockPosOpt() {
        return java.util.Optional.ofNullable(aimingBlockPos);
    }

    private AimingPacket(java.util.Optional<BlockPos> mountPos, java.util.Optional<BlockPos> aimingPos, boolean start) {
        this(mountPos.orElse(null), aimingPos.orElse(null), start);
    }

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

    public static void handle(final AimingPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.startAiming()) {
                    if (payload.cannonMountPos() != null && getDistanceSqrSafely(player, payload.cannonMountPos()) > 256)
                        return;
                    if (payload.aimingBlockPos() != null && getDistanceSqrSafely(player, payload.aimingBlockPos()) > 256)
                        return;
                }

                if (payload.aimingBlockPos() != null) {
                    BlockEntity be = com.vestalihy.compat.SableHelper.isAvailable()
                            ? com.vestalihy.compat.SableHelper.getBlockEntitySafely(player.level(), payload.aimingBlockPos())
                            : player.level().getBlockEntity(payload.aimingBlockPos());
                    if (be instanceof CommanderScopeBlockEntity scope) {
                        if (payload.startAiming()) {
                            scope.setOperator(player);
                        } else {
                            scope.setOperator(null);
                        }
                    } else if (be instanceof AimingBlockEntity aiming) {
                        if (payload.startAiming()) {
                            aiming.setOperator(player);
                        } else {
                            aiming.setOperator(null);
                        }
                    }
                }
            }
        });
    }
}
