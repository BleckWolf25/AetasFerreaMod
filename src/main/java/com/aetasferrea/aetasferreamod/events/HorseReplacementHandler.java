package com.aetasferrea.aetasferreamod.events;

import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.aetasferrea.aetasferreamod.AetasFerreaMod.MODID)
public class HorseReplacementHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // If it is a vanilla Horse (but specifically NOT our custom HorseEventHandler)
        if (event.getEntity().getClass() == Horse.class) {
            Horse vanillaHorse = (Horse) event.getEntity();
            
            // 1. Cancel the vanilla spawn
            event.setCanceled(true);
            
            // 2. Create our custom horse
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                HorseEventHandler customHorse = EntityInit.AETAS_HORSE.get().create(serverLevel);
                if (customHorse != null) {
                    // Copy location and rotation
                    customHorse.moveTo(vanillaHorse.getX(), vanillaHorse.getY(), vanillaHorse.getZ(), vanillaHorse.getYRot(), vanillaHorse.getXRot());
                    
                    // Crucial fix: Properly transfer Foal/Baby status and scale logic 
                    if (vanillaHorse.isBaby()) {
                        customHorse.setBaby(true);
                    }
                    customHorse.setAge(vanillaHorse.getAge());
                    
                    // TODO: In Phase 2, if the vanilla horse had a saddle/armor, copy its inventory here before spawning.
                    
                    // Spawn the custom horse
                    serverLevel.addFreshEntity(customHorse);
                }
            }
        }
    }
}