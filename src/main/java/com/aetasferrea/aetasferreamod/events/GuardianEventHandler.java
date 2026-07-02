/**
 * @file GuardianEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Transformation and drop handler for Aetas Ferrea Nether Guardians.
 *
 * @description
 * Transforms skeletons into Vanguards and handles their custom drops.
 * Utilizes safe fallbacks for Spartan Weaponry and Pehkui to ensure zero hard dependencies.
 *
 * @since 30/06/2026
 * @updated 02/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.entity.boss.VanguardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

// ---------- CLASS: GUARDIAN EVENT HANDLER
@SuppressWarnings({"removal"})
@Mod.EventBusSubscriber(modid = "aetasferreamod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuardianEventHandler {

    public static final String VANGUARD_TAG = "Aetas_IsVanguard";

    public static boolean isVanguard(net.minecraft.world.entity.Entity entity) {
        return entity != null && (entity.getTags().contains("aetas_vanguard") || entity.getPersistentData().getBoolean(VANGUARD_TAG));
    }

    /**
     * Transforms a freshly spawned (or summoned) AbstractSkeleton into an Aetas Vanguard.
     * Extracted as a public static helper so other handlers (e.g. MonarchEventHandler) can
     * reuse the exact same tagging/attribute/equipment/scaling logic when summoning escorts.
     *
     * @param skeleton The skeleton entity to transform. Must already be added to the level.
     */
    public static void transformToVanguard(AbstractSkeleton skeleton) {
        if (skeleton == null) return;
        // Tags
        skeleton.addTag("aetas_vanguard");
        skeleton.getPersistentData().putBoolean(VANGUARD_TAG, true);
        stripEnhancedAI(skeleton);
        skeleton.setCustomName(net.minecraft.network.chat.Component.literal("Vanguard").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        skeleton.setCustomNameVisible(false); // Keeps the name plate from showing constantly, but still syncs it to client

        // Attributes
        var maxHealth = skeleton.getAttribute(Objects.requireNonNull(Attributes.MAX_HEALTH));
        if (maxHealth != null) maxHealth.setBaseValue(80.0D); // 40 Hearts
        skeleton.setHealth(80.0F);

        var attackDamage = skeleton.getAttribute(Objects.requireNonNull(Attributes.ATTACK_DAMAGE));
        if (attackDamage != null) attackDamage.setBaseValue(8.0D);

        var knockbackResist = skeleton.getAttribute(Objects.requireNonNull(Attributes.KNOCKBACK_RESISTANCE));
        if (knockbackResist != null) knockbackResist.setBaseValue(0.5D);

        // Movement Speed - Slower movement speed (vanilla skeleton is 0.25)
        var movementSpeed = skeleton.getAttribute(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
        if (movementSpeed != null) movementSpeed.setBaseValue(0.18D);

        // Equipment
        Item spearItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "iron_spear"));
        if (spearItem == null || spearItem == Items.AIR) {
            spearItem = Items.IRON_SWORD; // Vanilla fallback if Spartan Weaponry is missing
        }

        // Equip the boss (vanguard has only iron chestplate, other slots empty)
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Objects.requireNonNull(spearItem)));
        skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Objects.requireNonNull(Items.ENDER_EYE)));
        skeleton.setItemSlot(EquipmentSlot.HEAD, Objects.requireNonNull(ItemStack.EMPTY));
        skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Objects.requireNonNull(Items.IRON_CHESTPLATE)));
        skeleton.setItemSlot(EquipmentSlot.LEGS, Objects.requireNonNull(ItemStack.EMPTY));
        skeleton.setItemSlot(EquipmentSlot.FEET, Objects.requireNonNull(ItemStack.EMPTY));

        // Force drop chances to 0.0F so they don't clutter the ground with damaged iron armor
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        skeleton.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
        skeleton.setDropChance(EquipmentSlot.CHEST, 0.0F);
        skeleton.setDropChance(EquipmentSlot.LEGS, 0.0F);
        skeleton.setDropChance(EquipmentSlot.FEET, 0.0F);

        // Remove fire vulnerability pathfinding or behaviors if any exist
        skeleton.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.LAVA, 0.0F);
        skeleton.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_FIRE, 0.0F);
        skeleton.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DAMAGE_FIRE, 0.0F);

        // Scaling (default size -> 1.2F)
        applyBossScale(skeleton, 1.2F);
    }

    /**
     * Prevents Enhanced AI, Improved Mobs, and InsaneLib from modifying the AI or tags
     * of Hollow Monarch and Vanguards.
     */
    public static void stripEnhancedAI(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        boolean isProtected = com.aetasferrea.aetasferreamod.client.ClientEvents.isHollowMonarch(entity) || isVanguard(entity);
        if (!isProtected) return;

        var nbt = entity.getPersistentData();
        if (!nbt.getBoolean("enhancedai:unaffected_by_features")) {
            nbt.putBoolean("enhancedai:unaffected_by_features", true);
        }
        if (!nbt.getBoolean("improvedmobs:ignore")) {
            nbt.putBoolean("improvedmobs:ignore", true);
        }

        // Remove any tags added by Enhanced AI or Improved Mobs
        for (String key : new java.util.ArrayList<>(nbt.getAllKeys())) {
            if ((key.startsWith("enhancedai") || key.startsWith("improvedmobs") || key.startsWith("insane"))
                && !"enhancedai:unaffected_by_features".equals(key) && !"improvedmobs:ignore".equals(key)) {
                nbt.remove(key);
            }
        }

        // Strip any custom AI goals added by those mods
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.goalSelector.getAvailableGoals().removeIf(wrapped -> {
                String className = wrapped.getGoal().getClass().getName().toLowerCase(java.util.Locale.ROOT);
                return className.contains("enhancedai") || className.contains("insane") || className.contains("improvedmobs");
            });
            mob.targetSelector.getAvailableGoals().removeIf(wrapped -> {
                String className = wrapped.getGoal().getClass().getName().toLowerCase(java.util.Locale.ROOT);
                return className.contains("enhancedai") || className.contains("insane") || className.contains("improvedmobs");
            });
        }
    }

    public static void allowEnhancedAI(net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        var nbt = entity.getPersistentData();
        if (nbt.getBoolean("enhancedai:unaffected_by_features")) {
            nbt.remove("enhancedai:unaffected_by_features");
        }
        if (nbt.getBoolean("improvedmobs:ignore")) {
            nbt.remove("improvedmobs:ignore");
        }
    }

    /**
     * Applies scaling to a boss entity using Pehkui API reflection if available at runtime,
     * with fallback to multiple command variations using the entity's command source stack.
     */
    public static void applyBossScale(net.minecraft.world.entity.LivingEntity entity, float scale) {
        if (entity.level().isClientSide) return;

        stripEnhancedAI(entity);

        boolean appliedViaApi = false;
        try {
            // Attempt direct Pehkui API invocation via reflection without hard compile-time dependency
            Class<?> scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Object baseScaleType = scaleTypesClass.getField("BASE").get(null);
            java.lang.reflect.Method getScaleData = baseScaleType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);
            Object scaleData = getScaleData.invoke(baseScaleType, entity);
            if (scaleData != null) {
                java.lang.reflect.Method setScale = scaleData.getClass().getMethod("setScale", float.class);
                setScale.invoke(scaleData, scale);
                try {
                    java.lang.reflect.Method setTargetScale = scaleData.getClass().getMethod("setTargetScale", float.class);
                    setTargetScale.invoke(scaleData, scale);
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method setBaseScale = scaleData.getClass().getMethod("setBaseScale", float.class);
                    setBaseScale.invoke(scaleData, scale);
                } catch (Throwable ignored) {}
                // Zero out scale transition / animation tick delay to prevent spawn animation interpolation loops
                try {
                    java.lang.reflect.Method setScaleTickDelay = scaleData.getClass().getMethod("setScaleTickDelay", int.class);
                    setScaleTickDelay.invoke(scaleData, 0);
                } catch (Throwable ignored) {}
                // Force network synchronization immediately
                try {
                    java.lang.reflect.Method markForSync = scaleData.getClass().getMethod("markForSync", boolean.class);
                    markForSync.invoke(scaleData, true);
                } catch (Throwable ignored) {}
                appliedViaApi = true;
            }
        } catch (Throwable ignored) {
            // Pehkui API not present or method changed; fall back to commands
        }

        if (appliedViaApi) {
            return;
        }

        // Only execute fallback commands if API reflection was not available
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && serverLevel.getServer() != null) {
            var commands = serverLevel.getServer().getCommands();
            var serverSource = serverLevel.getServer().createCommandSourceStack();
            String uuidStr = entity.getStringUUID();
            commands.performPrefixedCommand(Objects.requireNonNull(serverSource), Objects.requireNonNull(String.format(java.util.Locale.ROOT, "scale set %.2f %s", scale, uuidStr)));
            commands.performPrefixedCommand(Objects.requireNonNull(serverSource), Objects.requireNonNull(String.format(java.util.Locale.ROOT, "scale set pehkui:base %.2f %s", scale, uuidStr)));
        }
    }

    /**
     * Spawns a fully configured Nether Guardian at the specified position.
     *
     * @param level The server level to spawn in.
     * @param pos   The position to spawn the Guardian at.
     * @return The created AbstractSkeleton entity, or null if creation failed.
     */
    public static AbstractSkeleton spawnNetherGuardian(ServerLevel level, BlockPos pos) {
        return spawnVanguard(level, pos, false);
    }

    public static AbstractSkeleton spawnVanguard(ServerLevel level, BlockPos pos, boolean isRoyalEscort) {
        if (level == null || pos == null) return null;
        AbstractSkeleton skeleton = net.minecraft.world.entity.EntityType.WITHER_SKELETON.create(level);
        if (skeleton == null) return null;

        skeleton.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        transformToVanguard(skeleton);
        if (isRoyalEscort) {
            skeleton.getPersistentData().putBoolean("IsRoyalEscort", true);
        }
        level.addFreshEntity(skeleton);

        return skeleton;
    }

    // Drops - Make Guardians drop Eyes of Ender
    @SubscribeEvent
    public static void onGuardianDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof VanguardEntity vanguard) {
            // Clear standard skeleton drops (bones, arrows, coal, etc.)
            event.getDrops().clear();

            // Drop 2 to 4 Eyes of Ender (guarantee >= 2)
            int eyeCount = 2 + vanguard.level().random.nextInt(3);
            ItemStack eyes = new ItemStack(Objects.requireNonNull(Items.ENDER_EYE), eyeCount);

            ItemEntity eyeEntity = new ItemEntity(Objects.requireNonNull(vanguard.level()), vanguard.getX(), vanguard.getY(), vanguard.getZ(), eyes);
            event.getDrops().add(eyeEntity);
        }
    }
}
