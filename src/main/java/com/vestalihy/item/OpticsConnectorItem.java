package com.vestalihy.item;

import com.vestalihy.block.AimingBlock;
import com.vestalihy.block.AimingBlockEntity;
import com.vestalihy.block.OpticsModuleBlock;
import com.vestalihy.block.CommanderScopeBlock;
import com.vestalihy.block.CommanderScopeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OpticsConnectorItem extends Item {

    public OpticsConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        // ПКМ по модулю оптики — запоминаем позицию
        if (state.getBlock() instanceof OpticsModuleBlock) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putInt("optics_x", pos.getX());
                tag.putInt("optics_y", pos.getY());
                tag.putInt("optics_z", pos.getZ());
            });

            if (!level.isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.literal("§aМодуль оптики привязан [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"),
                        true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ПКМ по прицелу (AimingBlock или CommanderScopeBlock) — подключаем модуль
        if (state.getBlock() instanceof AimingBlock || state.getBlock() instanceof CommanderScopeBlock) {
            boolean isScope = state.getBlock() instanceof CommanderScopeBlock;
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            boolean connected = false;
            
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("optics_x")) {
                    BlockPos modulePos = new BlockPos(
                            tag.getInt("optics_x"),
                            tag.getInt("optics_y"),
                            tag.getInt("optics_z"));

                    // Проверяем что модуль оптики всё ещё на месте
                    net.minecraft.world.level.block.state.BlockState moduleState = com.vestalihy.compat.SableHelper.isAvailable()
                            ? com.vestalihy.compat.SableHelper.getBlockStateSafely(level, modulePos)
                            : level.getBlockState(modulePos);
                    if (!(moduleState.getBlock() instanceof OpticsModuleBlock)) {
                        if (!level.isClientSide && context.getPlayer() != null) {
                            context.getPlayer().displayClientMessage(
                                    Component.literal("§cМодуль оптики не найден на привязанной позиции!"),
                                    true);
                        }
                        return InteractionResult.FAIL;
                    }

                    // Записываем позицию модуля в AimingBlockEntity или CommanderScopeBlockEntity
                    BlockEntity be = com.vestalihy.compat.SableHelper.isAvailable()
                            ? com.vestalihy.compat.SableHelper.getBlockEntitySafely(level, pos)
                            : level.getBlockEntity(pos);
                    if (be instanceof AimingBlockEntity aimingBE) {
                        aimingBE.setOpticsModulePos(modulePos);
                        connected = true;
                    } else if (be instanceof CommanderScopeBlockEntity scopeBE) {
                        scopeBE.setOpticsModulePos(modulePos);
                        connected = true;
                    }
                }
            }
            
            // Записываем его координаты в предмет для последующей привязки к ПТУРу
            CustomData.update(DataComponents.CUSTOM_DATA, stack, itemTag -> {
                itemTag.putInt("commander_scope_x", pos.getX());
                itemTag.putInt("commander_scope_y", pos.getY());
                itemTag.putInt("commander_scope_z", pos.getZ());
            });
            
            if (!level.isClientSide && context.getPlayer() != null) {
                String name = isScope ? "Командирский прицел" : "Прицел";
                if (connected) {
                    context.getPlayer().displayClientMessage(
                            Component.literal("§a" + name + " подключён к модулю оптики и записан для связи с ПТУР!"),
                            true);
                } else {
                    context.getPlayer().displayClientMessage(
                            Component.literal("§a" + name + " записан [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"),
                            true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
