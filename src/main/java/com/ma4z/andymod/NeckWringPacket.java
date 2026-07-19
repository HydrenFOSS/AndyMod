package com.ma4z.andymod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class NeckWringPacket {
    private final int ticksElapsed;

    public NeckWringPacket(int ticksElapsed) {
        this.ticksElapsed = ticksElapsed;
    }

    public static void encode(NeckWringPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ticksElapsed);
    }

    public static NeckWringPacket decode(FriendlyByteBuf buf) {
        return new NeckWringPacket(buf.readInt());
    }

    public static void handle(NeckWringPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            float headSpinSpeed = 22.0F; 
            float newYaw = player.getYRot() + headSpinSpeed;
            float newPitch = player.getXRot() + (float) Math.sin(msg.ticksElapsed * 0.7D) * 5.0F;
            player.setYRot(newYaw);
            player.setXRot(newPitch);
            player.yRotO = newYaw;
            player.xRotO = newPitch;
            player.yHeadRot = newYaw;
            player.yHeadRotO = newYaw;
            float lockedBodyYaw = player.getYRot() - (msg.ticksElapsed * headSpinSpeed);
            player.yBodyRot = lockedBodyYaw;
            player.yBodyRotO = lockedBodyYaw;
        }
    });
    ctx.get().setPacketHandled(true);
}
}