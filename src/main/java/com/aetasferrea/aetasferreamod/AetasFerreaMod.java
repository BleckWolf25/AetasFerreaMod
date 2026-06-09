package com.aetasferrea.aetasferreamod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.aetasferrea.aetasferreamod.init.ModItems;

@Mod(AetasFerreaMod.MODID)
public class AetasFerreaMod {

    public static final String MODID = "aetasferreamod";

    public AetasFerreaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ModItems.ITEMS.register(modEventBus);
        com.aetasferrea.aetasferreamod.init.EntityInit.ENTITIES.register(modEventBus);

        // Required for safe Network setup
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AetasFerreaConfig.SPEC);

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("configured") && !net.minecraftforge.fml.ModList.get().isLoaded("yet_another_config_lib_v3")) {
                ClientHelper.registerConfigScreen();
            }
        }
    }

    // Guarantees network protocol registers correctly
    private void commonSetup(final net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.aetasferrea.aetasferreamod.network.PacketHandler.register();
        });
    }

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