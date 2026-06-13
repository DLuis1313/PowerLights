package com.example.powergridlights.blockentity;

import com.example.powergridlights.block.FloodlightBlock;
import com.example.powergridlights.block.FloodlightLightBlock;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.example.powergridlights.registry.PGLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity do Refletor Elétrico.
 *
 * Extende ElectricBlockEntity (Create/PowerGrid SmartBlockEntity com ElectricBehaviour).
 * O método chave é buildCircuit(): é chamado pelo PowerGrid toda vez que o bloco
 * entra em uma rede elétrica. Aqui definimos 2 terminais e conectamos com um resistor.
 *
 * Circuito:
 *   Terminal 0 (TERM_L, plug esquerdo)  ──[R=1440Ω]──  Terminal 1 (TERM_N, plug direito)
 *
 * A 120V a potência é V²/R = 14400/1440 = 10W.
 */
public class FloodlightBlockEntity extends ElectricBlockEntity implements IElectricEntity {

    // ── Parâmetros elétricos ────────────────────────────────────────────────
    private static final double RESISTANCE  = 1440.0; // Ω  →  10W @ 120V
    private static final double MIN_VOLTAGE =   30.0; // V  abaixo disso apaga

    // ── Cone de luz ──────────────────────────────────────────────────────────
    private static final int LIGHT_RANGE      = 16;  // blocos à frente
    private static final int CONE_HALF_WIDTH  =  4;  // ±blocos na extremidade

    // ── Estado interno ───────────────────────────────────────────────────────
    private ElectricWire wire;
    private boolean isLit = false;
    private final List<BlockPos> placedLights = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────────────
    public FloodlightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Construtor sem tipo explícito (chamado pelo registro via lambda)
    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        this(PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get(), pos, state);
    }

    // ── API do PowerGrid ─────────────────────────────────────────────────────

    /**
     * PowerGrid chama este método quando o bloco entra em uma rede elétrica.
     * Criamos 2 nós (um por terminal) e os conectamos com um resistor fixo.
     */
    @Override
    public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
        builder.setTerminalCount(2);
        var nodeL = builder.terminalNode(0); // terminal TERM_L (plug esquerdo)
        var nodeN = builder.terminalNode(1); // terminal TERM_N (plug direito)
        wire = builder.connect(nodeL, nodeN);
        wire.setResistance(RESISTANCE);
    }

    /**
     * Chamado a cada tick elétrico (sincronizado com a simulação da rede).
     * Aqui lemos a tensão e atualizamos o estado de iluminação.
     */
    @Override
    public void electricalTick() {
        if (wire == null) return;

        double voltage = Math.abs(wire.potentialDifference());
        boolean shouldBeLit = voltage >= MIN_VOLTAGE;

        if (shouldBeLit != isLit) {
            isLit = shouldBeLit;
            if (level instanceof ServerLevel sl) {
                updateLights(sl);
            }
        }
    }

    /**
     * PowerGrid chama este método para aplicar potência/calor ao bloco.
     * Delegamos para o comportamento térmico da superclasse.
     */
    @Override
    public void applyPower(AbstractElectricWire w) {
        super.applyPower(w);
    }

    /** Sem aquecimento térmico para o refletor (não esquenta). */
    @Override
    @Nullable
    public ThermalBehaviour specifyThermalBehaviour() {
        return null;
    }

    // ── Cone de luz ──────────────────────────────────────────────────────────

    private void updateLights(ServerLevel level) {
        removePlacedLights(level);
        FloodlightBlock.setLit(level, worldPosition, getBlockState(), isLit);
        if (!isLit) return;

        Direction facing = getBlockState().getValue(FloodlightBlock.FACING);
        placeLightCone(level, facing);
    }

    private void placeLightCone(ServerLevel level, Direction facing) {
        // Eixos perpendiculares à direção do feixe
        Direction perp1, perp2;
        if (facing.getAxis() == Direction.Axis.Y) {
            perp1 = Direction.EAST;
            perp2 = Direction.SOUTH;
        } else {
            perp1 = facing.getClockWise();
            perp2 = Direction.UP;
        }

        for (int dist = 1; dist <= LIGHT_RANGE; dist++) {
            // Nível de luz diminui com a distância (15 → 1)
            int lightLevel = 15 - (int)((14.0 * (dist - 1)) / (LIGHT_RANGE - 1));
            if (lightLevel < 1) break;

            int halfWidth = (int) Math.ceil(CONE_HALF_WIDTH * dist / (double) LIGHT_RANGE);
            BlockPos center = worldPosition.relative(facing, dist);

            for (int a = -halfWidth; a <= halfWidth; a++) {
                for (int b = -halfWidth; b <= halfWidth; b++) {
                    BlockPos candidate = center.relative(perp1, a).relative(perp2, b);
                    BlockState existing = level.getBlockState(candidate);
                    if (existing.isAir() || existing.is(PGLBlocks.FLOODLIGHT_LIGHT.get())) {
                        level.setBlock(candidate,
                            PGLBlocks.FLOODLIGHT_LIGHT.get().defaultBlockState()
                                .setValue(FloodlightLightBlock.LEVEL, lightLevel),
                            Block.UPDATE_CLIENTS);
                        placedLights.add(candidate);
                    }
                }
            }
        }
        setChanged();
    }

    private void removePlacedLights(ServerLevel level) {
        for (BlockPos pos : placedLights) {
            if (level.getBlockState(pos).is(PGLBlocks.FLOODLIGHT_LIGHT.get()))
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                               Block.UPDATE_CLIENTS);
        }
        placedLights.clear();
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) {
            removePlacedLights(sl);
            FloodlightBlock.setLit(sl, worldPosition, getBlockState(), false);
        }
        super.setRemoved();
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("lit", isLit);
        int n = placedLights.size();
        int[] xs = new int[n], ys = new int[n], zs = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = placedLights.get(i).getX();
            ys[i] = placedLights.get(i).getY();
            zs[i] = placedLights.get(i).getZ();
        }
        tag.putIntArray("lx", xs);
        tag.putIntArray("ly", ys);
        tag.putIntArray("lz", zs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,
                                  net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isLit = tag.getBoolean("lit");
        placedLights.clear();
        int[] xs = tag.getIntArray("lx");
        int[] ys = tag.getIntArray("ly");
        int[] zs = tag.getIntArray("lz");
        for (int i = 0; i < xs.length; i++)
            placedLights.add(new BlockPos(xs[i], ys[i], zs[i]));
    }
}
