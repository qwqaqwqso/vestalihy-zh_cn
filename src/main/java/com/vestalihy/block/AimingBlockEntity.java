package com.vestalihy.block;

import com.vestalihy.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class AimingBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos opticsModulePos = null;

    @Nullable
    private UUID activeOperatorUUID = null;

    public AimingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIMING_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public BlockPos getOpticsModulePos() {
        return opticsModulePos;
    }

    public void setOperator(@Nullable ServerPlayer player) {
        if (player == null) {
            this.activeOperatorUUID = null;
        } else {
            this.activeOperatorUUID = player.getUUID();
        }
        this.setChanged();
    }

    @Nullable
    public ServerPlayer getOperator(Level level) {
        if (activeOperatorUUID == null) return null;
        Player player = level.getPlayerByUUID(activeOperatorUUID);
        if (player instanceof ServerPlayer sp) {
            // Confirm the player is within range
            double distSqr;
            net.minecraft.world.phys.Vec3 blockWorldPos = net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition);
            if (com.vestalihy.compat.SableHelper.isAvailable()) {
                Object subLevel = com.vestalihy.compat.SableHelper.getSubLevelManagingPos(level, this.worldPosition);
                if (subLevel != null) {
                    blockWorldPos = com.vestalihy.compat.SableHelper.sublevelToWorld(subLevel, blockWorldPos);
                }
            }
            distSqr = sp.position().distanceToSqr(blockWorldPos);
            if (distSqr < 256.0) {
                return sp;
            }
        }
        return null;
    }

    public void setOpticsModulePos(@Nullable BlockPos pos) {
        this.opticsModulePos = pos;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (opticsModulePos != null) {
            tag.putInt("optics_x", opticsModulePos.getX());
            tag.putInt("optics_y", opticsModulePos.getY());
            tag.putInt("optics_z", opticsModulePos.getZ());
        }
        if (activeOperatorUUID != null) {
            tag.putUUID("operator_uuid", activeOperatorUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("optics_x")) {
            opticsModulePos = new BlockPos(
                    tag.getInt("optics_x"),
                    tag.getInt("optics_y"),
                    tag.getInt("optics_z"));
        } else {
            opticsModulePos = null;
        }
        if (tag.hasUUID("operator_uuid")) {
            activeOperatorUUID = tag.getUUID("operator_uuid");
        } else {
            activeOperatorUUID = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
