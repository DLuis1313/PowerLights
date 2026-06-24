package com.example.powergridlights;

import com.example.powergridlights.registry.PGLBlocks;
import com.example.powergridlights.registry.PGLBlockEntities;
import com.example.powergridlights.registry.PGLItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PowerGridLights.MOD_ID)
public class PowerGridLights {

    public static final String MOD_ID = "powergridlights";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public PowerGridLights(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("PowerGrid Lights Addon initializing...");

        // Register all deferred registries to the mod event bus
        PGLBlocks.BLOCKS.register(modBus);
        PGLItems.ITEMS.register(modBus);
        PGLBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        LOGGER.info("PowerGrid Lights Addon initialized.");
    }
}
