package com.vestalihy.network;

import com.vestalihy.Vestalihy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Vestalihy.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    public static final ResourceLocation AIMING_ID = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "aiming");
    public static final ResourceLocation OPTICS_OFFSET_ID = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "optics_offset");
    public static final ResourceLocation OPTICS_ROTATION_ID = ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "optics_rotation");

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Vestalihy.MODID).versioned("1");

        registrar.playToServer(
                AimingPacket.TYPE,
                AimingPacket.STREAM_CODEC,
                AimingPacket::handle
        );

        registrar.playToServer(
                OpticsOffsetPacket.TYPE,
                OpticsOffsetPacket.STREAM_CODEC,
                OpticsOffsetPacket::handle
        );

        registrar.playToServer(
                OpticsRotationPacket.TYPE,
                OpticsRotationPacket.STREAM_CODEC,
                OpticsRotationPacket::handle
        );
    }
}
