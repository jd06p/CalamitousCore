package dev.karma.calamitouscore.mixin.crc;

import dev.karma.calamitouscore.compat.crc.CrcPortalNetwork;
import dev.karma.calamitouscore.compat.crc.CrcPortalTeleportPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-side half of the CRC dedicated-server portal fix.
 *
 * <p>This mixin lives in the {@code client} mixin list, so it is only ever applied on
 * the physical client (where {@code PortalScreen} - a class-level
 * {@code @OnlyIn(CLIENT)} class - is the only side that loads it at all).
 *
 * <p>CRC's portal-accept button builds a lambda ({@code lambda$init$0}) that ends with
 * {@code PacketHandler.sendToServer(new PacketHandlerPowers(6, null, null))}. That
 * client-to-server packet cannot be decoded on a dedicated server (its decoding
 * constructor loads {@code net.minecraft.client.Minecraft} and crashes the server).
 * The redirect below drops that specific send and emits
 * {@link CrcPortalTeleportPacket} instead, which the dedicated server can decode and
 * which reproduces CRC's id {@code 6} behaviour server-side.
 *
 * <p>The lambda sends nothing else, so replacing the call unconditionally keeps the
 * two-argument {@code sendToServer(Object)} signature fully intact and introduces no
 * other behaviour change. The handler deliberately takes its argument as
 * {@code Object} so that no client type appears in this mixin's bytecode.
 */
@Pseudo
@Mixin(targets = "com.TBK.crc.common.screen.PortalScreen", remap = false)
public abstract class CrcPortalScreenMixin {

    @Redirect(
            method = "lambda$init$0",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lcom/TBK/crc/server/network/PacketHandler;sendToServer(Ljava/lang/Object;)V"),
            remap = false)
    private void calamitouscore$sendServerSafePortalPacket(Object message) {
        CrcPortalNetwork.sendToServer(new CrcPortalTeleportPacket());
    }
}
