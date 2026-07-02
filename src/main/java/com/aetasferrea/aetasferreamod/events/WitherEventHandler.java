/**
 * @file WitherEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Event handlers for custom Wither boss modifications and End dimension gating.
 *
 * @description
 * Applies custom modifications to the Wither boss including health adjustment, explosion behavior changes,
 * regeneration rebalancing, delayed heal-on-kill mechanics, custom co-op reward distribution, and arrow
 * immunity removal. Additionally enforces a dimensional gate by violently rejecting players from entering
 * the End dimension until they possess the Wither's catalyst.
 *
 * @since 29/06/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

// ---------- CLASS: WITHER EVENT HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WitherEventHandler {

    private static final String HEAL_ON_KILL_TIMER = "WitherHealTimer";
    private static final String HEAL_ON_KILL_TOTAL = "WitherHealTotal";

    // Entity Join - Set max health when Wither joins the world
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof WitherBoss wither && !event.getLevel().isClientSide()) {
            double maxHealth = AetasFerreaConfig.WITHER_MAX_HEALTH.get();
            var attribute = wither.getAttribute(Objects.requireNonNull(Attributes.MAX_HEALTH));
            if (attribute != null) {
                attribute.setBaseValue(maxHealth);
            }
            wither.setHealth((float) maxHealth);
        }
    }

    // Tick - Handle regen and delayed heal-on-kill
    @SubscribeEvent
    public static void onWitherTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide) {
            return;
        }

        handleRegen(wither);
        handleHealOnKill(wither);
    }

    // Passive regeneration
    private static void handleRegen(WitherBoss wither) {
        int regenTicks = AetasFerreaConfig.WITHER_REGEN_TICKS.get();
        if (regenTicks <= 0) {
            return;
        }

        long gameTime = wither.level().getGameTime();
        if (gameTime % regenTicks == 0 && wither.getHealth() < wither.getMaxHealth()) {
            wither.heal(1.0F);
        }
    }

    // Delayed heal-on-kill over duration
    private static void handleHealOnKill(WitherBoss wither) {
        long timer = wither.getPersistentData().getLong(HEAL_ON_KILL_TIMER);
        double totalHeal = wither.getPersistentData().getDouble(HEAL_ON_KILL_TOTAL);

        if (timer > 0) {
            int duration = AetasFerreaConfig.WITHER_HEAL_ON_KILL_DURATION.get();
            double amount = AetasFerreaConfig.WITHER_HEAL_ON_KILL_AMOUNT.get();

            if (timer <= wither.level().getGameTime()) {
                wither.heal((float) (amount / duration));
                totalHeal += amount / duration;
                wither.getPersistentData().putDouble(HEAL_ON_KILL_TOTAL, totalHeal);

                if (totalHeal >= amount) {
                    wither.getPersistentData().remove(HEAL_ON_KILL_TIMER);
                    wither.getPersistentData().remove(HEAL_ON_KILL_TOTAL);
                } else {
                    wither.getPersistentData().putLong(HEAL_ON_KILL_TIMER, wither.level().getGameTime() + 1);
                }
            }
        }
    }

    // Death - Distribute co-op rewards and trigger heal-on-kill
    @SubscribeEvent
    public static void onWitherDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        // Co-op Friendly Reward Distribution
        // If the Wither is the one dying, distribute rewards to all participating players
        if (victim instanceof WitherBoss deadWither && !deadWither.level().isClientSide()) {
            Level level = deadWither.level();

            // 128-block radius ensures all players in the arena get the reward, regardless of who got the final hit
            AABB rewardBox = deadWither.getBoundingBox().inflate(128.0D);
            List<Player> players = level.getEntitiesOfClass(Player.class, Objects.requireNonNull(rewardBox));

            for (Player player : players) {
                // Give 2 Nether Stars (Acts as the co-op key for the End Portal and other for whatever the player wishes)
                giveItemToPlayer(player, new ItemStack(Objects.requireNonNull(Items.NETHER_STAR), 2));

                // Give 4 Golden Apples
                giveItemToPlayer(player, new ItemStack(Objects.requireNonNull(Items.GOLDEN_APPLE), 4));

                // Give 12 Wither Roses
                giveItemToPlayer(player, new ItemStack(Objects.requireNonNull(Items.WITHER_ROSE), 12));

                // Give Punch II Enchanted Book
                ItemStack punchBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Objects.requireNonNull(Enchantments.PUNCH_ARROWS), 2));
                giveItemToPlayer(player, Objects.requireNonNull(punchBook));
            }
        }

        // If the Wither is the killer, heal it
        if (!(sourceEntity instanceof WitherBoss wither) || victim == wither) {
            return;
        }

        double healAmount = AetasFerreaConfig.WITHER_HEAL_ON_KILL_AMOUNT.get();
        int duration = AetasFerreaConfig.WITHER_HEAL_ON_KILL_DURATION.get();

        if (healAmount > 0 && duration > 0) {
            wither.getPersistentData().putLong(HEAL_ON_KILL_TIMER, wither.level().getGameTime() + 1);
            wither.getPersistentData().putDouble(HEAL_ON_KILL_TOTAL, 0.0);
        } else if (healAmount > 0) {
            wither.heal((float) healAmount);
        }
    }

    // Helper method to safely give items to a player (drops on ground if inventory is full)
    private static void giveItemToPlayer(Player player, @Nonnull ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // Drops - Remove default Nether Star to prevent duplicates since I distribute them manually
    @SubscribeEvent
    public static void onWitherDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof WitherBoss) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Objects.requireNonNull(Items.NETHER_STAR)));
        }
    }

    // Hurt - Custom Phase 2 Arrow Damage Handling
    @SubscribeEvent
    public static void onWitherHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(Objects.requireNonNull(DamageTypes.ARROW))) {
            if (AetasFerreaConfig.WITHER_ARROW_IMMUNITY.get()) {
                double healthRatio = wither.getHealth() / wither.getMaxHealth();
                if (healthRatio <= 0.5) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // Explosion - Modify radius, block destruction, and wither effect
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Explosion explosion = event.getExplosion();
        Entity source = explosion.getDirectSourceEntity();

        if (source instanceof WitherSkull || source instanceof WitherBoss) {
            Level level = event.getLevel();
            if (level.isClientSide) {
                return;
            }

            double radius = AetasFerreaConfig.WITHER_EXPLOSION_RADIUS.get();
            boolean applyWitherEffect = AetasFerreaConfig.WITHER_EXPLOSION_APPLY_WITHER_EFFECT.get();
            boolean destroyBedrockObsidian = AetasFerreaConfig.WITHER_EXPLOSION_DESTROY_BEDROCK_OBSIDIAN.get();

            event.getAffectedBlocks().removeIf((BlockPos pos) -> {
                if (pos == null) return true;
                BlockState state = level.getBlockState(pos);
                if (!destroyBedrockObsidian && (state.is(Objects.requireNonNull(Blocks.BEDROCK)) || state.is(Objects.requireNonNull(Blocks.OBSIDIAN)) || state.is(Objects.requireNonNull(Blocks.CRYING_OBSIDIAN)) || state.is(Objects.requireNonNull(Blocks.REINFORCED_DEEPSLATE)))) {
                    return true;
                }
                return false;
            });

            // Remove wither effect from entities in explosion radius
            if (!applyWitherEffect) {
                Vec3 center = explosion.getPosition();
                if (center != null) {
                    AABB searchBox = new AABB(
                        Objects.requireNonNull(center.subtract(radius, radius, radius)),
                        Objects.requireNonNull(center.add(radius, radius, radius))
                    );

                    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);
                    MobEffect witherEffect = MobEffects.WITHER;

                    if (witherEffect != null) {
                        for (LivingEntity entity : entities) {
                            if (entity != null) {
                                entity.removeEffect(witherEffect);
                            }
                        }
                    }
                }
            }
        }
    }

    // Dimension Travel - Lock the End behind the Wither
    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        // Only target players and only run on the server side
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        // Check if the player is trying to enter the End dimension
        if (event.getDimension().equals(Level.END)) {

            // Check if player has already permanently unlocked the End, OR if they are carrying the "Key"
            boolean hasDefeatedWither = player.getPersistentData().getBoolean("AetasFerrea_WitherDefeated");
            boolean hasNetherStar = player.getInventory().countItem(Objects.requireNonNull(Items.NETHER_STAR)) > 0;

            if (!hasDefeatedWither && !hasNetherStar) {
                // 1. Cancel the teleportation
                event.setCanceled(true);

                // 2. Play a harsh, heavy rejection sound (Anvil landing)
                player.level().playSound(null, Objects.requireNonNull(player.blockPosition()), Objects.requireNonNull(SoundEvents.ANVIL_LAND), SoundSource.PLAYERS, 1.0F, 0.5F);

                // 3. Violently throw the player backward using reverse vector math
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(-look.x * 1.5D, 0.5D, -look.z * 1.5D);
                player.hurtMarked = true;

                // 4. Deal damage using a custom Death Message source (with a safe fallback)
                DamageSource voidRejectionSource;
                try {
                    ResourceKey<net.minecraft.world.damagesource.DamageType> voidKey = ResourceKey.create(Objects.requireNonNull(Registries.DAMAGE_TYPE), new ResourceLocation("aetasferreamod", "void_rejection"));
                    voidRejectionSource = new DamageSource(Objects.requireNonNull(player.level().registryAccess().registryOrThrow(Objects.requireNonNull(Registries.DAMAGE_TYPE)).getHolderOrThrow(Objects.requireNonNull(voidKey))));
                } catch (Exception e) {
                    // Safe fallback if the datapack is missing
                    voidRejectionSource = player.damageSources().magic();
                }
                player.hurt(Objects.requireNonNull(voidRejectionSource), 6.0F);

                // 5. Display the rejection message using Resource Pack translatable components
                player.displayClientMessage(Objects.requireNonNull(Component.translatable("message.aetasferrea.void_rejects").withStyle(ChatFormatting.RED)), true);
            }
            else if (!hasDefeatedWither && hasNetherStar) {
                // If they used the Nether Star to enter for the first time, write the permanent tag to their data
                player.getPersistentData().putBoolean("AetasFerrea_WitherDefeated", true);

                // Translatable thematic message confirming the unlock
                player.displayClientMessage(Objects.requireNonNull(Component.translatable("message.aetasferrea.void_accepts").withStyle(ChatFormatting.DARK_PURPLE)), true);
            }
        }
    }
}
