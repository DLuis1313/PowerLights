package com.example.powergridlights.blockentity;

import com.example.powergridlights.block.FloodlightBlock;
import com.example.powergridlights.block.FloodlightLightBlock;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.example.powergridlights.registry.PGLBlocks;
import com.patryk3211.powergrid.blockentity.base.IElectricalBlockEntity;
import com.patryk3211.powergrid.electricity.GlobalElectricNetwork;
import com.patryk3211.powergrid.electricity.circuit.ElectricNetwork;
import com.patryk3211.powergrid.electricity.circuit.node.CircuitNode;
import com.patryk3211.powergrid.electricity.unit.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity for the Floodlight.
 *
 * <h2>Electrical integration (PowerGrid)</h2>
 * The floodlight registers as an electrical consumer on the PowerGrid network.
 * It has one terminal on the BACK face (opposite to FACING) for the hot wire
 * and uses the block below as the return/neutral path (configurable).
 *
 * <p>Key parameters:
 * <pre>
 *   Rated voltage  = 120 V
 *   Resistance     = 1440 Ω  →  P = V²/R = 10 W
 *   Min voltage    =  60 V (lamp is dim)
 *   Max voltage    = 240 V (lamp explodes – not yet implemented)
 * </pre>
 *
 * <h2>Light cone generation</h2>
 * Each server tick (20/s) is too expensive for heavy raycasting; instead we
 * update lights every {@link #LIGHT_UPDATE_INTERVAL} ticks.  When ON the BE
 * places {@link FloodlightLightBlock}s in a cone shape ahead of the floodlight.
 * When OFF all placed light blocks are removed.
 *
 * <h2>NOTE FOR IMPLEMENTORS</h2>
 * The exact PowerGrid API (package names, interface methods) depends on the
 * specific PowerGrid version you are compiling against.  The imports and method
 * calls below are written against the public API visible in the 1.20.1 sources
 * (architectury-1.20.1/dev branch).  If the NeoForge 1.21.1 port differs you
 * will need to adjust accordingly.
 */
public class FloodlightBlockEntity extends /* PowerGrid base */ net.minecraft.world.level.block.entity.BlockEntity
        implements IElectricalBlockEntity {

    // ----- Constants -----

    /** How far (in blocks) the light cone extends. */
    public static final int LIGHT_RANGE = 16;

    /** How wide the cone is at max range (blocks). The cone is square in cross-section. */
    public static final int CONE_HALF_WIDTH = 4;

    /** Ticks between light-placement updates (1 second). */
    private static final int LIGHT_UPDATE_INTERVAL = 20;

    /** Floodlight rated voltage (V). */
    private static final double RATED_VOLTAGE = 120.0;

    /** Floodlight resistance (Ω). Determines power consumption. */
    private static final double RESISTANCE = 1440.0; // P = V²/R = 10 W @ 120V

    /** Minimum RMS voltage for the lamp to produce light. */
    private static final double MIN_VOLTAGE = 30.0;

    // ----- Electrical circuit nodes -----

    /** The two circuit nodes – one for each terminal of the lamp. */
    private CircuitNode nodeA;
    private CircuitNode nodeB;

    // ----- Internal state -----

    /** Positions of all light blocks currently placed by this floodlight. */
    private final List<BlockPos> placedLights = new ArrayList<>();

    /** Tick counter for throttling light updates. */
    private int tickCounter = 0;

    /** Whether the lamp is currently on. */
    private boolean isLit = false;

    /** Voltage across the lamp this tick (used for brightness scaling). */
    private double currentVoltage = 0.0;

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(PGLBlockEntities.FLOODLIGHT_BLOCK_ENTITY.get(), pos, state);
    }

    // =========================================================================
    // PowerGrid – IElectricalBlockEntity
    // =========================================================================

    /**
     * Called by PowerGrid when it needs to know what circuit nodes this block
     * exposes on each face.  The floodlight has:
     * <ul>
     *   <li>Terminal A on the BACK face (the mount face)
     *   <li>Terminal B on any adjacent face that connects to a return conductor
     * </ul>
     * Returning {@code null} means that face has no electrical connection.
     */
    @Override
    @Nullable
    public CircuitNode getNode(Direction face) {
        Direction facing = getBlockState().getValue(FloodlightBlock.FACING);
        Direction back   = facing.getOpposite();

        if (face == back)   return nodeA; // hot terminal
        if (face == Direction.DOWN) return nodeB; // neutral / return

        return null;
    }

    /**
     * Called by PowerGrid when the block joins or re-joins a network.
     * We create the two circuit nodes and insert a resistor between them.
     */
    @Override
    public void onNetworkJoin(ElectricNetwork network) {
        nodeA = network.createNode();
        nodeB = network.createNode();
        // Insert a fixed resistor between the two terminals
        network.addResistor(nodeA, nodeB, RESISTANCE);
    }

    /**
     * Called every simulation step by PowerGrid (not every game tick).
     * We read the voltage across our resistor and update the lit state.
     */
    @Override
    public void onNetworkTick(ElectricNetwork network) {
        if (nodeA == null || nodeB == null) return;

        double va = nodeA.getVoltage();
        double vb = nodeB.getVoltage();
        double v  = Math.abs(va - vb);

        currentVoltage = v;
        boolean shouldBeLit = v >= MIN_VOLTAGE;

        if (shouldBeLit != isLit) {
            isLit = shouldBeLit;
            // Trigger a light update next tick
            tickCounter = LIGHT_UPDATE_INTERVAL;
        }
    }

    // =========================================================================
    // Tick logic
    // =========================================================================

    /**
     * Static ticker called by the block's getTicker().
     */
    public static void tick(Level level, BlockPos pos, BlockState state,
                            FloodlightBlockEntity be) {
        if (level.isClientSide()) return;

        be.tickCounter++;
        if (be.tickCounter >= LIGHT_UPDATE_INTERVAL) {
            be.tickCounter = 0;
            be.updateLights((ServerLevel) level, pos, state);
        }
    }

    // =========================================================================
    // Light-cone management
    // =========================================================================

    /**
     * Recomputes which positions in the cone should have light blocks, then
     * adds/removes them as needed.
     */
    private void updateLights(ServerLevel level, BlockPos origin, BlockState state) {
        // 1. Remove all previously placed lights
        removePlacedLights(level);

        // 2. Update the block state LIT property
        FloodlightBlock.setLit(level, origin, state, isLit);

        // 3. If off, nothing more to do
        if (!isLit) return;

        // 4. Determine light level from voltage (brightness scales with voltage)
        int maxLight = computeMaxLightLevel();

        // 5. Place new lights in the cone
        Direction facing = state.getValue(FloodlightBlock.FACING);
        placeLightCone(level, origin, facing, maxLight);
    }

    /**
     * Places invisible light blocks in a widening cone starting one block
     * in front of the floodlight head.
     *
     * <p>The cone uses a simple square cross-section that expands linearly
     * from 1×1 at distance 1 to (2*CONE_HALF_WIDTH+1)² at LIGHT_RANGE.
     */
    private void placeLightCone(ServerLevel level, BlockPos origin,
                                 Direction facing, int maxLight) {
        // Axis vectors perpendicular to the facing direction
        Direction perp1;
        Direction perp2;
        if (facing.getAxis() == Direction.Axis.Y) {
            perp1 = Direction.EAST;
            perp2 = Direction.SOUTH;
        } else {
            perp1 = facing.getClockWise();
            perp2 = Direction.UP;
        }

        for (int dist = 1; dist <= LIGHT_RANGE; dist++) {
            // Light level decreases with distance
            int lightLevel = maxLight - (int)((maxLight - 1) * (double)(dist - 1) / (LIGHT_RANGE - 1));
            if (lightLevel < 1) break;

            // Cone width at this distance
            int halfWidth = (int) Math.ceil(CONE_HALF_WIDTH * dist / (double) LIGHT_RANGE);

            BlockPos center = origin.relative(facing, dist);

            for (int a = -halfWidth; a <= halfWidth; a++) {
                for (int b = -halfWidth; b <= halfWidth; b++) {
                    BlockPos candidate = center
                            .relative(perp1, a)
                            .relative(perp2, b);

                    // Don't place inside solid blocks
                    BlockState existing = level.getBlockState(candidate);
                    if (existing.isAir() || existing.is(PGLBlocks.FLOODLIGHT_LIGHT.get())) {
                        BlockState lightState = PGLBlocks.FLOODLIGHT_LIGHT.get()
                                .defaultBlockState()
                                .setValue(FloodlightLightBlock.LEVEL, lightLevel);
                        level.setBlock(candidate, lightState, Block.UPDATE_CLIENTS);
                        placedLights.add(candidate);
                    }
                }
            }
        }
        setChanged();
    }

    /** Removes every light block previously placed by this floodlight. */
    private void removePlacedLights(ServerLevel level) {
        for (BlockPos pos : placedLights) {
            BlockState existing = level.getBlockState(pos);
            if (existing.is(PGLBlocks.FLOODLIGHT_LIGHT.get())) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                               Block.UPDATE_CLIENTS);
            }
        }
        placedLights.clear();
    }

    /**
     * Computes the maximum light level (1-15) emitted at the floodlight head
     * based on the current voltage.  At rated voltage we get 15; at minimum
     * voltage we get 1.
     */
    private int computeMaxLightLevel() {
        if (currentVoltage <= MIN_VOLTAGE) return 1;
        double ratio = Math.min(currentVoltage / RATED_VOLTAGE, 1.0);
        return Math.max(1, (int) Math.round(ratio * 15.0));
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void setRemoved() {
        // When the block is broken, clean up all light blocks immediately
        if (level instanceof ServerLevel serverLevel) {
            removePlacedLights(serverLevel);
            // Make sure the block-state resets too
            FloodlightBlock.setLit(serverLevel, worldPosition, getBlockState(), false);
        }
        super.setRemoved();
    }

    // =========================================================================
    // NBT serialisation (save/load)
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("lit", isLit);
        tag.putDouble("voltage", currentVoltage);
        // Save placed lights so we can remove them after a reload
        int[] xs = new int[placedLights.size()];
        int[] ys = new int[placedLights.size()];
        int[] zs = new int[placedLights.size()];
        for (int i = 0; i < placedLights.size(); i++) {
            xs[i] = placedLights.get(i).getX();
            ys[i] = placedLights.get(i).getY();
            zs[i] = placedLights.get(i).getZ();
        }
        tag.putIntArray("light_xs", xs);
        tag.putIntArray("light_ys", ys);
        tag.putIntArray("light_zs", zs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isLit          = tag.getBoolean("lit");
        currentVoltage = tag.getDouble("voltage");
        placedLights.clear();
        int[] xs = tag.getIntArray("light_xs");
        int[] ys = tag.getIntArray("light_ys");
        int[] zs = tag.getIntArray("light_zs");
        for (int i = 0; i < xs.length; i++) {
            placedLights.add(new BlockPos(xs[i], ys[i], zs[i]));
        }
    }
        }
