package com.aetasferrea.aetasferreamod.client;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {

    public static final KeyMapping WALK_MODE_KEY = new KeyMapping(
            "key.aetasferrea.walk_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.aetasferrea.keys"
    );

    @Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(WALK_MODE_KEY);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_HORSE.get(), 
                net.minecraft.client.renderer.entity.HorseRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                while (WALK_MODE_KEY.consumeClick()) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null && mc.player.getVehicle() instanceof com.aetasferrea.aetasferreamod.entity.HorseEventHandler horse) {
                        
                        boolean newState = !horse.isWalkMode();
                        horse.setWalkMode(newState);
                        
                        MutableComponent walkMessage = Component.literal("[Aetas Ferrea] ").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal("Walk Mode: ").withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(newState ? "ON" : "OFF").withStyle(newState ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));

                        mc.player.displayClientMessage(walkMessage, true);

                        com.aetasferrea.aetasferreamod.network.PacketHandler.INSTANCE.sendToServer(
                            new com.aetasferrea.aetasferreamod.network.WalkModePacket()
                        );
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGui(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof com.aetasferrea.aetasferreamod.entity.HorseEventHandler horse) {
                net.minecraft.client.gui.Font font = mc.font;
                if (font != null) {
                    double speedX = horse.getX() - horse.xo;
                    double speedZ = horse.getZ() - horse.zo;
                    double blocksPerTick = Math.sqrt(speedX * speedX + speedZ * speedZ);
                    event.getGuiGraphics().drawString(font, "Speed: " + String.format("%.2f", blocksPerTick * 20.0) + " blocks/sec", 10, 10, 0x00FF00);
                    event.getGuiGraphics().drawString(font, "Walk Mode: " + horse.isWalkMode(), 10, 20, 0x00FF00);
                }
            }
        }
    }
}