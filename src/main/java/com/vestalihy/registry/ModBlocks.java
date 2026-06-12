package com.vestalihy.registry;

import com.vestalihy.Vestalihy;
import com.vestalihy.block.AimingBlock;
import com.vestalihy.block.OpticsModuleBlock;
import com.vestalihy.block.ScopeType;
import com.vestalihy.block.CommanderScopeBlock;
import com.vestalihy.block.PturControllerBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Vestalihy.MODID);

        public static final DeferredBlock<Block> AIMING_BLOCK_USSR = BLOCKS.register("aiming_block_ussr",
                        () -> new AimingBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(3.5f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops(), ScopeType.USSR));

        public static final DeferredBlock<Block> AIMING_BLOCK_CHINA = BLOCKS.register("aiming_block_china",
                        () -> new AimingBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(3.5f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops(), ScopeType.CHINA));

        public static final DeferredBlock<Block> AIMING_BLOCK_NATO = BLOCKS.register("aiming_block_nato",
                        () -> new AimingBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(3.5f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops(), ScopeType.NATO));

        public static final DeferredBlock<Block> AIMING_BLOCK_BTR = BLOCKS.register("aiming_block_btr",
                        () -> new AimingBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.METAL)
                                        .strength(3.5f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops(), ScopeType.BTR));

        public static final DeferredBlock<Block> OPTICS_MODULE = BLOCKS.register("optics_module",
                        () -> new OpticsModuleBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.STONE)
                                        .strength(3.0f)
                                        .sound(SoundType.STONE)
                                        .requiresCorrectToolForDrops()));

        public static final DeferredBlock<Block> COMMANDER_SCOPE = BLOCKS.register("commander_scope",
                        () -> new CommanderScopeBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.GOLD)
                                        .strength(3.0f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops()));

        public static final DeferredBlock<Block> PTUR_CONTROLLER = BLOCKS.register("ptur_controller",
                        () -> new PturControllerBlock(BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.DIAMOND)
                                        .strength(3.0f)
                                        .sound(SoundType.METAL)
                                        .requiresCorrectToolForDrops()));

        // Deprecated - use AIMING_BLOCK_USSR instead
        @Deprecated
        public static final DeferredBlock<Block> AIMING_BLOCK = AIMING_BLOCK_USSR;
}
