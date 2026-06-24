package com.example.powergridlights.registry;

import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PGLBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PowerGridLights.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBlockEntity>> FLOODLIGHT =
            BLOCK_ENTITY_TYPES.register("floodlight", () ->
                    BlockEntityType.Builder
                            .<FloodlightBlockEntity>of(
                                    FloodlightBlockEntity::new,
                                    PGLBlocks.FLOODLIGHT.get()
                            )
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
