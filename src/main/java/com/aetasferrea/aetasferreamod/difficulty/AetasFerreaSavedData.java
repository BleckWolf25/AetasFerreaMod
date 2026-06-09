/**
 * @file AetasFerreaSavedData.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Persistent world saved data tracking custom difficulty states.
 *
 * @description
 * Manages saved data for the world, specifically tracking the last in-game day a mini-boss spawned.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.difficulty;

// ---------- IMPORTS
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

// ---------- CLASS: AETAS FERREA SAVED DATA
public class AetasFerreaSavedData extends SavedData {

    // ---------- CONSTANTS & FIELDS
    private static final String DATA_NAME = "aetasferrea_difficulty_data";
    private int lastSpawnedDay = -1;

    // ---------- DATA RETRIEVAL (STATIC API)
    public static AetasFerreaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            AetasFerreaSavedData::load,
            AetasFerreaSavedData::new,
            DATA_NAME
        );
    }

    // ---------- CONSTRUCTORS & DESERIALIZATION
    public AetasFerreaSavedData() {}

    /**
     * Factory method to load data from an NBT CompoundTag.
     */
    public static AetasFerreaSavedData load(CompoundTag tag) {
        AetasFerreaSavedData data = new AetasFerreaSavedData();
        data.lastSpawnedDay = tag.getInt("lastSpawnedDay");
        return data;
    }

    // ---------- SERIALIZATION (SAVE)
    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        tag.putInt("lastSpawnedDay", this.lastSpawnedDay);
        return tag;
    }

    // ---------- GETTERS & SETTERS
    public int getLastSpawnedDay() {
        return lastSpawnedDay;
    }

    public void setLastSpawnedDay(int day) {
        this.lastSpawnedDay = day;
        setDirty(); // Crucial to mark dirty so Minecraft saves changes to disk
    }
}
