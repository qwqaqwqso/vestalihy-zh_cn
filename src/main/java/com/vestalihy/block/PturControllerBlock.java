package com.vestalihy.block;

import com.vestalihy.entity.TubusEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PturControllerBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public PturControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        boolean hasSignal = level.hasNeighborSignal(pos);
        boolean isPowered = state.getValue(POWERED);

        if (hasSignal && !isPowered) {
            // Rising edge: trigger launch and set powered to true
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            triggerLaunch(level, pos);
        } else if (!hasSignal && isPowered) {
            // Falling edge: set powered to false
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }

    private void triggerLaunch(Level level, BlockPos pos) {
        // Scan 5-block radius bounding box for TubusEntities
        AABB area = new AABB(pos).inflate(5.0);
        List<TubusEntity> launchers = level.getEntitiesOfClass(TubusEntity.class, area);

        for (TubusEntity tubus : launchers) {
            // Check that it's within actual spherical or block distance (5 blocks)
            if (tubus.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 25.0) {
                if (tubus.launch()) {
                    // Only launch ONE per redstone activation
                    break;
                }
            }
        }
    }
}
