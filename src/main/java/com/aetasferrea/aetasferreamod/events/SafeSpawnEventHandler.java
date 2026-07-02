/**
 * @file SafeSpawnEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Configurable spawn protection for hostile mobs in the Overworld.
 *
 * @description
 * This class listens for the MobSpawnEvent.FinalizeSpawn event and cancels the spawn of hostile mobs (MONSTER category) within a configurable radius around the world spawn point in the Overworld.
 * The radius is defined in chunks, where each chunk is 16x16 blocks. The default safe zone is set to 3 chunks (48x48 blocks).
 *
 * @since 01/07/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: SafeSpawnEventHandler
public class SafeSpawnEventHandler {

    @SubscribeEvent
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Only apply to the Overworld
        ServerLevelAccessor levelAccessor = event.getLevel();
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }

        // Only block the MONSTER category (Hostile mobs)
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }

        // Only block natural spawns (Allows spawners, events, breeding, etc.)
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        // Get the radius from configuration
        int safeZoneChunkRadius = AetasFerreaConfig.SAFE_ZONE_CHUNK_RADIUS.get();
        if (safeZoneChunkRadius <= 0) {
            return; // Feature disabled if set to 0
        }

        // Calculate the distance from the world spawn point
        BlockPos spawnPos = levelAccessor.getLevel().getSharedSpawnPos();
        double mobX = event.getX();
        double mobZ = event.getZ();

        /**
         * Calculate the block radius based on the chunk radius.
         * A chunk is 16 blocks. A 3x3 chunk area is 48x48 blocks.
         * From the exact center, that's 24 blocks in each direction (3 * 16 / 2).
         */
        double blockRadius = (safeZoneChunkRadius * 16.0) / 2.0;

        double distanceX = Math.abs(mobX - spawnPos.getX());
        double distanceZ = Math.abs(mobZ - spawnPos.getZ());

        // If the mob is attempting to spawn within the safe zone, cancel it
        if (distanceX <= blockRadius && distanceZ <= blockRadius) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }
}
