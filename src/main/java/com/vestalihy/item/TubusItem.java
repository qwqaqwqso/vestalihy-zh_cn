package com.vestalihy.item;

import com.vestalihy.entity.TubusEntity;
import com.vestalihy.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


public class TubusItem extends Item {

    public TubusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Vec3 clickLoc = context.getClickLocation();
        Direction horizontalFacing = context.getHorizontalDirection();
        Direction clickedFace = context.getClickedFace();

        double x = clickLoc.x;
        double y = clickLoc.y;
        double z = clickLoc.z;

        // Размеры хитбокса TubusEntity: 0.55 x 0.55
        double halfWidth = 0.55 / 2.0;
        double halfHeight = 0.55 / 2.0;

        if (clickedFace == Direction.UP) {
            // Клик сверху: низ хитбокса совпадает с y, ничего не меняем
        } else if (clickedFace == Direction.DOWN) {
            // Клик снизу: смещаем вниз на всю высоту
            y -= 0.55;
        } else {
            // Клик сбоку: смещаем наружу по нормали на половину ширины
            x += clickedFace.getNormal().getX() * halfWidth;
            z += clickedFace.getNormal().getZ() * halfWidth;
            // Центрируем хитбокс по вертикали относительно точки клика
            y -= halfHeight;
        }

        TubusEntity tubus = new TubusEntity(ModEntities.TUBUS.get(), level);
        tubus.moveTo(x, y, z, horizontalFacing.toYRot(), 0.0f);
        tubus.setFacing(horizontalFacing);

        level.addFreshEntity(tubus);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
