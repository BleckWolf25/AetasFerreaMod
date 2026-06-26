/**
 * @file PacketHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Registers and exposes the mod's SimpleChannel network instance.
 *
 * @description
 * Creates the single SimpleChannel used for client-to-server communication and registers
 * all packet types with their encoder, decoder, and handler bindings.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.network;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// ---------- CLASS: PacketHandler
public class PacketHandler {

    // ---------- CONSTANTS
    private static final String PROTOCOL_VERSION = "1";

    // Shared channel instance used by all packet send calls throughout the mod
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AetasFerreaMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // ---------- PACKET REGISTRATION
    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(WalkModePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buf) -> packet.toBytes(buf))
                .decoder(buf -> new WalkModePacket(buf))
                .consumerMainThread(WalkModePacket::handle)
                .add();
    }
}