package dev.karma.calamitouscore.compat.crc;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry.ChannelBuilder;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * A minimal, self-contained network channel used only by the CRC portal fix.
 *
 * <p>It carries a single client-to-server message, {@link CrcPortalTeleportPacket},
 * registered with a decoder that constructs the object without ever naming a client
 * class. Registering it is unconditional so the protocol version is identical on both
 * physical sides; the packet instance itself is only ever sent by
 * {@code CrcPortalScreenMixin} on the client.
 */
public final class CrcPortalNetwork {

    private static final String PROTOCOL_VERSION = "1.0";
    private static SimpleChannel channel;

    private CrcPortalNetwork() {
    }

    public static void register() {
        channel = ChannelBuilder
                .named(new ResourceLocation("calamitouscore", "crc_portal"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        int index = 0;
        channel.registerMessage(index++, CrcPortalTeleportPacket.class,
                CrcPortalTeleportPacket::encode,
                CrcPortalTeleportPacket::new,
                CrcPortalTeleportPacket::handle);
    }

    public static void sendToServer(CrcPortalTeleportPacket message) {
        channel.sendToServer(message);
    }
}
