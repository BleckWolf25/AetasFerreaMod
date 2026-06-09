package com.aetasferrea.aetasferreamod.network;

import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WalkModePacket {
    public WalkModePacket() {}
    public WalkModePacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public static void handle(WalkModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof HorseEventHandler horse) {
                horse.setWalkMode(!horse.isWalkMode());
            }
        });
        context.setPacketHandled(true);
    }
}