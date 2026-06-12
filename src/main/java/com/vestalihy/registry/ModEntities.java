package com.vestalihy.registry;

import com.vestalihy.Vestalihy;
import com.vestalihy.entity.PturEntity;
import com.vestalihy.entity.PturJetEntity;
import com.vestalihy.entity.TubusEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Vestalihy.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TubusEntity>> TUBUS = ENTITIES.register("tubus",
            () -> EntityType.Builder.<TubusEntity>of(TubusEntity::new, MobCategory.MISC)
                    .sized(0.55f, 0.55f)
                    .noSummon()
                    .build("tubus"));

    public static final DeferredHolder<EntityType<?>, EntityType<PturEntity>> PTUR = ENTITIES.register("ptur",
            () -> EntityType.Builder.<PturEntity>of(PturEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("ptur"));

    /**
     * Кумулятивная струя ПТУР. Аналог HEAP_BURST из CBC Modern Warfare.
     * Параметры (урон, drag, lifetime) в data/vestalihy/munition_properties/projectiles/ptur_jet.json
     */
    public static final DeferredHolder<EntityType<?>, EntityType<PturJetEntity>> PTUR_JET = ENTITIES.register("ptur_jet",
            () -> EntityType.Builder.<PturJetEntity>of(PturJetEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)   // тот же размер что у ShrapnelBurst в CBC
                    .fireImmune()
                    .noSummon()
                    .build("ptur_jet"));
}
