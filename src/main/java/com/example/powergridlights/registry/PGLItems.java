package com.example.powergridlights.registry;

import com.example.powergridlights.PowerGridLights;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PGLItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PowerGridLights.MOD_ID);

    // ─────────────────────────────────────────────────────────────────────────
    // Floodlight item (the one players hold and place)
    // ─────────────────────────────────────────────────────────────────────────

    public static final DeferredItem<BlockItem> FLOODLIGHT =
            ITEMS.registerSimpleBlockItem(
                    "floodlight",
                    PGLBlocks.FLOODLIGHT,
                    new Item.Properties()
            );

    // NOTE: FLOODLIGHT_LIGHT has no item – it is purely server-side.

    private PGLItems() {}
}
