/**
 * @file ClientEvents.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Registers client-side key bindings and entity renderers for Aetas Ferrea equines.
 *
 * @description
 * Declares the walk-mode keybinding, registers it on the Mod event bus, wires up custom entity renderers
 * for the AETAS_HORSE (with chest layer), AETAS_DONKEY, and AETAS_MULE, and handles the client tick
 * to detect walk-mode key presses and send the corresponding server packet.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.client;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// ---------- CLASS: ClientEvents
public class ClientEvents {

    // ---------- KEY BINDING
    public static final KeyMapping WALK_MODE_KEY = new KeyMapping(
            "key.aetasferrea.walk_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.aetasferrea.keys"
    );

    // ---------- HELPERS
    public static boolean isHollowMonarch(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Husk husk) {
            Component name = husk.getCustomName();
            return (name != null && name.getString().contains("Hollow Monarch")) || husk.getBbHeight() > 3.0F;
        }
        return false;
    }

    public static boolean isNetherGuardian(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton) {
            Component name = skeleton.getCustomName();
            return (name != null && name.getString().contains("Nether Guardian")) || skeleton.getBbHeight() > 3.0F;
        }
        return false;
    }


    // ---------- CLASS: ClientModBusEvents
    @Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBusEvents {

        // ---------- KEY MAPPING REGISTRATION
        @SubscribeEvent
        @SuppressWarnings("null")
        public static void onKeyRegister(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(WALK_MODE_KEY);
        }

        // ---------- ENTITY RENDERER REGISTRATION
        @SuppressWarnings({ "null", "unchecked" })
        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            // Aetas Horse: standard HorseRenderer with a custom saddlebag chest layer
            EntityType<?> horseType = com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_HORSE.get();
            event.registerEntityRenderer((EntityType<? extends net.minecraft.world.entity.animal.horse.Horse>) horseType, context -> {
                net.minecraft.client.renderer.entity.HorseRenderer renderer =
                        new net.minecraft.client.renderer.entity.HorseRenderer(context);
                renderer.addLayer(new CustomChestLayer(renderer));
                return renderer;
            });

            // Aetas Donkey: ChestedHorseRenderer with shadow scale 0.87 and vanilla donkey texture
            EntityType<?> donkeyType = com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_DONKEY.get();
            ModelLayerLocation donkeyLayer = net.minecraft.client.model.geom.ModelLayers.DONKEY;

            event.registerEntityRenderer((EntityType<? extends com.aetasferrea.aetasferreamod.entity.AetasDonkey>) donkeyType, context ->
                new net.minecraft.client.renderer.entity.ChestedHorseRenderer<com.aetasferrea.aetasferreamod.entity.AetasDonkey>(
                        context, 0.87F, Objects.requireNonNull(donkeyLayer)) {
                    private static final ResourceLocation TEXTURE =
                            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/donkey.png"));
                    @Override
                    @Nonnull
                    public ResourceLocation getTextureLocation(
                            @Nonnull com.aetasferrea.aetasferreamod.entity.AetasDonkey pEntity) {
                        return TEXTURE;
                    }
                }
            );

            // Aetas Mule: ChestedHorseRenderer with shadow scale 0.92 and vanilla mule texture
            EntityType<?> muleType = com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_MULE.get();
            ModelLayerLocation muleLayer = net.minecraft.client.model.geom.ModelLayers.MULE;

            event.registerEntityRenderer((EntityType<? extends com.aetasferrea.aetasferreamod.entity.AetasMule>) muleType, context ->
                new net.minecraft.client.renderer.entity.ChestedHorseRenderer<com.aetasferrea.aetasferreamod.entity.AetasMule>(
                        context, 0.92F, Objects.requireNonNull(muleLayer)) {
                    private static final ResourceLocation TEXTURE =
                            Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/mule.png"));
                    @Override
                    @Nonnull
                    public ResourceLocation getTextureLocation(
                            @Nonnull com.aetasferrea.aetasferreamod.entity.AetasMule pEntity) {
                        return TEXTURE;
                    }
                }
            );

            // Monarch: Use Husk renderer
            EntityType<?> monarchType = com.aetasferrea.aetasferreamod.init.EntityInit.MONARCH.get();
            event.registerEntityRenderer((EntityType<? extends net.minecraft.world.entity.monster.Husk>) monarchType,
                net.minecraft.client.renderer.entity.HuskRenderer::new);

            // Vanguard: Use Wither Skeleton renderer
            EntityType<?> vanguardType = com.aetasferrea.aetasferreamod.init.EntityInit.VANGUARD.get();
            event.registerEntityRenderer((EntityType<? extends net.minecraft.world.entity.monster.WitherSkeleton>) vanguardType,
                net.minecraft.client.renderer.entity.WitherSkeletonRenderer::new);

        }

        // ---------- ADD LAYERS REGISTRATION
        @SuppressWarnings("deprecation")
        @SubscribeEvent
        public static void onAddLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
            // Hollow Monarch: Register 3D magma fissures and emissive crimson eyes layer
            net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.monster.Husk, net.minecraft.client.model.ZombieModel<net.minecraft.world.entity.monster.Husk>> monarchRenderer =
                    event.getRenderer(com.aetasferrea.aetasferreamod.init.EntityInit.MONARCH.get());
            if (monarchRenderer != null) {
                monarchRenderer.addLayer(new MonarchVisualLayer<>(monarchRenderer));
            }

            // Also add to vanilla Husk for compatibility
            net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.monster.Husk, net.minecraft.client.model.ZombieModel<net.minecraft.world.entity.monster.Husk>> huskRenderer =
                    event.getRenderer(Objects.requireNonNull(EntityType.HUSK));
            if (huskRenderer != null) {
                huskRenderer.addLayer(new MonarchVisualLayer<>(huskRenderer));
            }

            // Nether Guardian: Register tattered fabric banner layer to Vanguard
            net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.monster.WitherSkeleton, net.minecraft.client.model.SkeletonModel<net.minecraft.world.entity.monster.WitherSkeleton>> vanguardRenderer =
                    event.getRenderer(com.aetasferrea.aetasferreamod.init.EntityInit.VANGUARD.get());
            if (vanguardRenderer != null) {
                vanguardRenderer.addLayer(new VanguardVisualLayer<>(vanguardRenderer));
            }

            // Also add to vanilla Wither Skeleton for compatibility
            net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.world.entity.monster.WitherSkeleton, net.minecraft.client.model.SkeletonModel<net.minecraft.world.entity.monster.WitherSkeleton>> witherRenderer =
                    event.getRenderer(Objects.requireNonNull(EntityType.WITHER_SKELETON));
            if (witherRenderer != null) {
                witherRenderer.addLayer(new VanguardVisualLayer<>(witherRenderer));
            }
        }
    }

    // ---------- CLASS: ClientForgeEvents
    @Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        // ---------- WALK MODE KEY HANDLING
        @SuppressWarnings("null")
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            while (WALK_MODE_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player == null) continue;

                net.minecraft.world.entity.Entity vehicle = player.getVehicle();
                if (vehicle == null) continue;

                boolean handled = false;
                boolean newState = false;

                // ---------- WALK MODE TOGGLE (Horse / Donkey / Mule) ----------
                if (vehicle instanceof com.aetasferrea.aetasferreamod.entity.HorseEventHandler horse) {
                    newState = !horse.isWalkMode();
                    horse.setWalkMode(newState);
                    handled = true;
                } else if (vehicle instanceof com.aetasferrea.aetasferreamod.entity.AetasDonkey donkey) {
                    newState = !donkey.isWalkMode();
                    donkey.setWalkMode(newState);
                    handled = true;
                } else if (vehicle instanceof com.aetasferrea.aetasferreamod.entity.AetasMule mule) {
                    newState = !mule.isWalkMode();
                    mule.setWalkMode(newState);
                    handled = true;
                }

                if (handled) {
                    MutableComponent prefix = Component.translatable("message.aetasferreamod.system.prefix").withStyle(ChatFormatting.GREEN);
                    MutableComponent label = Component.translatable("message.aetasferreamod.system.walk_mode").withStyle(ChatFormatting.WHITE);
                    MutableComponent status = Component.translatable(newState ? "message.aetasferreamod.system.on" : "message.aetasferreamod.system.off")
                            .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);

                    // Force compilation match for components chaining into @Nonnull Component parameters
                    Component message = prefix.append(label).append(status);

                    player.displayClientMessage(message, true);

                    // Inform the server so it can apply the walk mode cap server-side
                    com.aetasferrea.aetasferreamod.network.PacketHandler.INSTANCE.sendToServer(
                        new com.aetasferrea.aetasferreamod.network.WalkModePacket()
                    );
                }
            }
        }

        // ---------- CLIENT-SIDE TICK & PARTICLE EMISSIONS
        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            net.minecraft.world.entity.LivingEntity entity = event.getEntity();
            if (entity == null || !entity.level().isClientSide) return;

            if (isHollowMonarch(entity)) {
                if (entity.getPersistentData().getBoolean("IsDormant")) return; // No particles while dormant
                spawnMonarchParticles(entity);
            } else if (isNetherGuardian(entity)) {
                spawnGuardianParticles(entity);
            }
        }

        @SuppressWarnings("null")
        private static void spawnMonarchParticles(net.minecraft.world.entity.LivingEntity entity) {
            net.minecraft.world.level.Level level = entity.level();
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            net.minecraft.util.RandomSource random = level.random;

            // 1. Full-body Soul Fire Flames & Normal Flames spread across the entire height (0.2 to 3.8 blocks)
            int flameCount = 2 + random.nextInt(3);
            for (int i = 0; i < flameCount; i++) {
                double offX = (random.nextDouble() - 0.5D) * 1.2D;
                double offY = 0.2D + random.nextDouble() * 3.6D; // Spread from ankles to shoulders/head
                double offZ = (random.nextDouble() - 0.5D) * 1.2D;

                if (random.nextBoolean()) {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x + offX, y + offY, z + offZ, 0.0D, 0.01D, 0.0D);
                } else {
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, x + offX, y + offY, z + offZ, 0.0D, 0.01D, 0.0D);
                }
            }

            // 2. Heavy Dripping Lava & Falling Lava droplets spilling out from ribs, joints, arms, and legs across entire body
            int lavaCount = 1 + random.nextInt(2);
            for (int i = 0; i < lavaCount; i++) {
                double rx = x + (random.nextDouble() - 0.5D) * 1.1D;
                double ry = 0.4D + random.nextDouble() * 3.2D; // Spread across legs, ribs, and shoulders
                double rz = z + (random.nextDouble() - 0.5D) * 1.1D;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA, rx, y + ry, rz, 0.0D, 0.0D, 0.0D);
            }
            if (random.nextFloat() < 0.60F) {
                double kx = x + (random.nextDouble() - 0.5D) * 1.0D;
                double ky = 0.5D + random.nextDouble() * 3.0D;
                double kz = z + (random.nextDouble() - 0.5D) * 1.0D;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_LAVA, kx, y + ky, kz, 0.0D, 0.0D, 0.0D);
            }
            if (random.nextFloat() < 0.40F) {
                double lx = x + (random.nextDouble() - 0.5D) * 1.0D;
                double ly = 0.3D + random.nextDouble() * 3.4D;
                double lz = z + (random.nextDouble() - 0.5D) * 1.0D;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, lx, y + ly, lz, 0.0D, 0.0D, 0.0D);
            }

            // 3. Ender Portal particles for Eye of Ender in offhand/mainhand
            if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).is(Items.ENDER_EYE) ||
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).is(Items.ENDER_EYE)) {
                for (int i = 0; i < 3; i++) {
                    double px = x + (random.nextDouble() - 0.5D) * 1.5D;
                    double py = y + 1.0D + (random.nextDouble() - 0.5D) * 1.5D;
                    double pz = z + (random.nextDouble() - 0.5D) * 1.5D;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, px, py, pz, (random.nextDouble() - 0.5D) * 0.5D, (random.nextDouble() - 0.5D) * 0.5D, (random.nextDouble() - 0.5D) * 0.5D);
                    if (random.nextBoolean()) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, px, py, pz, 0, 0.05D, 0);
                    }
                }
            }
        }

        @SuppressWarnings("null")
        private static void spawnGuardianParticles(net.minecraft.world.entity.LivingEntity entity) {
            net.minecraft.world.level.Level level = entity.level();
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            net.minecraft.util.RandomSource random = level.random;
            double scale = 2.5D;

            // Shed ash and basalt particles from the joints to create a localized soot cloud
            if (random.nextFloat() < 0.7F) {
                int count = 1 + random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    double offX = (random.nextDouble() - 0.5D) * 0.8D * scale;
                    double offY = random.nextDouble() * 1.8D * scale;
                    double offZ = (random.nextDouble() - 0.5D) * 0.8D * scale;

                    net.minecraft.core.particles.ParticleOptions type;
                    float r = random.nextFloat();
                    if (r < 0.4F) {
                        type = net.minecraft.core.particles.ParticleTypes.ASH;
                    } else if (r < 0.8F) {
                        type = net.minecraft.core.particles.ParticleTypes.WHITE_ASH;
                    } else {
                        type = net.minecraft.core.particles.ParticleTypes.SMOKE;
                    }

                    level.addParticle(type, x + offX, y + offY, z + offZ, 0.0D, -0.01D, 0.0D);
                }
            }

            // 2. Ender Portal particles for Eye of Ender
            if (entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).is(Items.ENDER_EYE) ||
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).is(Items.ENDER_EYE)) {
                for (int i = 0; i < 3; i++) {
                    double px = x + (random.nextDouble() - 0.5D) * 1.5D;
                    double py = y + 1.0D + (random.nextDouble() - 0.5D) * 1.5D;
                    double pz = z + (random.nextDouble() - 0.5D) * 1.5D;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, px, py, pz, (random.nextDouble() - 0.5D) * 0.5D, (random.nextDouble() - 0.5D) * 0.5D, (random.nextDouble() - 0.5D) * 0.5D);
                }
            }

            // 3. Weapon swing trail particles
            if (entity.swinging) {
                net.minecraft.world.phys.Vec3 look = entity.getLookAngle();
                for (int i = 0; i < 4; i++) {
                    double sx = x + look.x * 1.5D + (random.nextDouble() - 0.5D) * 1.2D;
                    double sy = y + 1.5D + (random.nextDouble() - 0.5D) * 1.2D;
                    double sz = z + look.z * 1.5D + (random.nextDouble() - 0.5D) * 1.2D;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, sx, sy, sz, look.x * 0.2D, 0.0D, look.z * 0.2D);
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, sx, sy, sz, (random.nextDouble() - 0.5D) * 0.2D, random.nextDouble() * 0.2D, (random.nextDouble() - 0.5D) * 0.2D);
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, sx, sy, sz, 0.0D, 0.05D, 0.0D);
                }
            }
        }
    }
}
