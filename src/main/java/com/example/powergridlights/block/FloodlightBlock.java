package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class FloodlightBlock extends DirectionalElectricBlock implements IBE<FloodlightBlockEntity> {

    // Propriedade de estado: ligado/desligado
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // Shape base apontando para NORTE (bloco vai de Z=4 a Z=16)
    private static final VoxelShape SHAPE_NORTH = Block.box(1, 0, 4, 15, 10, 16);

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(LIT, false));

        // Terminais: plugues na traseira, espaçados horizontalmente
        // Coordenadas em unidades de bloco (0.0 a 1.0), orientação NORTH
        // Line (esquerdo): X de 4/16 a 6/16, Y de 1/16 a 3/16, Z de 12/16 a 14/16
        TerminalBoundingBox termLine = new TerminalBoundingBox(
                IDecoratedTerminal.POSITIVE,
                4d/16, 1d/16, 12d/16,
                6d/16, 3d/16, 14d/16
        );
        // Neutral (direito): X de 10/16 a 12/16
        TerminalBoundingBox termNeutral = new TerminalBoundingBox(
                IDecoratedTerminal.NEGATIVE,
                10d/16, 1d/16, 12d/16,
                12d/16, 3d/16, 14d/16
        );

        // Usar o builder como o LightFixtureBlock faz
        VoxelShaper shaper = VoxelShaper.forDirectional(SHAPE_NORTH, Direction.NORTH);

        BlockStateTerminalCollection collection =
                BlockStateTerminalCollection.builder(this)
                        .forAllStatesExcept(state -> new TerminalBoundingBox[]{termLine, termNeutral}, LIT)
                        .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                        .build();

        setTerminalCollection(collection);
    }

    // DirectionalElectricBlock já adiciona FACING — só adicionamos LIT
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite())
                .setValue(LIT, false);
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

    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT.get();
    }
}
