package com.example.powergridlights.registry;

import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.block.FloodlightBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PGLBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PowerGridLights.MOD_ID);

    public static final DeferredBlock<FloodlightBlock> FLOODLIGHT =
            BLOCKS.register("floodlight", () ->
                    new FloodlightBlock(Block.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0f, 6.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .lightLevel(state -> 4)  // luz fraca quando colocado; a iluminação real é via blocos de luz
                    ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
