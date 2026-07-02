/**
 * @file ModEntities.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Entity registration for Aetas Ferrea mod.
 *
 * @description
 * Registers custom entity types for Monarchs and Vanguards.
 *
 * @since 30/06/2026
 * @updated 02/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.entity.boss.MonarchEntity;
import com.aetasferrea.aetasferreamod.entity.boss.VanguardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// ---------- CLASS: MOD ENTITIES
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AetasFerreaMod.MODID);

    public static final RegistryObject<EntityType<MonarchEntity>> MONARCH = ENTITY_TYPES.register("monarch",
            () -> EntityType.Builder.of(MonarchEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("monarch"));

    public static final RegistryObject<EntityType<VanguardEntity>> VANGUARD = ENTITY_TYPES.register("vanguard",
            () -> EntityType.Builder.of(VanguardEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .clientTrackingRange(8)
                    .fireImmune()
                    .build("vanguard"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
