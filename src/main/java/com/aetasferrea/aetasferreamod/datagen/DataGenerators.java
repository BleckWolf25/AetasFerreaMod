/**
 * @file DataGenerators.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Registers and gathers all custom data generation providers for the mod.
 *
 * @description
 * Hooks into the Forge data gathering event to register providers for blocks, items,
 * and biomes. This ensures all generated JSON tags and assets are properly injected
 * into the build process when running the runData task.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.datagen;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.concurrent.CompletableFuture;

// ---------- CLASS: DataGenerators
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    // ---------- EVENT HANDLER: DATA GATHERING
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Assign block tags provider first to guarantee item tags can properly copy shared hierarchies
        AetasFerreaBlockTagsProvider blockTagsProvider = generator.addProvider(
                event.includeServer(),
                new AetasFerreaBlockTagsProvider(packOutput, lookupProvider, AetasFerreaMod.MODID, existingFileHelper)
        );

        // Inject the block tags getter so item tags can mirror block tag structures
        generator.addProvider(
                event.includeServer(),
                new AetasFerreaItemTagsProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), AetasFerreaMod.MODID, existingFileHelper)
        );

        // Attach standalone biome tags provider for world generation adjustments
        generator.addProvider(
                event.includeServer(),
                new AetasFerreaBiomeTagsProvider(packOutput, lookupProvider, AetasFerreaMod.MODID, existingFileHelper)
        );
    }
}
