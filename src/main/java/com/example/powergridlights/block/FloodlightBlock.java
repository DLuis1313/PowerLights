package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class FloodlightBlock extends DirectionalElectricBlock implements IBE<FloodlightBlockEntity> {

    // Shape base apontando para NORTE
    private static final VoxelShape SHAPE_NORTH = Block.box(1, 1, 4, 15, 15, 16);

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));

        // TerminalBoundingBox(Component, double x1, y1, z1, double x2, y2, z2)
        // Plugues na traseira do refletor (Z≈10-12/16 quando facing=NORTH)
        TerminalBoundingBox termLine = new TerminalBoundingBox(
                IDecoratedTerminal.POSITIVE,
                4d/16, 1d/16, 10d/16,
                6d/16, 3d/16, 12d/16
        );
        TerminalBoundingBox termNeutral = new TerminalBoundingBox(
                IDecoratedTerminal.NEGATIVE,
                10d/16, 1d/16, 10d/16,
                12d/16, 3d/16, 12d/16
        );

        // directionalNorthTerminals retorna BlockStateTerminalCollection diretamente (sem .build())
        BlockStateTerminalCollection collection =
                directionalNorthTerminals(this, new TerminalBoundingBox[]{termLine, termNeutral}, SHAPE_NORTH);

        setTerminalCollection(collection);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(BlockStateProperties.FACING,
                          ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FloodlightBlockEntity be) {
                be.removeLights();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // --- IBE ---

    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT.get();
    }
}
