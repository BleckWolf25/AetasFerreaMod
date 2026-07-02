/**
 * @file MiniBossManager.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Spawns and configures progressive mini-bosses.
 *
 * @description
 * Manages the scheduling, generation, equipment configuration, and spawning of mini-bosses
 * (e.g., Catena-Mail Vigil, Defiled Castellan, Dead Iron Knight, Diamond Knight) based on the world age (days).
 *
 * @since 20/05/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.boss;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.difficulty.AetasFerreaSavedData;
import com.aetasferrea.aetasferreamod.difficulty.WorldAgeTracker;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

@SuppressWarnings("null")
// ---------- CLASS: MINI BOSS MANAGER

public class MiniBossManager {

    // ---------- TICK EVENT HANDLER (SPAWN SCHEDULER)
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel level)) return;

        long days = WorldAgeTracker.getWorldDays(level);
        boolean isNight = level.isNight();

        // Mini-boss spawning is strictly restricted to night hours
        if (!isNight) return;

        AetasFerreaSavedData data = AetasFerreaSavedData.get(level);
        if (data.getLastSpawnedDay() >= days) return; // Mini-boss already spawned for tonight

        boolean success = false;

        // Schedule spawns based on the age of the world
        if (days == 4) {
            // Day 4: Spawn Catena-Mail Vigil
            success = spawnBossNearRandomPlayer(level, "vigil");
        } else if (days == 14) {
            // Day 14: Spawn Defiled Castellan
            success = spawnBossNearRandomPlayer(level, "castellan");
        } else if (days == 28) {
            // Day 28: Spawn unenchanted Dead Iron Knight
            success = spawnBossNearRandomPlayer(level, "knight_unenchanted");
        } else if (days >= 21 && days % 4 == 0) {
            // Day 21+ (every 4th day): Spawn randomized combinations of bosses
            boolean spawnKnight = (days >= 31);
            boolean spawnDouble = level.random.nextFloat() < 0.50f;

            if (spawnKnight && spawnDouble) {
                success = spawnBossNearRandomPlayer(level, "knight_enchanted_and_double");
            } else if (spawnKnight) {
                success = spawnBossNearRandomPlayer(level, "knight");
            } else if (spawnDouble) {
                success = spawnBossNearRandomPlayer(level, "both_vigil_castellan");
            } else {
                // No spawn rolled for tonight, but mark the day as checked
                success = true;
            }
        }

        if (success) {
            data.setLastSpawnedDay((int) days);
        }
    }

    // ---------- BOSS GENERATION API

    /**
     * Finds a random online player in the level and spawns a mini-boss near them.
     */
    public static boolean spawnBossNearRandomPlayer(ServerLevel level, String bossType) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return false;

        // Pick a random player
        ServerPlayer player = players.get(level.random.nextInt(players.size()));

        // Find a safe surface position between 24 and 40 blocks from the player
        BlockPos spawnPos = findSafeSpawnPosition(level, player.blockPosition());
        if (spawnPos == null) return false;

        return spawnBossAtPosition(level, spawnPos, bossType, player);
    }

    /**
     * Spawns the specified boss type at a block position and notifies the targeted player.
     */
    public static boolean spawnBossAtPosition(ServerLevel level, BlockPos spawnPos, String bossType, ServerPlayer targetPlayer) {
        if (bossType.equals("vigil")) {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton != null) {
                skeleton.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeCatenaVigil(skeleton, level);
                level.addFreshEntity(skeleton);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_vigil").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("castellan")) {
            Zombie husk = EntityType.HUSK.create(level);
            if (husk != null) {
                husk.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDefiledCastellan(husk, level);
                level.addFreshEntity(husk);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_castellan").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("knight_unenchanted")) {
            Zombie husk = EntityType.HUSK.create(level);
            if (husk != null) {
                husk.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDeadIronKnight(husk, level, false);
                level.addFreshEntity(husk);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_iron_knight").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("knight")) {
            Zombie husk = EntityType.HUSK.create(level);
            if (husk != null) {
                husk.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDeadIronKnight(husk, level, true);
                level.addFreshEntity(husk);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_iron_knight").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("diamond_knight")) {
            Zombie husk = EntityType.HUSK.create(level);
            if (husk != null) {
                husk.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDiamondKnight(husk, level);
                level.addFreshEntity(husk);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_diamond_knight").withStyle(net.minecraft.ChatFormatting.AQUA));
                return true;
            }
        } else if (bossType.equals("both_vigil_castellan")) {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            Zombie husk = EntityType.HUSK.create(level);
            if (skeleton != null && husk != null) {
                BlockPos spawnPos2 = targetPlayer != null ? findSafeSpawnPosition(level, targetPlayer.blockPosition()) : spawnPos;
                if (spawnPos2 == null) spawnPos2 = spawnPos;

                skeleton.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeCatenaVigil(skeleton, level);
                level.addFreshEntity(skeleton);

                husk.moveTo(spawnPos2.getX() + 0.5D, spawnPos2.getY(), spawnPos2.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDefiledCastellan(husk, level);
                level.addFreshEntity(husk);

                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_double").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("knight_enchanted_and_double")) {
            Zombie knight = EntityType.HUSK.create(level);
            Skeleton vigil = EntityType.SKELETON.create(level);
            Zombie castellan = EntityType.HUSK.create(level);

            if (knight != null && vigil != null && castellan != null) {
                BlockPos spawnPos2 = targetPlayer != null ? findSafeSpawnPosition(level, targetPlayer.blockPosition()) : spawnPos;
                if (spawnPos2 == null) spawnPos2 = spawnPos;
                BlockPos spawnPos3 = targetPlayer != null ? findSafeSpawnPosition(level, targetPlayer.blockPosition()) : spawnPos;
                if (spawnPos3 == null) spawnPos3 = spawnPos;

                knight.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDeadIronKnight(knight, level, true);
                level.addFreshEntity(knight);

                vigil.moveTo(spawnPos2.getX() + 0.5D, spawnPos2.getY(), spawnPos2.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeCatenaVigil(vigil, level);
                level.addFreshEntity(vigil);

                castellan.moveTo(spawnPos3.getX() + 0.5D, spawnPos3.getY(), spawnPos3.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                makeDefiledCastellan(castellan, level);
                level.addFreshEntity(castellan);

                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.boss.spawn_triple").withStyle(net.minecraft.ChatFormatting.RED));
                return true;
            }
        } else if (bossType.equals("monarch")) {
            MonarchEntity monarch = EntityInit.MONARCH.get().create(level);
            if (monarch != null) {
                monarch.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                monarch.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
                level.addFreshEntity(monarch);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Hollow Monarch has arrived.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
                return true;
            }
        } else if (bossType.equals("vanguard")) {
            VanguardEntity vanguard = EntityInit.VANGUARD.get().create(level);
            if (vanguard != null) {
                vanguard.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                vanguard.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
                level.addFreshEntity(vanguard);
                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("A Vanguard has spawned.").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
                return true;
            }
        } else if (bossType.equals("both_monarch_vanguard")) {
            BlockPos spawnPos2 = targetPlayer != null ? findSafeSpawnPosition(level, targetPlayer.blockPosition()) : spawnPos;
            if (spawnPos2 == null) spawnPos2 = spawnPos;

            MonarchEntity monarch = EntityInit.MONARCH.get().create(level);
            VanguardEntity vanguard = EntityInit.VANGUARD.get().create(level);
            if (monarch != null && vanguard != null) {
                monarch.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                monarch.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
                level.addFreshEntity(monarch);

                vanguard.moveTo(spawnPos2.getX() + 0.5D, spawnPos2.getY(), spawnPos2.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
                vanguard.getPersistentData().putBoolean("IsRoyalEscort", true);
                vanguard.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos2), net.minecraft.world.entity.MobSpawnType.TRIGGERED, null, null);
                level.addFreshEntity(vanguard);

                if (targetPlayer != null) targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("The Hollow Monarch and its royal Vanguards have emerged!").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
                return true;
            }
        }
        return false;
    }

// ---------- SPAWNING UTILITIES (SAFETY CALCULATOR)
    private static BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos center) {
        // Run up to 20 attempts to locate a safe surface block
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = 24.0D + level.random.nextDouble() * 16.0D;
            int x = center.getX() + (int) (Math.cos(angle) * distance);
            int z = center.getZ() + (int) (Math.sin(angle) * distance);

            // Fetch height coordinate mapping to the top surface block
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos spawnPos = new BlockPos(x, y, z);

            // Validate that the block is solid, has no fluids, and there's 2 blocks of air headroom
            BlockState belowState = level.getBlockState(spawnPos.below());
            if (!belowState.isAir() && level.getFluidState(spawnPos.below()).isEmpty() && level.isEmptyBlock(spawnPos) && level.isEmptyBlock(spawnPos.above())) {
                return spawnPos;
            }
        }
        return null;
    }

    // ---------- BOSS CONFIGURATORS (EQUIPMENT & STATS)

    /**
     * Initializes the Catena-Mail Vigil (Skeleton Archer boss).
     */
    private static void makeCatenaVigil(Skeleton skeleton, Level level) {
        skeleton.setPersistenceRequired();
        skeleton.getPersistentData().putBoolean("IsCatenaVigil", true);
        skeleton.setCustomName(net.minecraft.network.chat.Component.translatable("entity.aetasferreamod.boss.vigil"));

        // 60 HP (30 hearts) and high target acquisition range (128 blocks)
        safeSetAttribute(skeleton, Attributes.MAX_HEALTH, 60.0D);
        skeleton.setHealth(60.0F);
        safeSetAttribute(skeleton, Attributes.FOLLOW_RANGE, 128.0D);

        skeleton.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false));

        // Equips full Chainmail Armor (0% drop rate)
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        skeleton.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        skeleton.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
        skeleton.setDropChance(EquipmentSlot.CHEST, 0.0F);
        skeleton.setDropChance(EquipmentSlot.LEGS, 0.0F);
        skeleton.setDropChance(EquipmentSlot.FEET, 0.0F);

        // Equips Power II Bow (damaged slightly, 100% drop rate)
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 2);
        bow.setDamageValue(bow.getMaxDamage() / 2);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
    }

    /**
     * Initializes the Defiled Castellan (Husk Zombie boss).
     */
    private static void makeDefiledCastellan(Zombie husk, Level level) {
        husk.setPersistenceRequired();
        husk.getPersistentData().putBoolean("IsDefiledCastellan", true);
        husk.setCustomName(net.minecraft.network.chat.Component.translatable("entity.aetasferreamod.boss.castellan"));

        // 80 HP (40 hearts), 50% knockback resistance, and high follow range (128 blocks)
        safeSetAttribute(husk, Attributes.MAX_HEALTH, 80.0D);
        husk.setHealth(80.0F);
        safeSetAttribute(husk, Attributes.KNOCKBACK_RESISTANCE, 0.5D);
        safeSetAttribute(husk, Attributes.FOLLOW_RANGE, 128.0D);

        // Equips a damaged Iron Chestplate
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        chest.setDamageValue(chest.getMaxDamage() - 15);
        husk.setItemSlot(EquipmentSlot.CHEST, chest);

        // Equips a Spartan Weaponry Iron Greatsword if available, otherwise falls back to Vanilla Iron Sword
        @SuppressWarnings("removal")
        Item greatsword = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "iron_greatsword"));
        if (greatsword == null || greatsword == Items.AIR) {
            greatsword = Items.IRON_SWORD;
        }
        husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(greatsword));
    }

    /**
     * Initializes the Dead Iron Knight (Husk Zombie boss).
     */
    private static void makeDeadIronKnight(Zombie husk, Level level, boolean enchanted) {
        husk.setPersistenceRequired();
        husk.getPersistentData().putBoolean("IsDeadIronKnight", true);
        husk.setCustomName(net.minecraft.network.chat.Component.translatable("entity.aetasferreamod.boss.iron_knight"));

        // 100 HP (50 hearts), high follow range (128 blocks), and 10% faster movement speed
        safeSetAttribute(husk, Attributes.MAX_HEALTH, 100.0D);
        husk.setHealth(100.0F);
        safeSetAttribute(husk, Attributes.FOLLOW_RANGE, 128.0D);
        safeAddModifier(husk, Attributes.MOVEMENT_SPEED, new AttributeModifier("KnightSpeed", 0.1D, AttributeModifier.Operation.MULTIPLY_TOTAL));

        ItemStack head = new ItemStack(Items.IRON_HELMET);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.IRON_LEGGINGS);
        ItemStack feet = new ItemStack(Items.IRON_BOOTS);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);

        // Optional Enchantments (Protection II on armor, Sharpness III on sword)
        if (enchanted) {
            head.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
            chest.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
            legs.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
            feet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
            sword.enchant(Enchantments.SHARPNESS, 3);
        }

        husk.setItemSlot(EquipmentSlot.HEAD, head);
        husk.setItemSlot(EquipmentSlot.CHEST, chest);
        husk.setItemSlot(EquipmentSlot.LEGS, legs);
        husk.setItemSlot(EquipmentSlot.FEET, feet);
        husk.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        husk.setItemSlot(EquipmentSlot.MAINHAND, sword);

        // Spawns 4 accompanying breach squad (Sapper Squad) members with iron gear and tools
        Item[] sapperWeapons = {Items.IRON_PICKAXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE};
        for (int i = 0; i < 4; i++) {
            Zombie guard = EntityType.ZOMBIE.create(level);
            if (guard != null) {
                guard.setPos(husk.getX(), husk.getY(), husk.getZ());
                guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                guard.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                guard.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                guard.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(sapperWeapons[i]));
                guard.getPersistentData().putBoolean("IsSapperSquad", true);
                guard.setPersistenceRequired();
                level.addFreshEntity(guard);
            }
        }
    }

    /**
     * Initializes the Diamond Knight (Husk Zombie boss).
     */
    private static void makeDiamondKnight(Zombie husk, Level level) {
        husk.setPersistenceRequired();
        husk.getPersistentData().putBoolean("IsDiamondKnight", true);
        husk.setCustomName(net.minecraft.network.chat.Component.translatable("entity.aetasferreamod.boss.diamond_knight"));

        // 150 HP (75 hearts), full knockback resistance (1.0), and 15% speed increase
        safeSetAttribute(husk, Attributes.MAX_HEALTH, 150.0D);
        husk.setHealth(150.0F);
        safeSetAttribute(husk, Attributes.FOLLOW_RANGE, 128.0D);
        safeSetAttribute(husk, Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        safeAddModifier(husk, Attributes.MOVEMENT_SPEED, new AttributeModifier("KnightSpeed", 0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL));

        ItemStack head = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.DIAMOND_LEGGINGS);
        ItemStack feet = new ItemStack(Items.DIAMOND_BOOTS);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        // Strong enchantments (Protection IV on armor, Sharpness V on sword)
        head.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        chest.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        legs.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        feet.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        sword.enchant(Enchantments.SHARPNESS, 5);

        husk.setItemSlot(EquipmentSlot.HEAD, head);
        husk.setItemSlot(EquipmentSlot.CHEST, chest);
        husk.setItemSlot(EquipmentSlot.LEGS, legs);
        husk.setItemSlot(EquipmentSlot.FEET, feet);
        husk.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        husk.setItemSlot(EquipmentSlot.MAINHAND, sword);
    }

    // ---------- SAFE ATTRIBUTE HELPERS

    /**
     * Safely applies a base value to an entity attribute if it exists.
     */
    private static void safeSetAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /**
     * Safely applies a permanent modifier to an entity attribute if it exists.
     */
    private static void safeAddModifier(LivingEntity entity, Attribute attribute, AttributeModifier modifier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.addPermanentModifier(modifier);
        }
    }
}
