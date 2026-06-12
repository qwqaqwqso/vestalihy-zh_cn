package com.vestalihy.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class OpticsModuleBlock extends Block implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected static final VoxelShape SHAPE_NORTH = Block.box(5, 4, 4, 11, 10, 12);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(5, 4, 4, 11, 10, 12); // Will be rotated
    protected static final VoxelShape SHAPE_EAST = Block.box(4, 4, 5, 12, 10, 11);
    protected static final VoxelShape SHAPE_WEST = Block.box(4, 4, 5, 12, 10, 11);

    public OpticsModuleBlock(Properties properties) {
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
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape baseShape = switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(5, 4, 4, 11, 10, 12);
            case EAST -> Block.box(4, 4, 5, 12, 10, 11);
            case WEST -> Block.box(4, 4, 5, 12, 10, 11);
            default -> SHAPE_NORTH;
        };

        if (level.getBlockEntity(pos) instanceof OpticsModuleBlockEntity optics) {
            float x = optics.getOffsetX();
            float y = optics.getOffsetY();
            float z = optics.getOffsetZ();
            if (x != 0 || y != 0 || z != 0) {
                return baseShape.move(x, y, z);
            }
        }

        return baseShape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OpticsModuleBlockEntity(pos, state);
    }
}
