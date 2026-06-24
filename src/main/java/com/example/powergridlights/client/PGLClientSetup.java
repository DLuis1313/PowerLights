package com.example.powergridlights.client;

import com.example.powergridlights.compat.ShineCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.registry.PGLBlocks;

@EventBusSubscriber(modid = PowerGridLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PGLClientSetup {

    private PGLClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ShineCompat.onClientSetup();
        });
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Sem tint dinâmico por enquanto — o estado ligado/desligado é
        // controlado internamente pelo FloodlightBlockEntity, não pelo blockstate.
        event.register(
            (state, level, pos, tintIndex) -> -1,
            PGLBlocks.FLOODLIGHT.get()
        );
    }
}
