package com.example.powergridlights.registry;

import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.blockentity.FloodlightBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public final class PGLBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PowerGridLights.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloodlightBlockEntity>>
            FLOODLIGHT_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "floodlight",
            () -> BlockEntityType.Builder
                    .of(FloodlightBlockEntity::new, PGLBlocks.FLOODLIGHT.get())
                    .build(null)
    );

    private PGLBlockEntities() {}
}
