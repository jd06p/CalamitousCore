package dev.karma.calamitouscore.compat.etched;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.UUID;

public final class EtchedJukeboxNetwork {
    public static SimpleChannel CHANNEL;
    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("calamitouscore", "etched_jukebox"), () -> "1", "1"::equals, "1"::equals);
        CHANNEL.messageBuilder(Playback.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Playback::write).decoder(Playback::new).consumerMainThread((message, context) ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EtchedJukeboxClient.receive(message))).add();
        CHANNEL.messageBuilder(Completed.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> { buffer.writeUUID(message.storage); buffer.writeUUID(message.token); })
                .decoder(buffer -> new Completed(buffer.readUUID(), buffer.readUUID()))
                .consumerMainThread((message, context) -> EtchedJukeboxSessions.complete(context.get().getSender(), message.storage, message.token)).add();
    }
    public record Completed(UUID storage, UUID token) {}
    public record Playback(UUID storage, UUID token, ItemStack disc, Vec3 position, int entityId) {
        public Playback(FriendlyByteBuf buffer) { this(buffer.readUUID(), buffer.readUUID(), buffer.readItem(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readInt()); }
        void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(storage); buffer.writeUUID(token); buffer.writeItem(disc);
            buffer.writeDouble(position.x); buffer.writeDouble(position.y); buffer.writeDouble(position.z); buffer.writeInt(entityId);
        }
    }
}
