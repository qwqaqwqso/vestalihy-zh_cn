package com.vestalihy.registry;

import com.vestalihy.Vestalihy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(Registries.SOUND_EVENT, Vestalihy.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> THERMAL_ON = SOUND_EVENTS.register("thermal_on",
            () -> SoundEvent
                    .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "thermal_on")));
    public static final DeferredHolder<SoundEvent, SoundEvent> THERMAL_OFF = SOUND_EVENTS.register("thermal_off",
            () -> SoundEvent
                    .createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "thermal_off")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FONTS = SOUND_EVENTS.register("fonts",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "fonts")));
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PTUR = SOUND_EVENTS.register("fire_ptur",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vestalihy.MODID, "fire_ptur")));
}
