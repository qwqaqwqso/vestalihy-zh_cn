package com.vestalihy.mixin;

import com.vestalihy.entity.PturJetEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ERABlock.onProjectileHit() проверяет только instanceof HEAPBurst.
 * Этот миксин перехватывает вызов ДО оригинальной проверки и если прилетел
 * PturJetEntity — временно подменяем логику через kill субснаряда.
 *
 * Проще: если снаряд — PturJet, делаем kill() и пускаем оригинальный код дальше.
 * Но оригинальный код всё равно проверит instanceof HEAPBurst и не сработает —
 * поэтому нам нужно выполнить всю ERA логику самим, так же как делает ERABlock для HEAPBurst.
 *
 * Самый чистый вариант: inject HEAD + cancel, вызываем оригинальный код через
 * "притворяемся что это HEAPBurst" — невозможно без каста.
 *
 * Итог: inject HEAD, если PturJet — kill() субснаряда, затем вызываем super.onProjectileHit
 * (который сделает звук и прочее от Block.onProjectileHit), блок удаляем вручную.
 */
@Mixin(targets = "riftyboi.cbcmodernwarfare.content.reactive.ERABlock", remap = false)
public class MixinERABlock {

    @Inject(
        method = "onProjectileHit",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void vestalihy$killPturJet(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile,
            CallbackInfo ci) {

        // Только серверная сторона, только наша кумулятивная струя
        if (!level.isClientSide && projectile instanceof PturJetEntity) {
            net.minecraft.core.BlockPos pos = hit.getBlockPos();
            projectile.discard();
            level.removeBlock(pos, false);
            
            // Взрыв ДЗ
            rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion explosion = null;
            try {
                explosion = rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion.class.getConstructor(
                    net.minecraft.world.level.Level.class, net.minecraft.world.entity.Entity.class, net.minecraft.world.damagesource.DamageSource.class,
                    double.class, double.class, double.class, float.class, float.class, net.minecraft.world.level.Explosion.BlockInteraction.class
                ).newInstance(
                    level, null, null, pos.getX(), pos.getY(), pos.getZ(), 2.0f, 2.0f, net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                );
            } catch (Exception e) {
                explosion = new rbasamoyai.createbigcannons.munitions.autocannon.flak.FlakExplosion(
                    level, null, null, pos.getX(), pos.getY(), pos.getZ(), 2.0f, 
                    net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                );
            }
            rbasamoyai.createbigcannons.CreateBigCannons.handleCustomExplosion(level, explosion);
            
            // Звук уничтожения блока
            net.minecraft.world.level.block.SoundType type = state.getSoundType();
            level.playSound(null, pos, type.getBreakSound(), net.minecraft.sounds.SoundSource.NEUTRAL, type.getVolume() * 0.25f, type.getPitch());

            ci.cancel();
        }
    }
}
