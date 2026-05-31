package com.example.powergridlights.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible, non-collidable block placed by {@link FloodlightBlockEntity}
 * to simulate a cone of light at a distance.  The light level decreases with
 * distance from the floodlight head.
 *
 * <p>Players cannot interact with this block; it is removed automatically
 * when the floodlight is turned off, broken, or unloaded.
 */
public class FloodlightLightBlock extends Block {

    /** Light level stored inside the block state (1-15). */
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public FloodlightLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            this.stateDefinition.any().setValue(LEVEL, 15)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    // Completely invisible
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // No collision box
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // No collision box (items/players pass through)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // Emit light according to stored level
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    // Transparent / doesn't block the sky
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }
}
