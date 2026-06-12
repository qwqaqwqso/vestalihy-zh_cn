package com.vestalihy.entity;

import javax.annotation.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.base.PartialBlockDamageManager;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.index.CBCDamageTypes;
import rbasamoyai.createbigcannons.munitions.CannonDamageSource;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelBurst;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.fragment_burst.ProjectileBurstProperties;
import rbasamoyai.ritchiesprojectilelib.projectile_burst.ProjectileBurst;

/**
 * Кумулятивная струя ПТУР.
 *
 * Работает через стандартную систему пробития CBC (PartialBlockDamageManager),
 * используя жестко заданные характеристики в getProperties(), чтобы избежать
 * проблем с порядком загрузки JSON-конфигураций.
 *
 * Глубина пробития = количество суб-снарядов / 10 (так как блоку нужно 10 попаданий для разрушения).
 * При count = 70 в PturEntity.detonate(), струя пробивает около 7 блоков.
 */
public class PturJetEntity extends ShrapnelBurst {
    private static final Logger LOGGER = LogUtils.getLogger();

    public PturJetEntity(EntityType<? extends PturJetEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected DamageSource getDamageSource() {
        return new CannonDamageSource(
            CannonDamageSource.getDamageRegistry(this.level())
                .getHolderOrThrow(CBCDamageTypes.MOLTEN_METAL),
            true // ignores entity armor — кумулятивная струя
        );
    }

    @Nullable
    @Override
    public ParticleOptions getTrailParticle() {
        return ParticleTypes.SMALL_FLAME;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            LOGGER.info("[PturJetEntity-Debug] Tick: active sub-projectiles = {}, position = {}, entityId = {}, age = {}", 
                this.subProjectiles.size(), this.position(), this.getId(), this.tickCount);
        }
        super.tick();
    }

