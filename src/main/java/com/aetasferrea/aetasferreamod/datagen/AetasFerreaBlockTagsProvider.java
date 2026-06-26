/**
 * @file AetasFerreaBlockTagsProvider.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Defines custom block tags for world interactions and tool requirements.
 *
 * @description
 * Generates JSON data for block tags. Currently serves as a stub for future block
 * tag implementation, ensuring the infrastructure exists for data generation when
 * new blocks or material properties are added.
 *
 * @since 25/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.datagen;

// ---------- IMPORTS
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.CompletableFuture;

// ---------- CLASS: AetasFerreaBlockTagsProvider
public class AetasFerreaBlockTagsProvider extends BlockTagsProvider {

    // ---------- CONSTRUCTOR
    public AetasFerreaBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    // ---------- METHOD: ADD TAGS
    @SuppressWarnings("null")
    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        // No block tags for now
    }
}
