package com.example.powergridlights.client;

import com.example.powergridlights.compat.ShineCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.block.FloodlightBlock;
import com.example.powergridlights.registry.PGLBlocks;

/**
 * Client-side event handler.
 * Registered only on the CLIENT distribution.
 */
@EventBusSubscriber(modid = PowerGridLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PGLClientSetup {

    private PGLClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ShineCompat.onClientSetup();
        });
    }

    /**
     * Register a block color handler so the floodlight can be tinted
     * dynamically in the future (e.g. for colored variants).
     *
     * Currently returns -1 (no tint) for tint layer 0 and white (0xFFFFFF)
     * for tint layer 1 (the emissive layer when lit).
     */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, level, pos, tintIndex) -> {
                if (tintIndex == 1 && state.getValue(FloodlightBlock.LIT)) {
                    // White/neutral tint for the emissive layer
                    return 0xFFFFFF;
                }
                return -1; // no tint
            },
            PGLBlocks.FLOODLIGHT.get()
        );
    }
}
