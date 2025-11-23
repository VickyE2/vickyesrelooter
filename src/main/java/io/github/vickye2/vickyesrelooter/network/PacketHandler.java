package io.github.vickye2.vickyesrelooter.network;


import io.github.vickye2.vickyesrelooter.Vickyesrelooter;
import io.github.vickye2.vickyesrelooter.network.packets.CreateTablePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Vickyesrelooter.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        log("Registering CreateKitPacket at ID " + packetId);
        INSTANCE.registerMessage(
                packetId++, CreateTablePacket.class,
                CreateTablePacket::encode, CreateTablePacket::decode, CreateTablePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static void log(String msg) {
        System.out.println("[StarterKits::PacketHandler] " + msg);
    }
}