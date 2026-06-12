package com.vestalihy.registry;

import com.vestalihy.Vestalihy;
import com.vestalihy.item.OpticsConnectorItem;
import com.vestalihy.item.TubusItem;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Vestalihy.MODID);

        public static final DeferredItem<Item> AIMING_BLOCK_USSR_ITEM = ITEMS.register("aiming_block_ussr",
                        () -> new BlockItem(ModBlocks.AIMING_BLOCK_USSR.get(), new Item.Properties()));

        public static final DeferredItem<Item> AIMING_BLOCK_CHINA_ITEM = ITEMS.register("aiming_block_china",
                        () -> new BlockItem(ModBlocks.AIMING_BLOCK_CHINA.get(), new Item.Properties()));

        public static final DeferredItem<Item> AIMING_BLOCK_NATO_ITEM = ITEMS.register("aiming_block_nato",
                        () -> new BlockItem(ModBlocks.AIMING_BLOCK_NATO.get(), new Item.Properties()));

        public static final DeferredItem<Item> AIMING_BLOCK_BTR_ITEM = ITEMS.register("aiming_block_btr",
                        () -> new BlockItem(ModBlocks.AIMING_BLOCK_BTR.get(), new Item.Properties()));

        public static final DeferredItem<Item> EMR_ITEM = ITEMS.register("emr",
                        () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                                        .nutrition(3)
                                        .saturationModifier(0.3f)
                                        .build())));

        public static final DeferredItem<Item> OPTICS_MODULE_ITEM = ITEMS.register("optics_module",
                        () -> new BlockItem(ModBlocks.OPTICS_MODULE.get(), new Item.Properties()));

        public static final DeferredItem<Item> OPTICS_CONNECTOR_ITEM = ITEMS.register("optics_connector",
                        () -> new OpticsConnectorItem(new Item.Properties().stacksTo(1)));

        public static final DeferredItem<Item> COMMANDER_SCOPE_ITEM = ITEMS.register("commander_scope",
                        () -> new BlockItem(ModBlocks.COMMANDER_SCOPE.get(), new Item.Properties()));

        public static final DeferredItem<Item> PTUR_CONTROLLER_ITEM = ITEMS.register("ptur_controller",
                        () -> new BlockItem(ModBlocks.PTUR_CONTROLLER.get(), new Item.Properties()));

        public static final DeferredItem<Item> TUBUS_ITEM = ITEMS.register("tubus",
                        () -> new TubusItem(new Item.Properties().stacksTo(16)));

        // Deprecated - use AIMING_BLOCK_USSR_ITEM instead
        @Deprecated
        public static final DeferredItem<Item> AIMING_BLOCK_ITEM = AIMING_BLOCK_USSR_ITEM;
}