    @Override
    protected void onSubProjectileHitBlock(BlockHitResult result, ProjectileBurst.SubProjectile subProjectile) {
        if (this.isRemoved()) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockState state = this.level().getBlockState(pos);

        // Эквивалент super.super.onSubProjectileHitBlock(result, subProjectile)
        state.onProjectileHit(this.level(), state, result, this);

        if (this.isRemoved()) {
            return;
        }

        // Проверка динамической защиты (ДЗ)
        String blockClassName = state.getBlock().getClass().getName();
        if (blockClassName.equals("com.dokto.create_armored_constructs.block.Kontakt1PlatformBlock")) {
            if (!this.level().isClientSide) {
                try {
                    Class<?> blockClass = state.getBlock().getClass();
                    java.lang.reflect.Field slotsField = blockClass.getField("SLOTS");
                    net.minecraft.world.level.block.state.properties.BooleanProperty[] slots = 
                        (net.minecraft.world.level.block.state.properties.BooleanProperty[]) slotsField.get(null);
                    
                    boolean hasActive = false;
                    for (net.minecraft.world.level.block.state.properties.BooleanProperty slot : slots) {
                        if (state.getValue(slot)) {
                            hasActive = true;
                            break;
                        }
                    }
                    
                    if (hasActive) {
                        Class<?> handlerClass = Class.forName("com.dokto.create_armored_constructs.KontaktExplosionHandler");
                        java.lang.reflect.Method consumeMethod = handlerClass.getDeclaredMethod("consumeOneSlot", 
                            Level.class, BlockPos.class, BlockState.class);
                        consumeMethod.setAccessible(true);
                        consumeMethod.invoke(null, this.level(), pos, state);
                        
                        this.discard();
                        LOGGER.info("[PturJetEntity-Debug] Hit active Kontakt-1 block at {}. Slot consumed. Stopping jet stream.", pos);
                        return;
                    } else {
                        LOGGER.info("[PturJetEntity-Debug] Hit empty Kontakt-1 block at {}. Proceeding.", pos);
                    }
                } catch (Exception e) {
                    LOGGER.error("[PturJetEntity-Debug] Error handling Kontakt-1 block", e);
                }
            }
        }

        if (blockClassName.equals("riftyboi.cbcmodernwarfare.content.reactive.ERABlock") ||
            blockClassName.equals("riftyboi.cbcmodernwarfare.content.reactive.ERASlabBlock") ||
            blockClassName.equals("riftyboi.cbcmodernwarfare.content.reactive.ERAStairBlock") ||
            blockClassName.equals("riftyboi.cbcmodernwarfare.content.reactive.ERAVerticalSlabBlock")) {
            
            if (!this.level().isClientSide) {
                try {
                    this.discard();
                    this.level().removeBlock(pos, false);
                    
                    rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion explosion = null;
                    try {
                        explosion = rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion.class.getConstructor(
                            Level.class, net.minecraft.world.entity.Entity.class, DamageSource.class,
                            double.class, double.class, double.class, float.class, float.class, net.minecraft.world.level.Explosion.BlockInteraction.class
                        ).newInstance(
                            this.level(), null, null, pos.getX(), pos.getY(), pos.getZ(), 2.0f, 2.0f, net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                    } catch (Exception e) {
                        explosion = new rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion(
                            this.level(), null, null, pos.getX(), pos.getY(), pos.getZ(), 2.0f, 
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                    }
                    rbasamoyai.createbigcannons.CreateBigCannons.handleCustomExplosion(this.level(), explosion);
                    
                    SoundType soundType = state.getSoundType();
                    this.level().playSound(null, pos, soundType.getBreakSound(), SoundSource.NEUTRAL, soundType.getVolume() * 0.25f, soundType.getPitch());
                    
                    LOGGER.info("[PturJetEntity-Debug] Hit ERABlock at {}. Detonated. Stopping jet stream.", pos);
                    return;
                } catch (Exception e) {
                    LOGGER.error("[PturJetEntity-Debug] Error handling ERABlock", e);
                }
            }
        }

        if (!this.level().isClientSide) {
            double destroySpeed = state.getDestroySpeed(this.level(), pos);
            if (destroySpeed != -1 && this.canDestroyBlock(state)) {
                Vec3 curVel = new Vec3(subProjectile.velocity()[0], subProjectile.velocity()[1], subProjectile.velocity()[2]);
                double curPom = this.getProperties().ballistics().durabilityMass() * curVel.length();
                double toughness = BlockArmorPropertiesHandler.getProperties(state).toughness(this.level(), state, pos, true);
                
                // Исправление бага с приведением к int для низкой прочности (грязь и т.д.):
                // Гарантируем, что урон будет как минимум 1, если toughness > 0
                double rawDamage = Math.min(curPom, toughness);
                int damage = (int) Math.ceil(rawDamage);
                if (damage <= 0 && rawDamage > 0) {
                    damage = 1;
                }
                if (toughness <= 0) {
                    // Если прочность 0 или отрицательная, даем ей дефолтную прочность (например, 1), 
                    // чтобы блок можно было пробить, и урон был > 0.
                    toughness = 1.0;
                    damage = 1;
                }

                LOGGER.info("[PturJetEntity-Debug] Hit block at: {}, block: {}, velocity: [{}, {}, {}], curPom: {}, toughness: {}, damageAdded: {}", 
                    pos, state.toString(), curVel.x, curVel.y, curVel.z, curPom, toughness, damage);

                // Наносим урон блоку через рефлексию для совместимости типов аргументов
                java.util.function.BiConsumer<Level, BlockPos> onDestroy = PartialBlockDamageManager::voidBlock;
                try {
                    java.lang.reflect.Method damageMethod = null;
                    for (java.lang.reflect.Method m : CreateBigCannons.BLOCK_DAMAGE.getClass().getMethods()) {
                        if (m.getName().equals("damageBlock") && m.getParameterCount() == 5) {
                            damageMethod = m;
                            break;
                        }
                    }
                    if (damageMethod != null) {
                        Class<?> secondParamType = damageMethod.getParameterTypes()[1];
                        Object damageArg;
                        if (secondParamType == float.class || secondParamType == Float.class) {
                            damageArg = (float) damage;
                        } else if (secondParamType == double.class || secondParamType == Double.class) {
                            damageArg = (double) damage;
                        } else {
                            damageArg = damage;
                        }
                        damageMethod.invoke(CreateBigCannons.BLOCK_DAMAGE, pos.immutable(), damageArg, state, this.level(), onDestroy);
                    } else {
                        LOGGER.error("[PturJetEntity-Debug] Could not find damageBlock method via reflection!");
                    }
                } catch (Exception e) {
                    LOGGER.error("[PturJetEntity-Debug] Error invoking damageBlock via reflection", e);
                }
            }

            if (this.level() instanceof ServerLevel slevel) {
                ParticleOptions options = new BlockParticleOption(ParticleTypes.BLOCK, state);
                for (ServerPlayer player : slevel.players()) {
                    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 1024d) {
                        slevel.sendParticles(player, options, true, pos.getX(), pos.getY(), pos.getZ(), 20, 0.4, 2, 0.4, 1);
                    }
                }
            }
            SoundType type = state.getSoundType();
            this.level().playLocalSound(pos.getX(), pos.getY(), pos.getZ(), type.getBreakSound(), SoundSource.NEUTRAL,
                type.getVolume() * 2, type.getPitch(), false);
        }
    }

    @Override
    protected void onSubProjectileHitEntity(EntityHitResult result, ProjectileBurst.SubProjectile subProjectile) {
        if (this.isRemoved()) {
            return;
        }
        if (!this.level().isClientSide) {
            LOGGER.info("[PturJetEntity-Debug] Hit entity: {}, type: {}, location: {}", 
                result.getEntity(), result.getEntity().getType(), result.getLocation());
        }
        super.onSubProjectileHitEntity(result, subProjectile);
    }

    @Override
    protected ProjectileBurstProperties getProperties() {
        return new ProjectileBurstProperties(
            new BallisticPropertiesComponent(
                -0.01,  // gravity
                0.02,   // drag
                false,  // isQuadraticDrag
                100.0f, // durabilityMass — высокая пробивная способность кумулятивной струи
                0.0f,   // penetration
                0.0f,   // toughness
                0.0f    // deflection
            ),
            new EntityDamagePropertiesComponent(
                40.0f,  // entityDamage — урон по сущностям
                false,  // rendersInvulnerable
                true,   // ignoresInvulnerability
                true,   // ignoresEntityArmor
                0.0f    // knockback
            ),
            7 // lifetime
        );
    }
}
