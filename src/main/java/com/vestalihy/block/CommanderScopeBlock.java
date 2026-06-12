package com.vestalihy.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.vestalihy.client.AimingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CommanderScopeBlock extends Block implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(5.5, 0, 0, 10.5, 3, 3), // Base
            Block.box(2.5, 3, 0, 13.5, 15, 3)); // Main Body

    protected static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(5.5, 0, 13, 10.5, 3, 16), // Base
            Block.box(2.5, 3, 13, 13.5, 15, 16)); // Main Body

    protected static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(13, 0, 5.5, 16, 3, 10.5),
            Block.box(13, 3, 2.5, 16, 15, 13.5));

    protected static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(0, 0, 5.5, 3, 3, 10.5),
            Block.box(0, 3, 2.5, 3, 15, 13.5));

    public CommanderScopeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommanderScopeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos opticsModulePos = null;
        BlockEntity be = com.vestalihy.compat.SableHelper.isAvailable()
                ? com.vestalihy.compat.SableHelper.getBlockEntitySafely(level, pos)
                : level.getBlockEntity(pos);
        if (be instanceof CommanderScopeBlockEntity scopeBE) {
            opticsModulePos = scopeBE.getOpticsModulePos();
            if (opticsModulePos != null) {
                net.minecraft.world.level.block.state.BlockState opticsState = com.vestalihy.compat.SableHelper.isAvailable()
                        ? com.vestalihy.compat.SableHelper.getBlockStateSafely(level, opticsModulePos)
                        : level.getBlockState(opticsModulePos);
                if (!(opticsState.getBlock() instanceof OpticsModuleBlock)) {
                    opticsModulePos = null;
                    scopeBE.setOpticsModulePos(null);
                }
            }
        }

        if (level.isClientSide) {
            Direction facing = state.getValue(FACING);
            AimingHandler.startAiming(pos, null, facing, hit.getLocation(), ScopeType.COMMANDER, opticsModulePos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.CONSUME;
    }
}
