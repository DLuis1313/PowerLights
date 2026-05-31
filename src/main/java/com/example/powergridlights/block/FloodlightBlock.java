package com.example.powergridlights.block;

import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import com.example.powergridlights.registry.PGLBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import com.simibubi.create.foundation.block.IBE;

/**
 * FloodlightBlock — refletor elétrico direcional.
 *
 * Implementa IBE (Create pattern) para integrar com o sistema de BlockEntity
 * do Create/PowerGrid e define os terminais elétricos com TerminalBoundingBox,
 * que são os pontos visuais onde os fios se conectam.
 *
 * Os dois plugs do modelo (grupo "Power") estão em:
 *   Plug L (esquerdo): from [4,1,10] to [6,2,12]   → terminal quente
 *   Plug N (direito):  from [10,1,10] to [12,2,12]  → terminal neutro
 *
 * Coordenadas em espaço 0-1 = dividir por 16.
 */
public class FloodlightBlock extends BaseEntityBlock implements IBE<FloodlightBlockEntity> {

    // ── Block-state properties ─────────────────────────────────────────────
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty   LIT    = BlockStateProperties.LIT;

    // ── Terminal definitions (coordenadas dos plugs do modelo / 16) ────────

    /**
     * Plug "L" (Line/Hot) — cubo esquerdo do grupo Power.
     * from [4,1,10] to [6,2,12]  →  0.25, 0.0625, 0.625  a  0.375, 0.125, 0.75
     *
     * withOrigin() define o ponto exato onde o fio se prende (centro do topo do plug).
     */
    private static final TerminalBoundingBox TERMINAL_L_NORTH =
            new TerminalBoundingBox(Component.literal("L"),
                4/16.0, 1/16.0, 10/16.0,
                6/16.0, 2/16.0, 12/16.0)
            .withOrigin(new net.minecraft.world.phys.Vec3(5/16.0, 2/16.0, 11/16.0));

    /**
     * Plug "N" (Neutral) — cubo direito do grupo Power.
     * from [10,1,10] to [12,2,12]  →  0.625, 0.0625, 0.625  a  0.75, 0.125, 0.75
     */
    private static final TerminalBoundingBox TERMINAL_N_NORTH =
            new TerminalBoundingBox(Component.literal("N"),
                10/16.0, 1/16.0, 10/16.0,
                12/16.0, 2/16.0, 12/16.0)
            .withOrigin(new net.minecraft.world.phys.Vec3(11/16.0, 2/16.0, 11/16.0));

    public FloodlightBlock(Properties properties) {
        super(properties);

        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT,    false)
        );

        // ── Registrar terminais no PowerGrid ──────────────────────────────
        // forAllStatesExcept ignora a propriedade LIT (não muda posição dos plugs)
        // e rota os TerminalBoundingBox de acordo com o FACING do bloco.
        BlockStateTerminalCollection terminalCollection =
            BlockStateTerminalCollection.builder(this)
                .forAllStatesExcept(state -> {
                    Direction facing = state.getValue(FACING);
                    // Pega os terminais base (NORTH) e rotaciona para o FACING atual
                    TerminalBoundingBox tL = rotateTerminal(TERMINAL_L_NORTH, facing);
                    TerminalBoundingBox tN = rotateTerminal(TERMINAL_N_NORTH, facing);
                    return new TerminalBoundingBox[]{ tL, tN };
                }, LIT)
                .build();

        setTerminalCollection(terminalCollection);
    }

    // ── Rotação dos terminais ──────────────────────────────────────────────

    /**
     * Rota um terminal definido em NORTH para o facing desejado,
     * usando os helpers de rotação do próprio TerminalBoundingBox.
     */
    private static TerminalBoundingBox rotateTerminal(TerminalBoundingBox base,
                                                       Direction targetFacing) {
        return switch (targetFacing) {
            case NORTH -> base; // default, sem rotação
            case SOUTH -> base.rotateAroundY(net.minecraft.world.level.block.Rotation.CLOCKWISE_180);
            case EAST  -> base.rotateAroundY(net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
            case WEST  -> base.rotateAroundY(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            case UP    -> base.rotateAroundX(net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90);
            case DOWN  -> base.rotateAroundX(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        };
    }

    // ── BlockState ─────────────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // A face clicada é a face da parede, o refletor aponta PARA FORA dessa face.
        return defaultBlockState()
                .setValue(FACING, ctx.getClickedFace())
                .setValue(LIT, false);
    }

    // ── Collision shape ────────────────────────────────────────────────────
    // Shapes simples por enquanto — troque pelas shapes reais do seu modelo.

    private static final net.createmod.catnip.math.VoxelShaper SHAPE_SHAPER;
    static {
        VoxelShape baseShape = Block.box(3, 0, 3, 13, 9, 13);
        SHAPE_SHAPER = net.createmod.catnip.math.VoxelShaper
                .forDirectional(baseShape, Direction.NORTH);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return SHAPE_SHAPER.get(state.getValue(FACING));
    }

    // ── Render ─────────────────────────────────────────────────────────────

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        // Brilho suave no próprio bloco quando aceso
        return state.getValue(LIT) ? 4 : 0;
    }

    // ── BlockEntity ────────────────────────────────────────────────────────

    @Override
    public Class<FloodlightBlockEntity> getBlockEntityClass() {
        return FloodlightBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FloodlightBlockEntity> getBlockEntityType() {
        return PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get();
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return IBE.super.newBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get(),
                FloodlightBlockEntity::tick);
    }

    // ── Placement rules ─────────────────────────────────────────────────────

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction back = state.getValue(FACING).getOpposite();
        return level.getBlockState(pos.relative(back))
                    .isFaceSturdy(level, pos.relative(back), state.getValue(FACING));
    }

    public static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LIT) != lit)
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
    }
}
