/**
 * @file AetasFerreaBiomeTagsProvider.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Defines custom biome tags to control animal spawn behaviors and rules.
 *
 * @description
 * Generates JSON data for custom biome tags. Used to restrict vanilla animal
 * spawns to specific logical biome groups (e.g. pigs restricted to forests/swamps,
 * sheep restricted to mountains/hills) to enhance immersive spawning.
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
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// ---------- CLASS: AetasFerreaBiomeTagsProvider
public class AetasFerreaBiomeTagsProvider extends BiomeTagsProvider {

    // ---------- CONSTRUCTOR
    public AetasFerreaBiomeTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pProvider, modId, existingFileHelper);
    }

    // ---------- METHOD: ADD TAGS
    @SuppressWarnings("removal")
    @Override
    protected void addTags(@Nonnull HolderLookup.Provider pProvider) {

        // ---------- TAG GROUP (Pig Habitats)
        // Constrain pigs to dense, muddy, or wooded environments
        TagKey<Biome> PIG_HABITATS = TagKey.create(Objects.requireNonNull(net.minecraft.core.registries.Registries.BIOME), new ResourceLocation(AetasFerreaMod.MODID, "pig_habitats"));
        tag(Objects.requireNonNull(PIG_HABITATS))
                .addOptionalTag(new ResourceLocation("minecraft:is_forest"))
                .addOptionalTag(new ResourceLocation("minecraft:is_swamp"));

        // ---------- TAG GROUP (Sheep Habitats)
        // Constrain sheep to high altitude, craggy, or rolling environments
        TagKey<Biome> SHEEP_HABITATS = TagKey.create(Objects.requireNonNull(net.minecraft.core.registries.Registries.BIOME), new ResourceLocation(AetasFerreaMod.MODID, "sheep_habitats"));
        tag(Objects.requireNonNull(SHEEP_HABITATS))
                .addOptionalTag(new ResourceLocation("minecraft:is_mountain"))
                .addOptionalTag(new ResourceLocation("minecraft:is_hill"));
    }
}
