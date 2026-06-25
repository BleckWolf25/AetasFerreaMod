/**
 * @file AttributeTooltipEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Intercepts and reformats item attribute tooltips to align with vanilla standards.
 *
 * @description
 * Subscribes to the client-side ItemTooltipEvent to scan and purge duplicate vanilla modifier lines
 * and custom item attribute text blocks, reconstructs the tooltips with slot-aware color-coded formatting,
 * and formats small modifier values cleanly to prevent rounding artifacts.
 *
 * @since 24/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

// ---------- CLASS: ATTRIBUTETOOLTIPEVENTHANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttributeTooltipEventHandler {

    // ---------- CONSTANTS
    private static final Pattern ATTRIBUTE_LINE_PATTERN = Pattern.compile("^\\s*[+-]?\\s*\\d+(?:\\.\\d+)?%?\\s+(.*)$");

    // ---------- ALWAYS ABSOLUTE CHECK
    private static boolean isAlwaysAbsoluteAttribute(String descId) {
        if (descId == null) {
            return false;
        }

        // Match generic armor or armor toughness translation IDs
        return descId.contains("generic.armor")
            || descId.equals("minecraft:generic.armor")
            || descId.equals("minecraft:generic.armor_toughness");
    }

    // ---------- WEAPON ABSOLUTE CHECK
    private static boolean isWeaponAbsoluteAttribute(Attribute attribute) {
        if (attribute == null) {
            return false;
        }

        String descId = attribute.getDescriptionId();
        if (descId == null) {
            return false;
        }

        ResourceLocation regKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        String regPath = regKey != null ? regKey.toString() : "";

        // Check translation key and registry ID to classify damage, speed, or range
        return descId.contains("attack_damage") || regPath.contains("attack_damage")
            || descId.contains("attack_speed")  || regPath.contains("attack_speed")
            || descId.contains("attack_range")  || regPath.contains("attack_range")
            || descId.contains("entity_reach")  || regPath.contains("entity_reach");
    }

    // ---------- VALUE FORMATTING
    private static String formatValue(double value) {
        // Return pure integer representation if there is no decimal component
        if (value == (long) value) {
            return String.format(Locale.ROOT, "%d", (long) value);
        }

        String formatted = String.format(Locale.ROOT, "%.2f", value);
        // Crop trailing zeros to match clean vanilla style
        if (formatted.endsWith(".00")) {
            return formatted.substring(0, formatted.length() - 3);
        }

        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }

        return formatted;
    }

    // ---------- MODIFIER DELTA LINE BUILDER
    private static Component buildModifierLine(double delta, @Nonnull String descId) {
        // Omit showing modifiers with zero delta value
        if (Math.abs(delta) < 0.0001) {
            return null;
        }

        ChatFormatting color = delta < 0 ? ChatFormatting.RED : ChatFormatting.GREEN;
        String formattedValue;

        // Show as percentage for tiny values to avoid negative zero rounding artifacts
        if (Math.abs(delta) < 0.1) {
            double percent = delta * 100.0;
            String percentStr = formatValue(percent);
            if (delta > 0) {
                percentStr = "+" + percentStr;
            }
            formattedValue = percentStr + "%";
        } else {
            formattedValue = formatValue(delta);
            if (delta > 0) {
                formattedValue = "+" + formattedValue;
            }
        }

        return Component.translatable("attribute.modifier.equals.0",
            formattedValue,
            Component.translatable(descId)
        ).withStyle(color);
    }

    // ---------- ABSOLUTE VALUE LINE BUILDER
    private static Component buildAbsoluteLine(double finalValue, @Nonnull String descId) {
        // Omit showing absolute stats that are zero
        if (Math.abs(finalValue) < 0.0001) {
            return null;
        }

        ChatFormatting color = finalValue < 0 ? ChatFormatting.RED : ChatFormatting.BLUE;
        String formattedValue = formatValue(finalValue);

        return Component.translatable("attribute.modifier.equals.0",
            formattedValue,
            Component.translatable(descId)
        ).withStyle(color);
    }

    // ---------- MODIFIER DELTA COMPUTATION
    private static double[] computeValues(@Nonnull Attribute attribute, Collection<AttributeModifier> mods, Player player) {
        double base = (player != null && player.getAttributes().hasAttribute(attribute))
                      ? player.getAttributeBaseValue(attribute)
                      : attribute.getDefaultValue();

        double additions = 0.0;
        double multiplyBase = 0.0;
        double multiplyTotal = 1.0;

        for (AttributeModifier modifier : mods) {
            switch (modifier.getOperation()) {
                case ADDITION       -> additions    += modifier.getAmount();
                case MULTIPLY_BASE  -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal *= (1.0 + modifier.getAmount());
            }
        }

        double finalValue = (base + additions) * (1.0 + multiplyBase) * multiplyTotal;
        double totalDelta = finalValue - base;
        return new double[]{totalDelta, finalValue};
    }

    // ---------- ITEM TOOLTIP INTERCEPTION
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }

        List<Component> tooltips = event.getToolTip();
        if (tooltips == null) {
            return;
        }

        Player player = event.getEntity();

        // ---------- TOOLTIP SCAN (Identify and mark old attribute/CIA lines to remove)
        List<String> slotHeaders = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            slotHeaders.add(Component.translatable("item.modifiers." + slot.getName()).getString());
        }

        // Collect translated names of all active attributes on this item for matching
        Set<String> activeAttributeNames = new HashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(Objects.requireNonNull(slot));
            for (Attribute attribute : modifiers.keySet()) {
                if (attribute != null && attribute.getDescriptionId() != null) {
                    activeAttributeNames.add(
                        Component.translatable(Objects.requireNonNull(attribute.getDescriptionId()))
                            .getString().toLowerCase(Locale.ROOT)
                    );
                }
            }
        }

        List<Integer> linesToRemove = new ArrayList<>();

        for (int i = 0; i < tooltips.size(); i++) {
            String text = tooltips.get(i).getString();

            // Check and mark slot headers
            boolean isHeader = false;
            for (String header : slotHeaders) {
                if (text.equals(header)) {
                    isHeader = true;
                    break;
                }
            }

            if (isHeader) {
                linesToRemove.add(i);
                if (i > 0 && tooltips.get(i - 1).getString().trim().isEmpty()) {
                    if (!linesToRemove.contains(i - 1)) {
                        linesToRemove.add(i - 1);
                    }
                }
                continue;
            }

            // Check and mark existing attribute value lines
            Matcher matcher = ATTRIBUTE_LINE_PATTERN.matcher(text);
            if (matcher.matches()) {
                String remainder = matcher.group(1).toLowerCase(Locale.ROOT).trim();
                for (String activeName : activeAttributeNames) {
                    if (activeName.contains(remainder) || remainder.contains(activeName)) {
                        linesToRemove.add(i);
                        if (i > 0 && tooltips.get(i - 1).getString().trim().isEmpty()) {
                            if (i - 1 > 0 && (tooltips.get(i - 2).getString().startsWith(" ")
                                    || slotHeaders.contains(tooltips.get(i - 2).getString()))) {
                                if (!linesToRemove.contains(i - 1)) {
                                    linesToRemove.add(i - 1);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        // Remove marked lines in reverse order to preserve indexes
        Collections.sort(linesToRemove, Collections.reverseOrder());
        for (int index : linesToRemove) {
            if (index >= 0 && index < tooltips.size()) {
                tooltips.remove(index);
            }
        }

        // ---------- REBUILD (Slot-aware formatting: absolute for weapons, delta for armor)
        Set<String> processedAttributes = new HashSet<>();
        List<Component> newTooltipLines = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(Objects.requireNonNull(slot));
            if (modifiers.isEmpty()) {
                continue;
            }

            boolean isWeaponSlot = (slot == EquipmentSlot.MAINHAND);

            for (Attribute attribute : modifiers.keySet()) {
                if (attribute == null) {
                    continue;
                }
                String descId = attribute.getDescriptionId();
                if (descId == null) {
                    continue;
                }

                // Skip if already processed from another slot
                if (!processedAttributes.add(descId)) {
                    continue;
                }

                double[] values = computeValues(attribute, modifiers.get(attribute), player);
                double totalDelta = values[0];
                double finalValue = values[1];

                // ---------- DISPLAY STRATEGY (Select absolute or delta display style based on attribute and slot)
                Component line;
                if (isAlwaysAbsoluteAttribute(descId)) {
                    line = buildAbsoluteLine(finalValue, descId);
                } else if (isWeaponSlot && isWeaponAbsoluteAttribute(attribute)) {
                    line = buildAbsoluteLine(finalValue, descId);
                } else {
                    line = buildModifierLine(totalDelta, descId);
                }

                if (line != null) {
                    newTooltipLines.add(line);
                }
            }
        }

        tooltips.addAll(newTooltipLines);
    }
}
