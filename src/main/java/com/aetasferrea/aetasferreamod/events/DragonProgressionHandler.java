/**
 * @file DragonProgressionHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Blocks crafting/smithing legendary gear before slaying the Ender Dragon.
 *
 * @description
 * Blocks players from crafting or smithing advanced legendary gear (like Ornstein or Dragonslayer sets)
 * before they have completed the End Dragon kill advancement.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

// ---------- CLASS: DRAGON PROGRESSION HANDLER

public class DragonProgressionHandler {

    // Cache the heavy objects so they are only instantiated once when the mod loads
    private static final ResourceLocation END_DRAGON_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath("minecraft", "end/kill_dragon");
    private static final Component WARNING_MESSAGE = Component.translatable("message.aetasferreamod.dragon.lack_experience").withStyle(net.minecraft.ChatFormatting.RED);

    // ---------- TICK EVENT HANDLER (SMITHING BLOCKER)
    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Intercept the item before the player's client has a chance to click it this tick
        if (event.phase == TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) return;

        if (serverPlayer.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu smithingMenu) {
            ItemStack result = smithingMenu.getSlot(3).getItem();
            
            if (!result.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(result.getItem());
                if (id == null) return;
                
                // Compare the ResourceLocation properties directly (infinitely faster than building strings)
                if (id.getNamespace().equals("fantasy_armor") && (id.getPath().contains("ornstein") || id.getPath().contains("dragonslayer"))) {
                    
                    Advancement advancement = serverPlayer.server.getAdvancements().getAdvancement(END_DRAGON_ADVANCEMENT);
                    
                    if (advancement != null && !serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
                        
                        smithingMenu.getSlot(3).set(ItemStack.EMPTY);
                        
                        if (serverPlayer.tickCount % 40 == 0) {
                            serverPlayer.displayClientMessage(WARNING_MESSAGE, true);
                        }
                    }
                }
            }
        }
    }
}
