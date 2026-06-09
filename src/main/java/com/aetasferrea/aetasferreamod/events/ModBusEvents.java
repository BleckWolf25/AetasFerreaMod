package com.aetasferrea.aetasferreamod.events;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        // Tells Forge to give our custom entity the vanilla horse attribute map before it ever spawns
        event.put(EntityInit.AETAS_HORSE.get(), Horse.createBaseHorseAttributes().build());
    }
}