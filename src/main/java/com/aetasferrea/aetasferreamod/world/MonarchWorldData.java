/**
 * @file MonarchWorldData.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Persistent world saved data tracking granted Hollow Monarch structure & biome regions.
 *
 * @description
 * Manages saved data for the world, specifically ensuring that each valid Keep, Bastion, or biome region
 * grants exactly ONE Hollow Monarch and can never spawn more than one.
 *
 * @since 01/07/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.world;

// ---------- IMPORTS
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// ---------- CLASS: MonarchWorldData
public class MonarchWorldData extends SavedData {

    private static final String DATA_NAME = "aetas_monarch_world_data";
    private final Set<String> grantedRegions = new HashSet<>();

    public MonarchWorldData() {}

    // Static method to retrieve the MonarchWorldData instance for a given ServerLevel
    public static MonarchWorldData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            MonarchWorldData::load,
            MonarchWorldData::new,
            DATA_NAME
        );
    }

    // Static method to load MonarchWorldData from a CompoundTag
    public static MonarchWorldData load(CompoundTag tag) {
        MonarchWorldData data = new MonarchWorldData();
        ListTag list = tag.getList("GrantedRegions", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            data.grantedRegions.add(list.getString(i));
        }
        return data;
    }

    // Method to save MonarchWorldData to a CompoundTag
    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag list = new ListTag();
        for (String region : grantedRegions) {
            list.add(StringTag.valueOf(Objects.requireNonNull(region)));
        }
        tag.put("GrantedRegions", list);
        return tag;
    }

    // Method to check if a region has been granted a Hollow Monarch
    public boolean isGranted(String regionKey) {
        return grantedRegions.contains(regionKey);
    }

    // Method to grant a region a Hollow Monarch
    public void grant(String regionKey) {
        if (grantedRegions.add(regionKey)) {
            setDirty();
        }
    }
}
