package com.example.powergridlights.compat;

import com.example.powergridlights.PowerGridLights;
import com.example.powergridlights.block.FloodlightBlock;
import com.example.powergridlights.registry.PGLBlocks;
import net.neoforged.fml.ModList;

/**
 * Optional compatibility with the "Shine" mod (tapeQz).
 *
 * <p>Shine adds selective bloom and colored-light VFX to Minecraft without
 * requiring shaders.  This compatibility class bridges the Floodlight block
 * (and future colored lamps) with Shine so that they:
 * <ol>
 *   <li>Emit visible bloom glow when lit.
 *   <li>Optionally tint the bloom color based on a lamp color property.
 * </ol>
 *
 * <h2>Important caveat</h2>
 * Shine (v1.0.0 for 1.21.1) does NOT currently expose a public Java API for
 * third-party mods to register custom bloom sources programmatically.  The mod
 * achieves bloom purely through a client-side post-processing shader that reads
 * emissive pixel data from the rendered frame – no registry hooks are needed.
 *
 * <p>Therefore, "Shine compatibility" for the Floodlight is achieved entirely
 * through <em>resource-pack convention</em>:
 * <ul>
 *   <li>The floodlight model uses an <strong>emissive texture layer</strong>
 *       (suffix {@code _e}) which Shine's shader picks up automatically.
 *   <li>The colored lamp variants store their RGB tint in the block-state and
 *       pass it to the vertex color so that Shine bloom inherits the correct hue.
 * </ul>
 *
 * <p>If Shine ever exposes a proper API this class is the right place to add
 * the registration logic, guarded by the {@link #isShineLoaded()} check.
 *
 * <h2>Emissive texture convention (Shine)</h2>
 * Create a texture at:
 * {@code assets/powergridlights/textures/block/floodlight_on_e.png}
 * This is the emissive layer that Shine's pipeline will pick up.  The brighter
 * the pixels, the stronger the bloom effect.  Transparent pixels are ignored.
 *
 * <h2>Future colored lamp compat</h2>
 * When you add colored lamps (e.g. a red/green/blue variant), each variant
 * should have its own {@code _e} texture tinted in the appropriate color.
 * Alternatively you can use a single grayscale emissive texture and override
 * the tint via the {@code tintindex} key in the model JSON.
 */
public final class ShineCompat {

    private ShineCompat() {}

    /** Returns true if the Shine mod is present in the current mod list. */
    public static boolean isShineLoaded() {
        return ModList.get().isLoaded("shine");
    }

    /**
     * Called during client setup to perform any Shine-specific registration.
     *
     * <p>Currently a no-op because Shine has no public API.  The emissive
     * textures in the resource-pack do all the work automatically.
     */
    public static void onClientSetup() {
        if (!isShineLoaded()) return;
        PowerGridLights.LOGGER.info(
            "[PowerGrid Lights] Shine detected – emissive textures will provide bloom for the Floodlight."
        );
        // Future: call Shine API here if/when one is added, e.g.
        //   ShineRegistry.registerEmissiveBlock(PGLBlocks.FLOODLIGHT.get(), ...);
    }
}
