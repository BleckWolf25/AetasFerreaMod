/*
 * @file AetasFerreaMod.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Main Mod Entry Point
 *
 * @description BEHAVIOR:
 * - Initializes the Forge Mod context.
 * - Registers configuration files for common, client, and server logic.
 * - Acts as the primary event bus subscriber for initialization events.
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod;

// ---------- IMPORTS
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AetasFerreaMod.MODID)
public class AetasFerreaMod {

    // ---------- MOD CONSTANTS
    public static final String MODID = "aetasferreamod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---------- INITIALIZATION
    public AetasFerreaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AetasFerreaConfig.SPEC);

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("configured") && !net.minecraftforge.fml.ModList.get().isLoaded("yet_another_config_lib_v3")) {
                ClientHelper.registerConfigScreen();
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AetasFerreaMod common setup complete");
    }

    // ---------- CLIENT-ONLY REF ISOLATION
    private static class ClientHelper {
        private static void registerConfigScreen() {
            net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> {
                    return new com.aetasferrea.aetasferreamod.client.FallbackConfigScreen(screen);
                })
            );
        }
    }
}

