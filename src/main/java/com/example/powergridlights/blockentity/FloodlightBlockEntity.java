package com.example.powergridlights.blockentity;

import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.blockentity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.blockentity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.base.IElectricNode;
import org.patryk3211.powergrid.electricity.base.IElectricNode.FloatingNode;

import java.util.ArrayList;
import java.util.List;

public class FloodlightBlockEntity extends ElectricBlockEntity implements IElectricEntity {

    // Terminais: 0 = Line (fio vivo), 1 = Neutral (neutro)
    private static final int TERMINAL_LINE    = 0;
    private static final int TERMINAL_NEUTRAL = 1;

    private static final float RESISTANCE = 1440f; // ~10 W a 120 V
    private static final float MIN_VOLTAGE = 30f;
    private static final int   LIGHT_RANGE = 16;

    private AbstractElectricWire wire;
    private boolean powered = false;
    private final List<BlockPos> lightBlocks = new ArrayList<>();

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(PGLBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    // -------------------------------------------------------
    // API do PowerGrid — chamados via reflection, SEM @Override
    // -------------------------------------------------------

    /** Monta o circuito elétrico do bloco. Chamado via reflection pelo PowerGrid. */
    public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
        FloatingNode nodeL = builder.terminalNode(TERMINAL_LINE);
        FloatingNode nodeN = builder.terminalNode(TERMINAL_NEUTRAL);
        wire = builder.connect(RESISTANCE, nodeL, nodeN);
    }

    /** Tick elétrico. Chamado via reflection pelo PowerGrid. */
    public void electricalTick() {
        if (wire == null) return;

        float voltage = Math.abs(wire.potentialDifference());
        boolean shouldBePowered = voltage >= MIN_VOLTAGE;

        if (shouldBePowered != powered) {
            powered = shouldBePowered;
            updateLights();
        }
    }

    // -------------------------------------------------------
    // SmartBlockEntity — NBT
    // write/read em SmartBlockEntity assinatura: (CompoundTag, HolderLookup.Provider, boolean)
    // -------------------------------------------------------

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Powered", powered);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        powered = tag.getBoolean("Powered");
    }

    // -------------------------------------------------------
    // Lógica de luz
    // -------------------------------------------------------

    private void updateLights() {
        removeLights();
        if (powered && level != null) {
            placeLights();
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void placeLights() {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        BlockPos origin = worldPosition.relative(facing);

        for (int dist = 1; dist <= LIGHT_RANGE; dist++) {
            int spread = Math.min(dist / 3, 4); // abre de 0 até ±4
            for (int dx = -spread; dx <= spread; dx++) {
                for (int dy = -spread; dy <= spread; dy++) {
                    BlockPos target = origin
                            .relative(facing, dist - 1)
                            .offset(perpX(facing) * dx, perpY(facing) * dx, 0)
                            .offset(0, dy, 0);

                    // Posição real no eixo principal + desvios
                    target = offsetAlongFacing(origin, facing, dist, dx, dy);

                    if (level.getBlockState(target).isAir()) {
                        level.setBlock(target,
                                net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState()
                                        .setValue(net.minecraft.world.level.block.LightBlock.LEVEL,
                                                  Math.max(1, 15 - dist)),
                                3);
                        lightBlocks.add(target);
                    }
                }
            }
        }
    }

    private BlockPos offsetAlongFacing(BlockPos origin, Direction facing, int dist, int dx, int dy) {
        return switch (facing) {
            case NORTH -> origin.offset(dx, dy, -dist);
            case SOUTH -> origin.offset(dx, dy,  dist);
            case EAST  -> origin.offset( dist, dy, dx);
            case WEST  -> origin.offset(-dist, dy, dx);
            case UP    -> origin.offset(dx,  dist, dy);
            case DOWN  -> origin.offset(dx, -dist, dy);
        };
    }

    private int perpX(Direction d) {
        return (d == Direction.NORTH || d == Direction.SOUTH) ? 1 : 0;
    }

    private int perpY(Direction d) {
        return (d == Direction.EAST || d == Direction.WEST) ? 1 : 0;
    }

    public void removeLights() {
        if (level == null) return;
        for (BlockPos pos : lightBlocks) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.LightBlock) {
                level.removeBlock(pos, false);
            }
        }
        lightBlocks.clear();
        powered = false;
    }

    @Override
    public void invalidate() {
        removeLights();
        super.invalidate();
    }
}
