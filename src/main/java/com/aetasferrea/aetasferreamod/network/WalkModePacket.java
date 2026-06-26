/**
 * @file WalkModePacket.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Client-to-server packet that toggles walk mode on the player's ridden equine.
 *
 * @description
 * Carries no payload dataits receipt alone signals that the sending player wishes to toggle
 * walk mode on their currently ridden HorseEventHandler, AetasDonkey, or AetasMule entity.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.network;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.AetasMule;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// ---------- CLASS: WalkModePacket
public class WalkModePacket {

    // ---------- CONSTRUCTORS
    public WalkModePacket() {}

    // No-op decoderpacket carries no data
    public WalkModePacket(FriendlyByteBuf buf) {}

    // No-op encoderpacket carries no data
    public void toBytes(FriendlyByteBuf buf) {}

    // ---------- SERVER HANDLER
    public static void handle(WalkModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity vehicle = player.getVehicle();

            // Toggle walk mode on whichever custom equine the player is riding
            if (vehicle instanceof HorseEventHandler horse) {
                horse.setWalkMode(!horse.isWalkMode());
            } else if (vehicle instanceof AetasDonkey donkey) {
                donkey.setWalkMode(!donkey.isWalkMode());
            } else if (vehicle instanceof AetasMule mule) {
                mule.setWalkMode(!mule.isWalkMode());
            }
        });
        context.setPacketHandled(true);
    }
}
