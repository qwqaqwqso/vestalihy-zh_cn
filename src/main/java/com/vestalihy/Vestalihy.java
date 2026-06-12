package com.vestalihy;

import com.mojang.logging.LogUtils;
import com.vestalihy.registry.ModBlockEntities;
import com.vestalihy.registry.ModBlocks;
import com.vestalihy.registry.ModEntities;
import com.vestalihy.registry.ModItems;
import com.vestalihy.registry.ModSounds;
import com.vestalihy.network.ModNetwork;
import com.vestalihy.client.AimingHandler;
import com.vestalihy.client.render.TubusRenderer;
import com.vestalihy.client.render.PturRenderer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelBurstRenderer;

import java.util.function.Supplier;

@Mod(Vestalihy.MODID)
public class Vestalihy {
    public static final String MODID = "vestalihy";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("vestalihy_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.AIMING_BLOCK_ITEM.get().getDefaultInstance())
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.vestalihy"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AIMING_BLOCK_USSR_ITEM.get());
                        output.accept(ModItems.AIMING_BLOCK_CHINA_ITEM.get());
                        output.accept(ModItems.AIMING_BLOCK_NATO_ITEM.get());
                        output.accept(ModItems.AIMING_BLOCK_BTR_ITEM.get());
                        output.accept(ModItems.OPTICS_MODULE_ITEM.get());
                        output.accept(ModItems.OPTICS_CONNECTOR_ITEM.get());
                        output.accept(ModItems.COMMANDER_SCOPE_ITEM.get());
                        output.accept(ModItems.PTUR_CONTROLLER_ITEM.get());
                        output.accept(ModItems.TUBUS_ITEM.get());
                    }).build());

    public Vestalihy(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Регистрируем ptur_jet в CBC системе свойств снарядов.
        // CBC читает data/vestalihy/munition_properties/projectiles/ptur_jet.json
        // и применяет настройки (урон, drag, lifetime, gravity).
        event.enqueueWork(() ->
            MunitionPropertiesHandler.registerProjectileHandler(
                ModEntities.PTUR_JET.get(),
                CBCMunitionPropertiesHandlers.PROJECTILE_BURST
            )
        );
        LOGGER.info("Vestalihy initialized");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            NeoForge.EVENT_BUS.register(AimingHandler.class);
            com.vestalihy.client.render.VeilCompat.registerPreprocessors();
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(com.vestalihy.client.Keybinds.THERMAL_VISION_KEY);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.OPTICS_MODULE_BLOCK_ENTITY.get(), com.vestalihy.client.render.OpticsModuleRenderer::new);
            event.registerEntityRenderer(ModEntities.TUBUS.get(), TubusRenderer::new);
            event.registerEntityRenderer(ModEntities.PTUR.get(), PturRenderer::new);
            // PturJetEntity использует ShrapnelBurstRenderer из CBC
            event.registerEntityRenderer(ModEntities.PTUR_JET.get(), ShrapnelBurstRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            // Hotbar and crosshair removal will be handled in AimingHandler via RenderGuiLayerEvent
        }
    }
}
