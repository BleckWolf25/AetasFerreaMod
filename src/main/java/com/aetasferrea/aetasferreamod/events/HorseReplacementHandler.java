/**
 * @file HorseReplacementHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Intercepts vanilla Horse, Donkey, and Mule spawns and replaces them with Aetas Ferrea variants.
 *
 * @description
 * Subscribes to EntityJoinLevelEvent on the server side and cancels any joining vanilla Horse, Donkey,
 * or Mule entity, then spawns the corresponding Aetas Ferrea custom entity in its place, copying NBT data
 * and re-rolling stats according to the custom equine system.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import java.util.Objects;

import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.AetasMule;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: HorseReplacementHandler

public class HorseReplacementHandler {

    // ---------- ENTITY JOIN INTERCEPTION
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // ---------- VANILLA HORSE REPLACEMENT
        if (event.getEntity().getClass() == Horse.class) {
            Horse vanillaHorse = (Horse) event.getEntity();
            event.setCanceled(true);

            CompoundTag nbt = new CompoundTag();
            vanillaHorse.saveWithoutId(nbt);

            // AetasProcessed flag distinguishes newly spawned (fresh) from loaded (save-file) horses
            boolean isNew = !vanillaHorse.getPersistentData().getBoolean("AetasProcessed");

            if (isNew) {
                float rand = serverLevel.random.nextFloat();

                if (rand < 0.15f) {
                    // 15% chance: replace with a custom Donkey
                    AetasDonkey donkey = EntityInit.AETAS_DONKEY.get().create(serverLevel);
                    if (donkey != null) {
                        donkey.load(nbt); // Copy position, age, and vanilla stats
                        donkey.rerollStats(Objects.requireNonNull(serverLevel.getRandom())); // Apply custom Aetas stats
                        donkey.getPersistentData().putBoolean("AetasProcessed", true);
                        serverLevel.addFreshEntity(donkey);
                    }
                } else if (rand < 0.16f) {
                    // 1% chance: replace with a custom Mule
                    AetasMule mule = EntityInit.AETAS_MULE.get().create(serverLevel);
                    if (mule != null) {
                        mule.load(nbt);
                        mule.rerollStats(Objects.requireNonNull(serverLevel.getRandom()));
                        mule.getPersistentData().putBoolean("AetasProcessed", true);
                        serverLevel.addFreshEntity(mule);
                    }
                } else {
                    // 84% chance: standard Aetas Horse
                    HorseEventHandler customHorse = EntityInit.AETAS_HORSE.get().create(serverLevel);
                    if (customHorse != null) {
                        customHorse.load(nbt);
                        customHorse.getPersistentData().putBoolean("AetasProcessed", true);
                        serverLevel.addFreshEntity(customHorse);
                    }
                }
            } else {
                // Existing save-file horse reloading: keep type as Aetas Horse
                HorseEventHandler customHorse = EntityInit.AETAS_HORSE.get().create(serverLevel);
                if (customHorse != null) {
                    customHorse.load(nbt);
                    serverLevel.addFreshEntity(customHorse);
                }
            }

        // ---------- VANILLA DONKEY REPLACEMENT
        } else if (event.getEntity().getClass() == Donkey.class) {
            Donkey vanillaDonkey = (Donkey) event.getEntity();
            event.setCanceled(true);

            AetasDonkey donkey = EntityInit.AETAS_DONKEY.get().create(serverLevel);
            if (donkey != null) {
                CompoundTag nbt = new CompoundTag();
                vanillaDonkey.saveWithoutId(nbt);
                donkey.load(nbt); // Copies existing items and name
                donkey.rerollStats(Objects.requireNonNull(serverLevel.getRandom()));  // Update stats to Aetas standards
                serverLevel.addFreshEntity(donkey);
            }

        // ---------- VANILLA MULE REPLACEMENT
        } else if (event.getEntity().getClass() == Mule.class) {
            Mule vanillaMule = (Mule) event.getEntity();
            event.setCanceled(true);

            AetasMule mule = EntityInit.AETAS_MULE.get().create(serverLevel);
            if (mule != null) {
                CompoundTag nbt = new CompoundTag();
                vanillaMule.saveWithoutId(nbt);
                mule.load(nbt); // Copies existing items and name
                mule.rerollStats(Objects.requireNonNull(serverLevel.getRandom()));  // Update stats to Aetas standards
                serverLevel.addFreshEntity(mule);
            }
        }
    }
}
