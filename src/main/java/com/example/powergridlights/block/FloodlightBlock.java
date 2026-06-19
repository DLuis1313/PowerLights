package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

/**
 * Refletor elétrico direcional.
 *
 * Estende DirectionalElectricBlock que:
 *  - Fornece a propriedade FACING
 *  - Gera o BlockStateTerminalCollection rotacionado automaticamente
 *  - Não estende BaseEntityBlock → não precisa de codec()
 *
 * directionalNorthTerminals(Block, TerminalBoundingBox[], VoxelShape) → 3 args.
 * A VoxelShape é usada para TODAS as direções (o helper cria variantes rotacionadas).
 */
public class FloodlightBlock extends DirectionalElectricBlock implements IBE<FloodlightBlockEntity> {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // Plugs do modelo (coordenadas do Blockbench / 16)
    // Plug L (esquerdo): from [4,1,10] to [6,2,12]
    private static final TerminalBoundingBox TERM_L = new TerminalBoundingBox(
            Component.literal("L"),
            4/16.0, 1/16.0, 10/16.0,
            6/16.0, 2/16.0, 12/16.0);

    // Plug N (direito): from [10,1,10] to [12,2,12]
    private static final TerminalBoundingBox TERM_N = new TerminalBoundingBox(
            Component.literal("N"),
            10/16.0, 1/16.0, 10/16.0,
            12/16.0, 2/16.0, 12/16.0);

    // Shape do modelo (caixa geral, válida para NORTH)
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public FloodlightBlock(Properties properties) {
        super(properties);

        // 3 args: block, terminais[], shape base (o helper rotaciona para todos os facings)
        setTerminalCollection(
            DirectionalElectricBlock.directionalNorthTerminals(
                this,
                new TerminalBoundingBox[]{ TERM_L, TERM_N },
                SHAPE
            ).build()
        );

        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adiciona FACING
        builder.add(LIT);
    }

    // ── IBE ──────────────────────────────────────────────────────────────────

    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get();
    }

    // ── Limpeza de luzes ao remover o bloco ──────────────────────────────────
    // (setRemoved() é final em SmartBlockEntity → usamos onRemove() no bloco)

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel sl
                && level.getBlockEntity(pos) instanceof FloodlightBlockEntity be) {
            be.removeLights(sl);
            setLit(sl, pos, state, false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    public static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LIT) != lit)
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
    }

    @Override
    public int getLightEmission(BlockState state,
                                net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 4 : 0;
    }
}
