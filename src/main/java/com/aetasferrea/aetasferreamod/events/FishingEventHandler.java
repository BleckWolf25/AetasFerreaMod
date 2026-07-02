/**
 * @file FishingEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Subscribes to player fishing events to filter fish drops by biome.
 *
 * @description
 * Listens for item fished events to validate that any caught fish are native to the biome
 * where they were fished, replacing non-native fish drops with random items from a junk pool.
 *
 * @since 25/06/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.List;
import java.util.Objects;

// ---------- CLASS: FISHINGEVENTHANDLER
public class FishingEventHandler {

    // ---------- CONSTANTS
    @SuppressWarnings("removal")
    private static final TagKey<Item> JUNK_TAG = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "fishing_junk"));

    // ---------- FISH IDENTIFICATION
    private static boolean isFish(Item item) {
        return item == Items.COD || item == Items.SALMON || item == Items.PUFFERFISH || item == Items.TROPICAL_FISH;
    }

    // ---------- BIOME VALIDATION
    private static boolean isValidBiomeForFish(Item fish, Holder<Biome> biome) {
        if (fish == Items.COD) {
            return biome.is(Objects.requireNonNull(BiomeTags.IS_OCEAN));
        }

        if (fish == Items.SALMON) {
            return biome.is(Objects.requireNonNull(BiomeTags.IS_OCEAN)) || biome.is(Objects.requireNonNull(BiomeTags.IS_RIVER));
        }

        // Require warm oceans or jungles for pufferfish and tropical fish
        if (fish == Items.PUFFERFISH || fish == Items.TROPICAL_FISH) {
            boolean isWarmOcean = biome.is(Objects.requireNonNull(BiomeTags.IS_OCEAN)) && biome.is(Objects.requireNonNull(BiomeTags.HAS_OCEAN_RUIN_WARM));
            return isWarmOcean || biome.is(Objects.requireNonNull(BiomeTags.IS_JUNGLE));
        }

        return true;
    }

    // ---------- FISH LOOT REPLACEMENT
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        FishingHook hook = event.getHookEntity();
        if (hook == null) {
            return;
        }

        Level level = hook.level();
        BlockPos pos = hook.blockPosition();
        if (pos == null) {
            return;
        }

        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) {
            return;
        }

        Holder<Biome> biomeHolder = level.getBiome(Objects.requireNonNull(pos));

        // ---------- LOOT CHECK (Scan drops and replace invalid catches with junk)
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            Item item = stack.getItem();
            if (!isFish(item)) {
                continue;
            }

            if (isValidBiomeForFish(item, biomeHolder)) {
                continue;
            }

            // Replace invalid fish drop with a random item from the junk pool
            var tag = ForgeRegistries.ITEMS.tags().getTag(JUNK_TAG);
            Item junkItem = tag.isEmpty() ? Items.STICK : tag.getRandomElement(level.getRandom()).orElse(Items.STICK);
            drops.set(i, new ItemStack(Objects.requireNonNull(junkItem)));
        }
    }
}
