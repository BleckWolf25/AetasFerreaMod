/**
 * @file ClientEvents.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.Objects;
import javax.annotation.Nonnull;

// ---------- CLASS: ClientEvents
public class ClientEvents {

    // ---------- KEY BINDING
    public static final KeyMapping WALK_MODE_KEY = new KeyMapping(
            "key.aetasferrea.walk_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.aetasferrea.keys"
    );

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
    }
}