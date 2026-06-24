package com.example.powergridlights.blockentity;

import com.example.powergridlights.registry.PGLBlockEntities;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.ArrayList;
import java.util.List;

public class FloodlightBlockEntity extends ElectricBlockEntity implements IElectricEntity {

    private static final int   TERMINAL_LINE    = 0;
    private static final int   TERMINAL_NEUTRAL = 1;
    private static final float RESISTANCE       = 1440f; // ~10 W a 120 V
    private static final float MIN_VOLTAGE      = 30f;
    private static final int   LIGHT_RANGE      = 14;

    private ElectricWire wire    = null;
    private boolean      powered = false;
    private final List<BlockPos> lightBlocks = new ArrayList<>();

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(PGLBlockEntities.FLOODLIGHT.get(), pos, state);
    }

    // ---------------------------------------------------------------
    // IElectricEntity — chamados pelo PowerGrid via reflection/interface
    // NÃO usar @Override (o compilador não os reconhece como override em ElectricBlockEntity)
    // ---------------------------------------------------------------

    public void buildCircuit(IElectricEntity.CircuitBuilder builder) {
        builder.setTerminalCount(2);
        FloatingNode nodeL = builder.terminalNode(TERMINAL_LINE);
        FloatingNode nodeN = builder.terminalNode(TERMINAL_NEUTRAL);
        // connect(float, IElectricNode, IElectricNode) → ElectricWire
        wire = builder.connect(RESISTANCE, nodeL, nodeN);
    }

    public void electricalTick() {
        if (wire == null) return;
        float voltage = Math.abs(wire.potentialDifference());
        boolean shouldBePowered = voltage >= MIN_VOLTAGE;
        if (shouldBePowered != powered) {
            powered = shouldBePowered;
            updateLights();
        }
    }

    // ---------------------------------------------------------------
    // SmartBlockEntity — NBT
    // Assinatura confirmada: (CompoundTag, HolderLookup.Provider, boolean)
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Lógica de iluminação
    // ---------------------------------------------------------------

    private void updateLights() {
        removeLights();
        if (powered && level != null && !level.isClientSide) {
            placeLights();
        }
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void placeLights() {
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);

        for (int dist = 1; dist <= LIGHT_RANGE; dist++) {
            int spread = Math.min(dist / 3, 3);
            for (int a = -spread; a <= spread; a++) {
                for (int b = -spread; b <= spread; b++) {
                    BlockPos target = projectPos(facing, dist, a, b);
                    if (level.getBlockState(target).isAir()) {
                        int lvl = Math.max(1, 15 - dist);
                        level.setBlock(target,
                                Blocks.LIGHT.defaultBlockState()
                                        .setValue(LightBlock.LEVEL, lvl),
                                3);
                        lightBlocks.add(target);
                    }
                }
            }
        }
    }

    private BlockPos projectPos(Direction facing, int dist, int a, int b) {
        BlockPos origin = worldPosition.relative(facing);
        return switch (facing) {
            case NORTH -> origin.offset(a, b, -(dist - 1));
            case SOUTH -> origin.offset(a, b,   dist - 1);
            case EAST  -> origin.offset(  dist - 1, b, a);
            case WEST  -> origin.offset(-(dist - 1), b, a);
            case UP    -> origin.offset(a,   dist - 1, b);
            case DOWN  -> origin.offset(a, -(dist - 1), b);
        };
    }

    public void removeLights() {
        if (level == null) return;
        for (BlockPos pos : lightBlocks) {
            if (level.getBlockState(pos).getBlock() instanceof LightBlock) {
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
