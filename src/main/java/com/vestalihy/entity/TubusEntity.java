package com.vestalihy.entity;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.vestalihy.block.AimingBlock;
import com.vestalihy.block.CommanderScopeBlock;
import com.vestalihy.item.OpticsConnectorItem;
import com.vestalihy.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class TubusEntity extends Entity {

    // Хитбокс тубуса — чуть меньше реальной модели (0.55 блока широкий, 0.55 высокий)
    private static final EntityDimensions TUBUS_DIMS = EntityDimensions.scalable(0.55f, 0.55f);

    private static final EntityDataAccessor<Boolean> IS_EMPTY = SynchedEntityData.defineId(TubusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(TubusEntity.class, EntityDataSerializers.DIRECTION);

    @Nullable
    private BlockPos commanderScopePos = null;

    public TubusEntity(EntityType<? extends TubusEntity> type, Level level) {
        super(type, level);
    }

    public boolean isEmpty() {
        return this.entityData.get(IS_EMPTY);
    }

    public void setEmpty(boolean empty) {
        this.entityData.set(IS_EMPTY, empty);
    }

    public Direction getFacing() {
        return this.entityData.get(FACING);
    }

    public void setFacing(Direction facing) {
        this.entityData.set(FACING, facing);
    }

    @Nullable
    public BlockPos getCommanderScopePos() {
        return this.commanderScopePos;
    }

    public void setCommanderScopePos(@Nullable BlockPos pos) {
        this.commanderScopePos = pos;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_EMPTY, false);
        builder.define(FACING, Direction.NORTH);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setEmpty(tag.getBoolean("IsEmpty"));
        if (tag.contains("Facing")) {
            this.setFacing(Direction.byName(tag.getString("Facing")));
        }
        if (tag.contains("scope_x")) {
            this.commanderScopePos = new BlockPos(
                    tag.getInt("scope_x"),
                    tag.getInt("scope_y"),
                    tag.getInt("scope_z")
            );
        } else {
            this.commanderScopePos = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("IsEmpty", this.isEmpty());
        tag.putString("Facing", this.getFacing().getName());
        if (this.commanderScopePos != null) {
            tag.putInt("scope_x", this.commanderScopePos.getX());
            tag.putInt("scope_y", this.commanderScopePos.getY());
            tag.putInt("scope_z", this.commanderScopePos.getZ());
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return TUBUS_DIMS;
    }

    // Импорт WrenchItem подключён выше — Create как compileOnly зависимость

    /**
     * Проверяет является ли предмет ключом Create — как в Tallyho через instanceof WrenchItem.
     */
    private static boolean isCreateWrench(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof WrenchItem;
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ПКМ ключом Create — убираем тубус
        if (isCreateWrench(stack)) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (stack.getItem() instanceof OpticsConnectorItem) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("commander_scope_x")) {
                    BlockPos scopePos = new BlockPos(
                            tag.getInt("commander_scope_x"),
                            tag.getInt("commander_scope_y"),
                            tag.getInt("commander_scope_z")
                    );

                    net.minecraft.world.level.block.state.BlockState scopeState = com.vestalihy.compat.SableHelper.isAvailable()
                            ? com.vestalihy.compat.SableHelper.getBlockStateSafely(this.level(), scopePos)
                            : this.level().getBlockState(scopePos);
                    if (scopeState.getBlock() instanceof CommanderScopeBlock || scopeState.getBlock() instanceof AimingBlock) {
                        if (!this.level().isClientSide) {
                            this.setCommanderScopePos(scopePos);
                            String name = scopeState.getBlock() instanceof CommanderScopeBlock ? "командирскому прицелу" : "прицелу";
                            player.displayClientMessage(Component.literal("§aТПК привязан к " + name + "!"), true);
                        }
                        return InteractionResult.sidedSuccess(this.level().isClientSide);
                    } else {
                        if (!this.level().isClientSide) {
                            player.displayClientMessage(Component.literal("§cПрицел не найден на привязанной позиции!"), true);
                        }
                        return InteractionResult.FAIL;
                    }
                }
            }
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.literal("§eСначала привяжите прицел (ПКМ по прицелу)"), true);
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public boolean launch() {
        if (this.isEmpty()) {
            return false;
        }

        this.setEmpty(true);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.vestalihy.registry.ModSounds.FIRE_PTUR.get(), SoundSource.BLOCKS, 1.5f, 1.2f);

        if (!this.level().isClientSide) {
            PturEntity ptur = new PturEntity(ModEntities.PTUR.get(), this.level());
            
            // Spawn in front of the tubus
            Direction facing = this.getFacing();
            double dx = facing.getNormal().getX() * 0.8;
            double dy = 0.3; // slightly offset vertically to align with the barrel center
            double dz = facing.getNormal().getZ() * 0.8;
            
            ptur.moveTo(this.getX() + dx, this.getY() + dy, this.getZ() + dz, net.minecraft.util.Mth.wrapDegrees(facing.toYRot()), 0.0f);
            
            Vec3 initialVel = new Vec3(facing.getNormal().getX(), facing.getNormal().getY(), facing.getNormal().getZ()).scale(0.4);
            ptur.setDeltaMovement(initialVel);
            ptur.setCommanderScopePos(this.commanderScopePos);
            
            this.level().addFreshEntity(ptur);
        }
        return true;
    }
}
