/**
 * @file AetasFerreaItemTagsProvider.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Defines all custom item tags for categorizing weapons, tools, and holdables.
 *
 * @description
 * Generates JSON data for item tags used throughout the mod to evaluate weapon types
 * (e.g. blunt, slashing, high mass), material tiers, and unique interactions (e.g. fox
 * holdables, burning items). These tags allow logic systems to query item capabilities
 * abstractly without hardcoding specific item instances.
 *
 * @since 25/06/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.datagen;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

// ---------- CLASS: AetasFerreaItemTagsProvider
public class AetasFerreaItemTagsProvider extends ItemTagsProvider {

    // ---------- CONSTRUCTOR
    public AetasFerreaItemTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagLookup<Block>> pBlockTags, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, pBlockTags, modId, existingFileHelper);
    }

    // ---------- METHOD: ADD TAGS
    @SuppressWarnings("removal")
    @Override
    protected void addTags(@Nonnull HolderLookup.Provider pProvider) {

        // ---------- TAG GROUP (Blunt Weapons)
        // Groups all weapons that deal crushing or blunt damage
        TagKey<Item> BLUNT_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "blunt_weapons"));
        tag(Objects.requireNonNull(BLUNT_WEAPONS))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_flanged_mace"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_flanged_mace"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_flanged_mace"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_flanged_mace"));

        // ---------- TAG GROUP (Burning Items)
        // Items that act as fire sources or are naturally ablaze
        TagKey<Item> BURNING_ITEMS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "burning_items"));
        tag(Objects.requireNonNull(BURNING_ITEMS))
                .addOptional(new ResourceLocation("minecraft:torch"))
                .addOptional(new ResourceLocation("minecraft:soul_torch"))
                .addOptional(new ResourceLocation("minecraft:campfire"))
                .addOptional(new ResourceLocation("minecraft:soul_campfire"))
                .addOptional(new ResourceLocation("minecraft:lava_bucket"));

        // ---------- TAG GROUP (Diamond Weapons)
        // All weapons constructed from diamond material tier
        TagKey<Item> DIAMOND_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "diamond_weapons"));
        tag(Objects.requireNonNull(DIAMOND_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:diamond_sword"))
                .addOptional(new ResourceLocation("minecraft:diamond_axe"))
                .addOptional(new ResourceLocation("minecraft:diamond_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:diamond_shovel"))
                .addOptional(new ResourceLocation("minecraft:diamond_hoe"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_pike"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_lance"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_flanged_mace"))
                .addOptional(new ResourceLocation("farmersdelight:diamond_knife"));

        // ---------- TAG GROUP (Fishing Junk)
        // Common miscellaneous items fished up from water bodies
        TagKey<Item> FISHING_JUNK = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "fishing_junk"));
        tag(Objects.requireNonNull(FISHING_JUNK))
                .addOptional(new ResourceLocation("minecraft:stick"))
                .addOptional(new ResourceLocation("minecraft:bone"))
                .addOptional(new ResourceLocation("minecraft:lily_pad"))
                .addOptional(new ResourceLocation("minecraft:kelp"))
                .addOptional(new ResourceLocation("minecraft:leather"))
                .addOptional(new ResourceLocation("minecraft:string"))
                .addOptional(new ResourceLocation("minecraft:rotten_flesh"));

        // ---------- TAG GROUP (Fox Holdable)
        // Items that foxes will actively pick up and hold in their mouths
        TagKey<Item> FOX_HOLDABLE = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "fox_holdable"));
        tag(Objects.requireNonNull(FOX_HOLDABLE))
                .addOptional(new ResourceLocation("minecraft:sweet_berries"))
                .addOptional(new ResourceLocation("minecraft:glow_berries"))
                .addOptional(new ResourceLocation("minecraft:rabbit_hide"))
                .addOptional(new ResourceLocation("minecraft:feather"))
                .addOptional(new ResourceLocation("minecraft:egg"))
                .addOptional(new ResourceLocation("minecraft:chicken"))
               .addOptional(new ResourceLocation("minecraft:cooked_chicken"))
                .addOptional(new ResourceLocation("minecraft:rabbit"))
                .addOptional(new ResourceLocation("minecraft:cooked_rabbit"))
                .addOptional(new ResourceLocation("minecraft:rabbit_foot"))
                .addOptional(new ResourceLocation("minecraft:wheat"))
                .addOptional(new ResourceLocation("minecraft:porkchop"))
                .addOptional(new ResourceLocation("minecraft:cooked_porkchop"))
                .addOptional(new ResourceLocation("minecraft:beef"))
                .addOptional(new ResourceLocation("minecraft:cooked_beef"))
                .addOptional(new ResourceLocation("minecraft:mutton"))
                .addOptional(new ResourceLocation("minecraft:cooked_mutton"))
                .addOptional(new ResourceLocation("minecraft:cod"))
                .addOptional(new ResourceLocation("minecraft:salmon"));

        // ---------- TAG GROUP (Golden Weapons)
        // All weapons constructed from gold material tier
        TagKey<Item> GOLDEN_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "golden_weapons"));
        tag(Objects.requireNonNull(GOLDEN_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:golden_sword"))
                .addOptional(new ResourceLocation("minecraft:golden_axe"))
                .addOptional(new ResourceLocation("minecraft:golden_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:golden_shovel"))
                .addOptional(new ResourceLocation("minecraft:golden_hoe"))
                .addOptional(new ResourceLocation("farmersdelight:golden_knife"));

        // ---------- TAG GROUP (High Mass Weapons)
        // Heavy weapons that impart more knockback or have slower swing speeds
        TagKey<Item> HIGH_MASS_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "high_mass_weapons"));
        tag(Objects.requireNonNull(HIGH_MASS_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:wooden_axe"))
                .addOptional(new ResourceLocation("minecraft:stone_axe"))
                .addOptional(new ResourceLocation("minecraft:iron_axe"))
                .addOptional(new ResourceLocation("minecraft:golden_axe"))
                .addOptional(new ResourceLocation("minecraft:diamond_axe"))
                .addOptional(new ResourceLocation("minecraft:netherite_axe"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_battleaxe"));

        // ---------- TAG GROUP (Iron Weapons)
        // All weapons constructed from iron material tier
        TagKey<Item> IRON_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "iron_weapons"));
        tag(Objects.requireNonNull(IRON_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:iron_sword"))
                .addOptional(new ResourceLocation("minecraft:iron_axe"))
                .addOptional(new ResourceLocation("minecraft:iron_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:iron_shovel"))
                .addOptional(new ResourceLocation("minecraft:iron_hoe"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_pike"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_lance"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_flanged_mace"))
                .addOptional(new ResourceLocation("farmersdelight:iron_knife"));

        // ---------- TAG GROUP (Netherite Weapons)
        // All weapons constructed from netherite material tier
        TagKey<Item> NETHERITE_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "netherite_weapons"));
        tag(Objects.requireNonNull(NETHERITE_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:netherite_sword"))
                .addOptional(new ResourceLocation("minecraft:netherite_axe"))
                .addOptional(new ResourceLocation("minecraft:netherite_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:netherite_shovel"))
                .addOptional(new ResourceLocation("minecraft:netherite_hoe"))
                .addOptional(new ResourceLocation("farmersdelight:netherite_knife"));

        // ---------- TAG GROUP (Slashing Weapons)
        // Weapons specialized for cutting or dealing sweep damage
        TagKey<Item> SLASHING_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "slashing_weapons"));
        tag(Objects.requireNonNull(SLASHING_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:wooden_sword"))
                .addOptional(new ResourceLocation("minecraft:stone_sword"))
                .addOptional(new ResourceLocation("minecraft:iron_sword"))
                .addOptional(new ResourceLocation("minecraft:golden_sword"))
                .addOptional(new ResourceLocation("minecraft:diamond_sword"))
                .addOptional(new ResourceLocation("minecraft:netherite_sword"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_parrying_dagger"))

                .addOptional(new ResourceLocation("spartanweaponry:diamond_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:iron_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:diamond_greatsword"))
                .addOptional(new ResourceLocation("farmersdelight:flint_knife"))
                .addOptional(new ResourceLocation("farmersdelight:iron_knife"))
                .addOptional(new ResourceLocation("farmersdelight:diamond_knife"))
                .addOptional(new ResourceLocation("farmersdelight:golden_knife"))
                .addOptional(new ResourceLocation("farmersdelight:netherite_knife"));

        // ---------- TAG GROUP (Stone Weapons)
        // All weapons constructed from stone material tier
        TagKey<Item> STONE_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "stone_weapons"));
        tag(Objects.requireNonNull(STONE_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:stone_sword"))
                .addOptional(new ResourceLocation("minecraft:stone_axe"))
                .addOptional(new ResourceLocation("minecraft:stone_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:stone_shovel"))
                .addOptional(new ResourceLocation("minecraft:stone_hoe"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_pike"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_lance"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:stone_flanged_mace"))
                .addOptional(new ResourceLocation("farmersdelight:flint_knife"));

        // ---------- TAG GROUP (Wooden Weapons)
        // All weapons constructed from wood material tier
        TagKey<Item> WOODEN_WEAPONS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "wooden_weapons"));
        tag(Objects.requireNonNull(WOODEN_WEAPONS))
                .addOptional(new ResourceLocation("minecraft:wooden_sword"))
                .addOptional(new ResourceLocation("minecraft:wooden_axe"))
                .addOptional(new ResourceLocation("minecraft:wooden_pickaxe"))
                .addOptional(new ResourceLocation("minecraft:wooden_shovel"))
                .addOptional(new ResourceLocation("minecraft:wooden_hoe"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_parrying_dagger"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_longsword"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_greatsword"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_warhammer"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_halberd"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_pike"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_lance"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_battleaxe"))
                .addOptional(new ResourceLocation("spartanweaponry:wooden_flanged_mace"));
    }
}
