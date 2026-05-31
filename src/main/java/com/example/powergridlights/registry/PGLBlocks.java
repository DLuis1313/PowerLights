package com.example.powergridlights.registry;

import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.block.FloodlightBlock;
import com.example.powergridlights.block.FloodlightLightBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PGLBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PowerGridLights.MOD_ID);

    // ─────────────────────────────────────────────────────────────────────────
    // Floodlight (Refletor)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The placeable floodlight block.  Hardness matches an iron block.
     */
    public static final DeferredBlock<FloodlightBlock> FLOODLIGHT =
            BLOCKS.register("floodlight", () ->
                    new FloodlightBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(3.5f, 6.0f)
                                    .sound(SoundType.METAL)
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()           // transparent enough for light to work
                                    .lightLevel(state -> state.getValue(FloodlightBlock.LIT) ? 4 : 0)
                    )
            );

    // ─────────────────────────────────────────────────────────────────────────
    // Virtual / phantom light block (not obtainable, placed programmatically)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Invisible block placed in the light cone.  Not in any creative tab or
     * loot table – purely a server-side light source.
     */
    public static final DeferredBlock<FloodlightLightBlock> FLOODLIGHT_LIGHT =
            BLOCKS.register("floodlight_light", () ->
                    new FloodlightLightBlock(
                            BlockBehaviour.Properties.of()
                                    .noCollission()
                                    .noLootTable()
                                    .replaceable()
                                    .strength(-1.0f, 3600000.0f)  // unbreakable by players
                                    .lightLevel(state ->
                                            state.getValue(FloodlightLightBlock.LEVEL))
                    )
            );

    private PGLBlocks() {}
}
