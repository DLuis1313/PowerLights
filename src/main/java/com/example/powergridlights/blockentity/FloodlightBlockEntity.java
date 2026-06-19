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
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity do Refletor Elétrico.
 *
 * Notas importantes sobre a API do PowerGrid (extraídas do jar):
 *  - buildCircuit() e electricalTick() são chamados via reflection → sem @Override
 *  - applyPower(AbstractElectricWire) é package-private → não sobrescrever
 *  - connect(float, IElectricNode, IElectricNode): resistência é o 1º argumento
 *  - wire.potentialDifference() retorna double com a tensão
 *  - SmartBlockEntity: saveAdditional/setRemoved são final → usar write/read
 */
public class FloodlightBlockEntity extends ElectricBlockEntity implements IElectricEntity {

    private static final float  RESISTANCE  = 1440.0f; // 10 W @ 120 V
    private static final double MIN_VOLTAGE =   30.0;
    private static final int    LIGHT_RANGE =   16;
    private static final int    CONE_WIDTH  =    4;

    private ElectricWire wire;
    private boolean isLit = false;
    private final List<BlockPos> placedLights = new ArrayList<>();

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get(), pos, state);
    }

    // ── PowerGrid: chamados via reflection ────────────────────────────────────

    public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connect(RESISTANCE, builder.terminalNode(0), builder.terminalNode(1));
    }

    public void electricalTick() {
        if (wire == null || level == null || level.isClientSide()) return;
        boolean on = Math.abs(wire.potentialDifference()) >= MIN_VOLTAGE;
        if (on == isLit) return;
        isLit = on;
        if (level instanceof ServerLevel sl) updateLights(sl);
    }

    // ── PowerGrid: sobrescrita normal ─────────────────────────────────────────

    @Override
    public float getResistance() {
        return RESISTANCE;
    }

    @Override
    public ThermalBehaviour specifyThermalBehaviour() {
        return null;
    }

    // ── Cone de luz ───────────────────────────────────────────────────────────

    private void updateLights(ServerLevel sl) {
        removeLights(sl);
        FloodlightBlock.setLit(sl, worldPosition, getBlockState(), isLit);
        if (isLit) placeLightCone(sl, getBlockState().getValue(FloodlightBlock.FACING));
    }

    private void placeLightCone(ServerLevel sl, Direction dir) {
        Direction p1 = dir.getAxis() == Direction.Axis.Y ? Direction.EAST  : dir.getClockWise();
        Direction p2 = dir.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;

        for (int d = 1; d <= LIGHT_RANGE; d++) {
            int lvl  = Math.max(1, 15 - (int)(14.0 * (d - 1) / (LIGHT_RANGE - 1)));
            int half = Math.max(1, (int)Math.ceil(CONE_WIDTH * d / (double) LIGHT_RANGE));
            BlockPos centre = worldPosition.relative(dir, d);

            for (int a = -half; a <= half; a++) {
                for (int b = -half; b <= half; b++) {
                    BlockPos p = centre.relative(p1, a).relative(p2, b);
                    BlockState cur = sl.getBlockState(p);
                    if (cur.isAir() || cur.is(PGLBlocks.FLOODLIGHT_LIGHT.get())) {
                        sl.setBlock(p,
                            PGLBlocks.FLOODLIGHT_LIGHT.get().defaultBlockState()
                                     .setValue(FloodlightLightBlock.LEVEL, lvl),
                            Block.UPDATE_CLIENTS);
                        placedLights.add(p);
                    }
                }
            }
        }
        setChanged();
    }

    /** Público para ser chamado por FloodlightBlock.onRemove(). */
    public void removeLights(ServerLevel sl) {
        placedLights.removeIf(p -> {
            if (sl.getBlockState(p).is(PGLBlocks.FLOODLIGHT_LIGHT.get()))
                sl.removeBlock(p, false);
            return true;
        });
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("lit", isLit);
        if (!clientPacket) {
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
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        isLit = tag.getBoolean("lit");
        if (!clientPacket) {
            placedLights.clear();
            int[] xs = tag.getIntArray("lx"),
                  ys = tag.getIntArray("ly"),
                  zs = tag.getIntArray("lz");
            for (int i = 0; i < xs.length; i++)
                placedLights.add(new BlockPos(xs[i], ys[i], zs[i]));
        }
    }
}
