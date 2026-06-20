package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.patryk3211.powergrid.block.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.blockentity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.blockentity.base.BlockStateTerminalCollection;
import net.minecraft.core.component.DataComponentMap;

public class FloodlightBlock extends DirectionalElectricBlock implements IBE<FloodlightBlockEntity> {

    // Shape do bloco (caixa principal do refletor)
    private static final VoxelShape SHAPE_NORTH = Block.box(1, 1, 4, 15, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1, 1, 0, 15, 15, 12);
    private static final VoxelShape SHAPE_EAST  = Block.box(0, 1, 1, 12, 15, 15);
    private static final VoxelShape SHAPE_WEST  = Block.box(4, 1, 1, 16, 15, 15);
    private static final VoxelShape SHAPE_UP    = Block.box(1, 0, 1, 15, 12, 15);
    private static final VoxelShape SHAPE_DOWN  = Block.box(1, 4, 1, 15, 16, 15);

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.FACING, net.minecraft.core.Direction.NORTH));

        // Terminais: plug esquerdo e plug direito do modelo Blockbench
        // Cubo esquerdo:  from [4,1,10] to [6,2,12]  → /16
        // Cubo direito:   from [10,1,10] to [12,2,12] → /16
        TerminalBoundingBox termL = new TerminalBoundingBox(
                net.minecraft.network.chat.Component.literal("Line"),
                4f/16, 1f/16, 10f/16,
                6f/16, 2f/16, 12f/16
        );
        TerminalBoundingBox termN = new TerminalBoundingBox(
                net.minecraft.network.chat.Component.literal("Neutral"),
                10f/16, 1f/16, 10f/16,
                12f/16, 2f/16, 12f/16
        );

        TerminalBoundingBox[] terminals = { termL, termN };

        // BlockStateTerminalCollection NÃO tem .build() — é construída direto
        BlockStateTerminalCollection collection =
                directionalNorthTerminals(this, terminals, SHAPE_NORTH);

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

    // Limpa os blocos de luz quando o refletor é removido
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

    // IBE
    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT.get();
    }
}
