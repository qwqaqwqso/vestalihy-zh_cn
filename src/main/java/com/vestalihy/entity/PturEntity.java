package com.vestalihy.entity;

import com.vestalihy.block.AimingBlock;
import com.vestalihy.block.AimingBlockEntity;
import com.vestalihy.block.CommanderScopeBlock;
import com.vestalihy.block.CommanderScopeBlockEntity;
import com.vestalihy.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.fragment_burst.CBCProjectileBurst;

import javax.annotation.Nullable;
import java.util.List;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class PturEntity extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private BlockPos commanderScopePos = null;

    public PturEntity(EntityType<? extends PturEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Nullable
    public BlockPos getCommanderScopePos() {
        return commanderScopePos;
    }

    public void setCommanderScopePos(@Nullable BlockPos pos) {
        this.commanderScopePos = pos;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 262144.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
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
        if (this.commanderScopePos != null) {
            tag.putInt("scope_x", this.commanderScopePos.getX());
            tag.putInt("scope_y", this.commanderScopePos.getY());
            tag.putInt("scope_z", this.commanderScopePos.getZ());
        }
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();

        if (!this.level().isClientSide) {
            guideMissile();
            vel = this.getDeltaMovement();

            if (this.tickCount > 160) {
                this.detonate(pos);
                return;
            }

            Vec3 nextPos = pos.add(vel);
            HitResult hitResult = this.level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.detonate(hitResult.getLocation());
                return;
            }

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    this.level(),
                    this,
                    pos,
                    nextPos,
                    this.getBoundingBox().expandTowards(vel).inflate(1.0D),
                    this::canHitEntity
            );
            if (entityHit != null) {
                this.detonate(entityHit.getLocation());
                return;
            }
        } else {
            double speed = vel.length();
            double maxSpeed = 1.6;
            if (speed < maxSpeed) {
                speed = Math.min(maxSpeed, speed + 0.08);
            }
            Vec3 dir = this.getLookAngle();
            vel = dir.scale(speed);
            this.setDeltaMovement(vel);

            if (vel.lengthSqr() > 0.001) {
                Vec3 back = pos.subtract(vel.normalize().scale(0.5));
                this.level().addParticle(ParticleTypes.FLAME,
                        back.x, back.y, back.z,
                        -vel.x * 0.1, -vel.y * 0.1, -vel.z * 0.1);
                this.level().addParticle(ParticleTypes.SMOKE,
                        back.x, back.y, back.z,
                        -vel.x * 0.2, -vel.y * 0.2, -vel.z * 0.2);
            }
        }

        this.move(MoverType.SELF, vel);
        this.setPos(this.getX(), this.getY(), this.getZ());
    }

    private void guideMissile() {
        Vec3 pturPos = this.position();
        Vec3 pturVel = this.getDeltaMovement();
        double speed = pturVel.length();

        double maxSpeed = 1.6;
        if (speed < maxSpeed) {
            speed = Math.min(maxSpeed, speed + 0.08);
        }

        boolean guided = false;

        if (this.commanderScopePos != null) {
            net.minecraft.world.level.block.state.BlockState scopeState = com.vestalihy.compat.SableHelper.isAvailable()
                    ? com.vestalihy.compat.SableHelper.getBlockStateSafely(this.level(), this.commanderScopePos)
                    : this.level().getBlockState(this.commanderScopePos);
            net.minecraft.world.level.block.entity.BlockEntity scopeBe = com.vestalihy.compat.SableHelper.isAvailable()
                    ? com.vestalihy.compat.SableHelper.getBlockEntitySafely(this.level(), this.commanderScopePos)
                    : this.level().getBlockEntity(this.commanderScopePos);
            
            ServerPlayer operator = null;
            BlockPos opticsPos = null;
            
            if (scopeState.getBlock() instanceof CommanderScopeBlock && scopeBe instanceof CommanderScopeBlockEntity commanderScope) {
                operator = commanderScope.getOperator(this.level());
                opticsPos = commanderScope.getOpticsModulePos();
            } else if (scopeState.getBlock() instanceof AimingBlock && scopeBe instanceof AimingBlockEntity aimingScope) {
                operator = aimingScope.getOperator(this.level());
                opticsPos = aimingScope.getOpticsModulePos();
            }

            if (operator != null) {
                BlockPos eyeBlockPos = opticsPos != null ? opticsPos : this.commanderScopePos;
                Vec3 camPos = Vec3.atCenterOf(eyeBlockPos);
                // Construct look direction from body rotation (world space) instead of head rotation to avoid passenger desync
                float pitch = operator.getXRot();
                float yaw = operator.getYRot();
                float f = pitch * ((float)Math.PI / 180F);
                float f1 = -yaw * ((float)Math.PI / 180F);
                float f2 = net.minecraft.util.Mth.cos(f1);
                float f3 = net.minecraft.util.Mth.sin(f1);
                float f4 = net.minecraft.util.Mth.cos(f);
                float f5 = net.minecraft.util.Mth.sin(f);
                Vec3 camDir = new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));

                LOGGER.info("[Ptur-Debug] guideMissile: operator={} isPassenger={} operatorYaw={} operatorPitch={} camLookAngle={}",
                        operator.getName().getString(), operator.isPassenger(), operator.getYRot(), operator.getXRot(), camDir);
                LOGGER.info("[Ptur-Debug] guideMissile: pturPos={} pturVel={} eyeBlockPos={} camPos={}",
                        pturPos, pturVel, eyeBlockPos, camPos);

                if (com.vestalihy.compat.SableHelper.isAvailable()) {
                    Object scopeSubLevel = com.vestalihy.compat.SableHelper.getSubLevelManagingPos(this.level(), eyeBlockPos);
                    Object pturSubLevel = com.vestalihy.compat.SableHelper.getSubLevelManagingPos(this.level(), this.blockPosition());
                    LOGGER.info("[Ptur-Debug] guideMissile: scopeSubLevel={} pturSubLevel={}", scopeSubLevel, pturSubLevel);
                    if (scopeSubLevel != pturSubLevel && scopeSubLevel != null) {
                        Vec3 origCamPos = camPos;
                        camPos = com.vestalihy.compat.SableHelper.sublevelToWorld(scopeSubLevel, camPos);
                        LOGGER.info("[Ptur-Debug] guideMissile: transform due to sublevel difference. camPos {} -> {} | camDir {}",
                                origCamPos, camPos, camDir);
                    }
                }

                double distanceAlongLine = pturPos.subtract(camPos).dot(camDir);
                LOGGER.info("[Ptur-Debug] guideMissile: distanceAlongLine={}", distanceAlongLine);
                if (distanceAlongLine > 0) {
                    Vec3 targetAhead = camPos.add(camDir.scale(distanceAlongLine + 15.0));
                    Vec3 desiredDir = targetAhead.subtract(pturPos).normalize();

                    double steerFactor = 0.12;
                    Vec3 currentDir = pturVel.lengthSqr() > 0.001 ? pturVel.normalize() : desiredDir;
                    Vec3 newDir = currentDir.scale(1.0 - steerFactor).add(desiredDir.scale(steerFactor)).normalize();

                    LOGGER.info("[Ptur-Debug] guideMissile: targetAhead={} desiredDir={} steerFactor={} currentDir={} newDir={}",
                            targetAhead, desiredDir, steerFactor, currentDir, newDir);
                    this.setDeltaMovement(newDir.scale(speed));
                    updateRotationFromDir(newDir);
                    guided = true;
                }
            }
        }

        if (!guided && pturVel.lengthSqr() > 0.001) {
            Vec3 newVel = pturVel.normalize().scale(speed);
            this.setDeltaMovement(newVel);
            updateRotationFromDir(newVel.normalize());
        }
    }

    /**
     * Обновляет yaw/pitch entity по вектору направления движения.
     * MC yaw: 0=юг(+Z), 90=запад(-X), 180=север(-Z), -90=восток(+X)
     * Формула: yaw = atan2(-x, z), нос модели в +Z при yaw=0.
     * Рендер: mulPose(YP, -yRot) → при yaw=0 нет поворота → нос на юг ✓
     */
    private void updateRotationFromDir(Vec3 dir) {
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        
        yaw = net.minecraft.util.Mth.wrapDegrees(yaw);
        pitch = net.minecraft.util.Mth.wrapDegrees(pitch);

        LOGGER.info("[Ptur-Debug] updateRotationFromDir: dir={}, yaw={}, pitch={}", dir, yaw, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    private boolean canHitEntity(Entity entity) {
        if (entity.isSpectator() || !entity.isAlive() || !entity.isPickable()) {
            return false;
        }
        if (this.tickCount < 5) {
            if (entity instanceof TubusEntity) return false;
        }
        return true;
    }

    private void detonate(Vec3 hitPos) {
        if (!this.level().isClientSide) {
            // Вектор движения ракеты в момент удара — ось кумулятивной струи.
            Vec3 oldDelta = this.getDeltaMovement().normalize().scale(2.5);

            // Взрыв боевой части (формирование кумулятивной струи)
            rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion explosion = null;
            try {
                explosion = rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion.class.getConstructor(
                    net.minecraft.world.level.Level.class, net.minecraft.world.entity.Entity.class, net.minecraft.world.damagesource.DamageSource.class,
                    double.class, double.class, double.class, float.class, float.class, net.minecraft.world.level.Explosion.BlockInteraction.class
                ).newInstance(
                    this.level(), this, null, hitPos.x, hitPos.y, hitPos.z, 2.5f, 2.5f, net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                );
            } catch (Exception e) {
                explosion = new rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion(
                    this.level(), this, null, hitPos.x, hitPos.y, hitPos.z, 2.5f, 
                    net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                );
            }
            rbasamoyai.createbigcannons.CreateBigCannons.handleCustomExplosion(this.level(), explosion);

            LOGGER.info("[PturEntity-Debug] detonate called at hitPos={}, oldDelta={}, isClient={}", hitPos, oldDelta, this.level().isClientSide());

            // Кумулятивная струя — конус суб-снарядов вдоль оси полёта.
            // durabilityMass в ptur_jet.json очень высокий — струя пробивает любые блоки.
            // ДЗ (ERABlock) уничтожит субснаряды при контакте.
            CBCProjectileBurst.spawnConeBurst(
                this.level(),
                ModEntities.PTUR_JET.get(),
                hitPos,
                oldDelta,
                /* count */ 70,
                /* spread */ 0.01
            );

            this.discard();
        }
    }
}
