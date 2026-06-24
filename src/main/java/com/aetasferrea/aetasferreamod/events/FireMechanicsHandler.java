/**
 * @file FireMechanicsHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Manages lava quenching, panic speed, and weapon-induced burning.
 *
 * @description
 * Manages heat-based mechanics including lava bucket quenching in water,
 * providing temporary Speed boosts when players are on fire (panic),
 * and inflicting burning fire ticks when attacked with fiery/burning items.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("null")
// ---------- CLASS: FIRE MECHANICS HANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FireMechanicsHandler {

    // ---------- CUSTOM BURNING ITEMS TAG
    private static final TagKey<Item> BURNING_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "burning_items"));

    // ---------- TICK EVENT HANDLERS (PLAYER BEHAVIORS)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        if (player.level().isClientSide || !player.isAlive()) return;

        // ---------- 5 TICK LOOP (Lava Quenching) ----------
        if (player.tickCount % 5 == 0) {
            // If the player goes underwater holding a lava bucket, quench it into an empty bucket and spawn obsidian
            if (!player.isCreative() && player.isInWater()) {
                ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
                boolean lavaQuenched = false;

                if (mainHand.is(Items.LAVA_BUCKET)) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                    lavaQuenched = true;
                } else if (offHand.is(Items.LAVA_BUCKET)) {
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
                    lavaQuenched = true;
                }

                if (lavaQuenched) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                    player.level().setBlockAndUpdate(player.blockPosition(), Blocks.OBSIDIAN.defaultBlockState());
                }
            }
        }

        // ---------- 20 TICK LOOP (Panic Speed) ----------
        if (player.tickCount % 20 == 0) {
            // Grant a temporary speed boost if the player is currently on fire
            if (player.isOnFire()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 0, false, false));
            }
        }
    }

    // ---------- LIVING HURT EVENT HANDLER (BURNING DAMAGE)
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        // Ensure damage comes directly from a living attacker in melee range
        if (sourceEntity instanceof LivingEntity attacker && sourceEntity == directEntity) {
            ItemStack weapon = attacker.getMainHandItem();
            
            // If attacking with a designated burning item, set target on fire for 5 seconds
            if (weapon.is(BURNING_ITEMS)) {
                LivingEntity target = event.getEntity();
                int currentFire = target.getRemainingFireTicks();
                target.setRemainingFireTicks(Math.max(currentFire, 100));
            }
        }
    }
}
