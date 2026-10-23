package dev.karma.calamitouscore.compat.crc;

import com.TBK.crc.server.capability.MultiArmCapability;
import com.TBK.crc.server.entity.PortalEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Server-side half of the CRC dedicated-server portal fix.
 *
 * <p>CRC's own portal-accept packet ({@code PacketHandlerPowers}, id {@code 6})
 * cannot be decoded on a dedicated server: its {@code PacketHandlerPowers(FriendlyByteBuf)}
 * deserialisation constructor unconditionally calls {@code Minecraft.getInstance()},
 * loading the class-level {@code @OnlyIn(CLIENT)} {@code net.minecraft.client.Minecraft}
 * and tripping Forge's {@code RuntimeDistCleaner}. This packet replaces only that
 * client-to-server send; its wire shape is empty and its handler never touches a
 * client type, so the server can decode it safely.
 *
 * <p>The handler reproduces exactly what CRC's {@code handle()} does for id {@code 6}:
 * it resets the sender's {@link MultiArmCapability} portal-warning state and runs
 * {@link PortalEntity#onTeleport(net.minecraft.world.entity.Entity)} to perform the
 * dimension teleport. Both callees are ordinary server-side CRC classes.
 */
public final class CrcPortalTeleportPacket {

    public CrcPortalTeleportPacket() {
    }

    public CrcPortalTeleportPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            MultiArmCapability cap = MultiArmCapability.get(player);
            if (cap != null) {
                cap.chickenEnemy = false;
                cap.warningLevel = 0;
                cap.wave = 0;
            }
            PortalEntity.onTeleport(player);
        });
        context.get().setPacketHandled(true);
    }
}
