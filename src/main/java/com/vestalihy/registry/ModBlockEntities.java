package com.vestalihy.registry;

import com.vestalihy.Vestalihy;
import com.vestalihy.block.AimingBlockEntity;
import com.vestalihy.block.OpticsModuleBlockEntity;
import com.vestalihy.block.CommanderScopeBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, Vestalihy.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AimingBlockEntity>> AIMING_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("aiming_block",
                    () -> BlockEntityType.Builder.of(AimingBlockEntity::new, 
                            ModBlocks.AIMING_BLOCK_USSR.get(),
                            ModBlocks.AIMING_BLOCK_CHINA.get(),
                            ModBlocks.AIMING_BLOCK_NATO.get(),
                            ModBlocks.AIMING_BLOCK_BTR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OpticsModuleBlockEntity>> OPTICS_MODULE_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("optics_module",
                    () -> BlockEntityType.Builder.of(OpticsModuleBlockEntity::new,
                            ModBlocks.OPTICS_MODULE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CommanderScopeBlockEntity>> COMMANDER_SCOPE_BLOCK_ENTITY = BLOCK_ENTITIES
            .register("commander_scope",
                    () -> BlockEntityType.Builder.of(CommanderScopeBlockEntity::new,
                            ModBlocks.COMMANDER_SCOPE.get())
                            .build(null));
}
