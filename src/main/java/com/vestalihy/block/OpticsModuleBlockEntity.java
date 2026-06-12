package com.vestalihy.block;

import com.vestalihy.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public class OpticsModuleBlockEntity extends BlockEntity {
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;
    private float yaw = 0.0f;
    private float pitch = 0.0f;

    public OpticsModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OPTICS_MODULE_BLOCK_ENTITY.get(), pos, state);
    }

    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public void setOffsets(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("OffsetX", offsetX);
        tag.putFloat("OffsetY", offsetY);
        tag.putFloat("OffsetZ", offsetZ);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.offsetX = tag.getFloat("OffsetX");
        this.offsetY = tag.getFloat("OffsetY");
        this.offsetZ = tag.getFloat("OffsetZ");
        this.yaw = tag.getFloat("Yaw");
        this.pitch = tag.getFloat("Pitch");
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
