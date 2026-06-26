/**
 * @file AetasFerreaMod.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Entry point for the Aetas Ferrea Forge mod.
 *
 * @description
 * Initialises the mod by registering items, entities, network packets, and the common config,
 * and conditionally registers the in-game config screen when no third-party config mod is present.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.init.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// ---------- CLASS: AetasFerreaMod
@Mod(AetasFerreaMod.MODID)
public class AetasFerreaMod {

    // ---------- CONSTANTS
    public static final String MODID = "aetasferreamod";

    // ---------- CONSTRUCTOR
    public AetasFerreaMod() {
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        com.aetasferrea.aetasferreamod.init.EntityInit.ENTITIES.register(modEventBus);

        // Deferred to FMLCommonSetupEvent so network packets register on both sides safely
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.MobSpawnEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.EconomyEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.FishingEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.command.EquineTestCommand.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.command.MiniBossCommand.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.command.VillagerTestCommand.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.difficulty.DifficultyEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.entity.ai.WolfEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.entity.boss.BossCombatHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.entity.boss.MiniBossManager.class);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.AttributeTooltipEventHandler.class);
        }
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.CombatEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.DragonProgressionHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.FantasyArmorHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.FireMechanicsHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.GoldenEnchantmentHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.GoldenEquipmentEventHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.HarvestFrictionHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.HorseMechanicsHandler.class);
        MinecraftForge.EVENT_BUS.register(com.aetasferrea.aetasferreamod.events.HorseReplacementHandler.class);
        
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AetasFerreaConfig.SPEC);

        // Only register the fallback config screen if no dedicated config mod is loaded
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("configured")
                    && !net.minecraftforge.fml.ModList.get().isLoaded("yet_another_config_lib_v3")) {
                ClientHelper.registerConfigScreen();
            }
        }
    }

    // ---------- COMMON SETUP
    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        // enqueueWork ensures thread-safe execution during the setup phase
        event.enqueueWork(() -> {
            com.aetasferrea.aetasferreamod.network.PacketHandler.register();
        });
    }

    // ---------- CLIENT CONFIG SCREEN REGISTRATION
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
