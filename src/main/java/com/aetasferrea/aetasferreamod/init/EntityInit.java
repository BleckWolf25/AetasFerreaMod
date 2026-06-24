/**
 * @file EntityInit.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Deferred registration of all custom entity types for the Aetas Ferrea mod.
 *
 * @description
 * Declares and registers the AETAS_HORSE, AETAS_DONKEY, and AETAS_MULE entity types using
 * Forge's DeferredRegister, applying vanilla-accurate hitbox sizes and client tracking ranges.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */

// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.init;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.AetasMule;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// ---------- CLASS: EntityInit
public class EntityInit {

    // ---------- ENTITY REGISTRY
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AetasFerreaMod.MODID);

    // ---------- REGISTERED ENTITIES
    public static final RegistryObject<EntityType<HorseEventHandler>> AETAS_HORSE = ENTITIES.register("aetas_horse",
            () -> EntityType.Builder.of(HorseEventHandler::new, MobCategory.CREATURE)
                    // Matches the standard vanilla horse hitbox dimensions
                    .sized(1.3964844F, 1.6F)
                    .clientTrackingRange(10)
                    .build("aetas_horse"));

    public static final RegistryObject<EntityType<AetasDonkey>> AETAS_DONKEY = ENTITIES.register("aetas_donkey",
            () -> EntityType.Builder.of(AetasDonkey::new, MobCategory.CREATURE)
                    // Matches the standard vanilla donkey hitbox dimensions
                    .sized(1.3964844F, 1.5F)
                    .clientTrackingRange(10)
                    .build("aetas_donkey"));

    public static final RegistryObject<EntityType<AetasMule>> AETAS_MULE = ENTITIES.register("aetas_mule",
            () -> EntityType.Builder.of(AetasMule::new, MobCategory.CREATURE)
                    // Matches the standard vanilla mule hitbox dimensions
                    .sized(1.3964844F, 1.6F)
                    .clientTrackingRange(10)
                    .build("aetas_mule"));
}