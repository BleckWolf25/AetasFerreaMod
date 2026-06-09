package com.aetasferrea.aetasferreamod.init;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AetasFerreaMod.MODID);

    public static final RegistryObject<EntityType<HorseEventHandler>> AETAS_HORSE = ENTITIES.register("aetas_horse",
            () -> EntityType.Builder.of(HorseEventHandler::new, MobCategory.CREATURE)
                    .sized(1.3964844F, 1.6F) // Standard vanilla horse hitbox
                    .clientTrackingRange(10)
                    .build("aetas_horse"));
}