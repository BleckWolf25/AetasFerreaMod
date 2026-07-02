/**
 * @file MobSpawnEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Subscribes to mob spawn position events to apply custom spawning rules.
 *
 * @description
 * Enforces environmental restrictions on entity spawns, including surface limits for
 * spiders based on biomes, and depth limits for squids to prevent shallow puddle spawns.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import java.util.Objects;
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: MOBSPAWNEVENTHANDLER
public class MobSpawnEventHandler {

    // ---------- MOB SPAWN POSITION CHECK
    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        Mob entity = event.getEntity();
        // Guard against null entity references
        if (entity == null) {
            return;
        }

        LevelAccessor level = event.getLevel();
        BlockPos pos = new BlockPos((int) event.getX(), (int) event.getY(), (int) event.getZ());
        Holder<Biome> biome = level.getBiome(pos);

        // ---------- SPIDERS (Surface biome validation)
        if (entity instanceof Spider) {
            // Check if it is a surface spawn roughly at or above configured Y
            if (pos.getY() >= AetasFerreaConfig.SPIDER_SURFACE_Y.get()) {
                boolean isValidBiome = biome.is(Objects.requireNonNull(BiomeTags.IS_FOREST)) || biome.is(Objects.requireNonNull(BiomeTags.IS_JUNGLE));
                if (!isValidBiome) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
        }

        // ---------- SQUIDS (Water depth validation)
        if (entity instanceof Squid) {
            // Check the block two spaces below the spawn point to ensure depth
            BlockPos belowPos = pos.below(2);
            if (!level.getBlockState(Objects.requireNonNull(belowPos)).is(Objects.requireNonNull(Blocks.WATER))) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
    }
}
