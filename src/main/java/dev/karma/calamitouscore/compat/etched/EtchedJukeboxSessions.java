package dev.karma.calamitouscore.compat.etched;

import dev.karma.calamitouscore.mixin.etched.StorageSoundAccessor;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import java.util.*;

/** Server retains the native keep-alive/playlist callback; clients report actual Etched completion. */
@Mod.EventBusSubscriber(modid = "calamitouscore")
public final class EtchedJukeboxSessions {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final class Session {
        final ServerLevel level; final UUID storage, token = UUID.randomUUID(); final ItemStack disc;
        final int entityId; final Set<UUID> listeners = new HashSet<>(); final Set<UUID> finished = new HashSet<>();
        Vec3 position;
        Session(ServerLevel level, UUID storage, ItemStack disc, Vec3 position, int entityId) {
            this.level = level; this.storage = storage; this.disc = disc.copy(); this.position = position; this.entityId = entityId;
        }
    }
    public static void start(ServerLevel level, Vec3 position, UUID storage, ItemStack disc, int entityId, Runnable callback) {
        ServerStorageSoundHandler.stopPlayingDisc(level, position, storage);
        Session session = new Session(level, storage, disc, position, entityId);
        SESSIONS.put(storage, session);
        ServerStorageSoundHandler.putSoundInfo(level, storage, () -> { stop(level, storage); callback.run(); }, position, Long.MAX_VALUE);
        update(session);
    }
    public static void stop(ServerLevel level, UUID storage) {
        Session session = SESSIONS.get(storage);
        if (session == null || session.level != level) return;
        SESSIONS.remove(storage);
        for (UUID id : session.listeners) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) send(player, session, ItemStack.EMPTY);
        }
    }
    private static void send(ServerPlayer player, Session session, ItemStack stack) {
        EtchedJukeboxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new EtchedJukeboxNetwork.Playback(session.storage, session.token, stack, session.position, session.entityId));
    }
    public static void complete(ServerPlayer player, UUID storage, UUID token) {
        Session session = SESSIONS.get(storage);
        if (player == null || session == null || !session.token.equals(token) || player.serverLevel() != session.level
                || !session.listeners.contains(player.getUUID())) return;
        session.finished.add(player.getUUID());
        finishIfReady(session);
    }
    private static void finishIfReady(Session session) {
        // Wait for every current listener, so a slow download on one client cannot cut another client's track short.
        if (!session.listeners.isEmpty() && session.finished.containsAll(session.listeners))
            StorageSoundAccessor.calamitouscore$finish(session.level, session.storage, true);
    }
    private static void update(Session session) {
        if (session.entityId >= 0) {
            var entity = session.level.getEntity(session.entityId);
            if (entity == null || !entity.isAlive()) { ServerStorageSoundHandler.stopPlayingDisc(session.level, session.position, session.storage); return; }
            session.position = entity.position();
        }
        Set<UUID> nearby = new HashSet<>();
        for (ServerPlayer player : session.level.players()) {
            if (player.distanceToSqr(session.position) > 128 * 128) continue;
            UUID id = player.getUUID(); nearby.add(id);
            if (session.listeners.add(id)) { session.finished.remove(id); send(player, session, session.disc); }
        }
        for (UUID id : new HashSet<>(session.listeners)) if (!nearby.contains(id)) {
            var player = session.level.getServer().getPlayerList().getPlayer(id);
            if (player != null) send(player, session, ItemStack.EMPTY);
            session.listeners.remove(id); session.finished.remove(id);
        }
        finishIfReady(session);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) for (Session session : new ArrayList<>(SESSIONS.values())) update(session);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        for (Session session : SESSIONS.values()) StorageSoundAccessor.calamitouscore$finish(session.level, session.storage, false);
        SESSIONS.clear();
    }
}
