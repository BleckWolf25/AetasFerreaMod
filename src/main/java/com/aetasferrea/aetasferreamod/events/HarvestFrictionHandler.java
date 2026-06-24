/**
 * @file HarvestFrictionHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Manages bare-handed harvest restrictions and hand trauma.
 *
 * @description
 * Manages realistic harvest mechanics including hand injuries (trauma) when breaking blocks bare-handed,
 * using knives to chop logs as a slow alternative to axes, and applying high durability penalties to wrong tools.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("null")
// ---------- CLASS: HARVEST FRICTION HANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HarvestFrictionHandler {

    // ---------- CUSTOM HARVEST TAGS
    public static final TagKey<Item> KNIVES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/knives"));

    // ---------- TOOL CONTEXT VALIDATION LOGIC
    private static class ToolContext {
        boolean isValid;
        boolean isKnifeChop;

        public ToolContext(boolean isValid, boolean isKnifeChop) {
            this.isValid = isValid;
            this.isKnifeChop = isKnifeChop;
        }
    }

    /**
     * Determines whether the tool used matches the requirements for mining the block.
     * Enforces log chopping using axes or knives specifically.
     */
    private static ToolContext validateToolContext(BlockState state, ItemStack item) {
        boolean isKnifeChop = item.is(KNIVES) && state.is(BlockTags.LOGS);
        
        // Context is valid if the block does not require a tool, or correct tool is held
        boolean isValid = !state.requiresCorrectToolForDrops() || item.isCorrectToolForDrops(state);

        // Logs require axes or knives explicitly to prevent bare-hand harvesting
        if (state.is(BlockTags.LOGS)) {
            isValid = item.canPerformAction(net.minecraftforge.common.ToolActions.AXE_DIG) || isKnifeChop;
        }

        return new ToolContext(isValid, isKnifeChop);
    }

    // ---------- UTILITY METHODS
    private static void executeDurabilityDamage(Player player, ItemStack item, int amount) {
        if (!item.isEmpty() && item.isDamageableItem()) {
            item.hurtAndBreak(amount, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    // ---------- CLICK BLOCK EVENT HANDLER (TRAUMA WARNING)
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (player == null || player.isCreative()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack item = player.getMainHandItem();

        ToolContext context = validateToolContext(state, item);

        // If trying to punch/mine an invalid block, apply fatigue and hand trauma immediately
        if (!context.isValid) {
            // Apply Mining Fatigue III
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 2, false, false));

            if (!level.isClientSide) {
                float strikeDamage = (float) (Math.random() * 0.5 + 0.2);
                LSOCompat.hurt(player, "left_arm", strikeDamage);
                LSOCompat.hurt(player, "right_arm", strikeDamage);

                float targetHealth = player.getHealth() - (strikeDamage * 0.25f);
                player.setHealth(Math.max(1.0f, targetHealth));

                // Display hand notice warning
                if (!player.getPersistentData().getBoolean("handInjuryNotice")) {
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.harvest.punch_hard").withStyle(ChatFormatting.RED), true);
                    player.getPersistentData().putBoolean("handInjuryNotice", true);
                }

                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.5f, 0.8f);
            }
        } else if (context.isKnifeChop) {
            // Give brief Haste I to allow knives to break wood logs slightly faster
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, false, false));
        }
    }

    // ---------- BLOCK BROKEN EVENT HANDLER (HARVEST PENALTIES)
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.isCreative()) return;

        Level level = event.getLevel() instanceof Level ? (Level) event.getLevel() : player.level();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ItemStack item = player.getMainHandItem();

        ToolContext context = validateToolContext(state, item);

        // CASE A: Chopping logs with a Knife (Allowed but damages knife durability heavily)
        if (context.isKnifeChop) {
            if (!level.isClientSide) {
                executeDurabilityDamage(player, item, 20);
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 0.4f, 1.4f);
            }
            return;
        }

        // CASE B: Mined completely using an invalid configuration (e.g. bare-handed stone or logs)
        if (!context.isValid) {
            if (!level.isClientSide) {
                // Apply major limb trauma
                float dynamicDamage = (float) (Math.random() * 1.5 + 1.0);
                LSOCompat.hurt(player, "left_arm", dynamicDamage);
                LSOCompat.hurt(player, "right_arm", dynamicDamage);

                float targetHealth = player.getHealth() - (dynamicDamage * 0.5f);
                player.setHealth(Math.max(1.0f, targetHealth));

                // Take durability damage on incorrect tools
                if (!item.isEmpty() && item.isDamageableItem()) {
                    executeDurabilityDamage(player, item, 24);
                }

                // Display major hand injury warning
                if (!player.getPersistentData().getBoolean("handInjuryNoticeMajor")) {
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.harvest.wrong_tools").withStyle(ChatFormatting.RED), true);
                    player.getPersistentData().putBoolean("handInjuryNoticeMajor", true);
                }

                // Particles and broken bone sounds
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
                }
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.5f, 0.7f);
            }
        }
    }
}
