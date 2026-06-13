package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 * Extende DirectionalElectricBlock (que já carrega FACING, VoxelShaper rotacionado,
 * e o helper directionalNorthTerminals para montar o BlockStateTerminalCollection).
 *
 * Os dois TerminalBoundingBox batem com os cubos do grupo "Power" no modelo:
 *   TERM_L  →  from [4,1,10] to [6,2,12]   (plug esquerdo)
 *   TERM_N  →  from [10,1,10] to [12,2,12]  (plug direito)
 */
public class FloodlightBlock extends DirectionalElectricBlock implements IBE<FloodlightBlockEntity> {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // ── Terminais (coordenadas do modelo divididas por 16) ─────────────────
    private static final TerminalBoundingBox TERM_L = new TerminalBoundingBox(
            Component.literal("L"),
            4/16.0, 1/16.0, 10/16.0,
            6/16.0, 2/16.0, 12/16.0
    );
    private static final TerminalBoundingBox TERM_N = new TerminalBoundingBox(
            Component.literal("N"),
            10/16.0, 1/16.0, 10/16.0,
            12/16.0, 2/16.0, 12/16.0
    );

    // ── Shape base (facing NORTH) ──────────────────────────────────────────
    private static final VoxelShape NORTH_SHAPE = Block.box(3, 0, 3, 13, 9, 13);

    public FloodlightBlock(Properties properties) {
        super(properties);

        // Registra os terminais para TODAS as direções (o helper rotaciona automaticamente)
        setTerminalCollection(
            DirectionalElectricBlock.directionalNorthTerminals(
                this,
                new TerminalBoundingBox[]{ TERM_L, TERM_N },
                NORTH_SHAPE,  // shape quando FACING=NORTH/SOUTH/EAST/WEST
                NORTH_SHAPE   // shape quando FACING=UP/DOWN
            ).build()
        );

        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    // ── BlockState ─────────────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // adiciona FACING
        builder.add(LIT);
    }

    // ── Codec (obrigatório em BaseEntityBlock no 1.21.1) ──────────────────

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FloodlightBlock::new);
    }

    // ── IBE ────────────────────────────────────────────────────────────────

    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get();
    }

    // ── Utilidade ──────────────────────────────────────────────────────────

    /** Chamado pelo BlockEntity para mudar o estado LIT sem loop infinito. */
    public static void setLit(net.minecraft.world.level.Level level,
                               BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LIT) != lit)
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
    }

    @Override
    public int getLightEmission(BlockState state,
                                net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 4 : 0;
    }
}
