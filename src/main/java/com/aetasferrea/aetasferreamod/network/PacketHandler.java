package com.aetasferrea.aetasferreamod.network;

import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AetasFerreaMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(WalkModePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WalkModePacket::toBytes)
                .decoder(WalkModePacket::new)
                .consumerMainThread(WalkModePacket::handle) // Safely binds explicitly to our static handler
                .add();
    }
}