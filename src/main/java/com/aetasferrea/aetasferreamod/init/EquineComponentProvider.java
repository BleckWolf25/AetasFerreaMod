/**
 * @file EquineComponentProvider.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Jade (Waila) entity component provider that appends equine custom stat tooltips.
 *
 * @description
 * Implements IEntityComponentProvider as a singleton enum to inject custom HUD lines into the Jade tooltip
 * for any AbstractHorse entity, including max health in hearts, taming status, horse class,
 * temper progress, daily feeding count, and specialization XP bars.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.init;

// ---------- IMPORTS
import java.util.Objects;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

// ---------- CLASS: EquineComponentProvider
public enum EquineComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    // ---------- CONSTANTS
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "equine_stats");

    // ---------- TOOLTIP APPEND
    @SuppressWarnings("null")
    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof AbstractHorse horse)) return;

        // ---------- STATS (Health)
        double maxHealth = horse.getMaxHealth();

        tooltip.add(Component.translatable("tooltip.aetasferreamod.horse.health", String.format(java.util.Locale.ROOT, "%.1f", maxHealth), String.format(java.util.Locale.ROOT, "%.1f", maxHealth / 2.0)).withStyle(net.minecraft.ChatFormatting.RED));

        // ---------- TAMING STATUS
        boolean isTamed = horse.isTamed();
        int temper = horse.getTemper();
        int maxTemper = horse.getMaxTemper();

        if (isTamed) {
            tooltip.add(Objects.requireNonNull(Component.translatable("tooltip.aetasferreamod.horse.status").withStyle(net.minecraft.ChatFormatting.GOLD)
                .append(Component.translatable("tooltip.aetasferreamod.horse.status_tamed").withStyle(net.minecraft.ChatFormatting.GREEN))));
        }

        // ---------- CUSTOM EQUINE CLASS INFO
        if (horse instanceof HorseEventHandler aetasHorse) {
            String className = switch (aetasHorse.getHorseClass()) {
                case 1 -> "Rouncey";
                case 2 -> "Destrier";
                case 3 -> "Courser";
                case 4 -> "Palfrey";
                default -> "Wild";
            };
            tooltip.add(Objects.requireNonNull(Component.translatable("tooltip.aetasferreamod.horse.class").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("entity.aetasferreamod.horse." + className.toLowerCase()).withStyle(net.minecraft.ChatFormatting.WHITE))));

            if (!isTamed) {
                tooltip.add(Objects.requireNonNull(Component.translatable("tooltip.aetasferreamod.horse.status").withStyle(net.minecraft.ChatFormatting.GOLD)
                        .append(Component.translatable("tooltip.aetasferreamod.horse.status_untamed_aetas", aetasHorse.getCustomTemper()).withStyle(net.minecraft.ChatFormatting.YELLOW))));
                tooltip.add(Component.translatable("tooltip.aetasferreamod.horse.daily_feeds", aetasHorse.getDailyFood()).withStyle(net.minecraft.ChatFormatting.YELLOW));
            } else if (aetasHorse.getHorseClass() == 1) {
                // Rounceys show training progress toward Destrier and Courser specializations
                tooltip.add(Component.translatable("tooltip.aetasferreamod.horse.combat_xp", Math.max(0, aetasHorse.getCombatXP())).withStyle(net.minecraft.ChatFormatting.YELLOW));
                tooltip.add(Component.translatable("tooltip.aetasferreamod.horse.agility_xp", Math.max(0, aetasHorse.getAgilityXP())).withStyle(net.minecraft.ChatFormatting.AQUA));
            }

        } else if (horse instanceof AetasDonkey donkey) {
            if (!isTamed) {
                tooltip.add(Objects.requireNonNull(Component.translatable("tooltip.aetasferreamod.horse.status").withStyle(net.minecraft.ChatFormatting.GOLD)
                        .append(Component.translatable("tooltip.aetasferreamod.horse.status_untamed_aetas", donkey.getCustomTemper()).withStyle(net.minecraft.ChatFormatting.YELLOW))));
            }
            tooltip.add(Component.translatable("tooltip.aetasferreamod.horse.daily_feeds", donkey.getDailyFood()).withStyle(net.minecraft.ChatFormatting.YELLOW));

        } else if (!isTamed) {
            // Fallback for vanilla horses: use standard temper display
            tooltip.add(Objects.requireNonNull(Component.translatable("tooltip.aetasferreamod.horse.status").withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(Component.translatable("tooltip.aetasferreamod.horse.status_untamed", temper, maxTemper).withStyle(net.minecraft.ChatFormatting.YELLOW))));
        }
    }

    // ---------- UID
    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}