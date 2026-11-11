package dev.karma.calamitouscore.compat.etched;

import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.*;
import gg.moonflower.etched.api.sound.source.AudioSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import java.util.*;

/** Uses Etched's own sounds and end callbacks, isolated by backpack and per-play token. */
public final class EtchedJukeboxClient {
    private static final Map<UUID, Playback> PLAYING = new HashMap<>();
    private static final class Playback {
        final EtchedJukeboxNetwork.Playback packet; final TrackData[] tracks;
        StopListeningSound sound; int index; boolean seenActive, waitingForEntity;
        Playback(EtchedJukeboxNetwork.Playback packet) {
            this.packet = packet; tracks = PlayableRecord.getStackMusic(packet.disc()).orElse(new TrackData[0]);
        }
    }
    public static void receive(EtchedJukeboxNetwork.Playback packet) {
        if (packet.disc().isEmpty()) {
            Playback old = PLAYING.get(packet.storage());
            if (old != null && old.packet.token().equals(packet.token())) stop(packet.storage());
            return;
        }
        stop(packet.storage());
        if (Minecraft.getInstance().level == null) return;
        Playback playback = new Playback(packet); PLAYING.put(packet.storage(), playback); next(playback);
    }
    private static void next(Playback playback) {
        Minecraft mc = Minecraft.getInstance();
        if (PLAYING.get(playback.packet.storage()) != playback || mc.level == null) return;
        if (mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.RECORDS) == 0
                || mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) == 0) playback.index = playback.tracks.length;
        if (playback.index >= playback.tracks.length) {
            // Retain the finished session until the server retires it; repeated callbacks cannot advance twice.
            playback.sound = null;
            EtchedJukeboxNetwork.CHANNEL.sendToServer(new EtchedJukeboxNetwork.Completed(playback.packet.storage(), playback.packet.token()));
            return;
        }
        TrackData track = playback.tracks[playback.index++];
        SoundInstance source;
        if (playback.packet.entityId() >= 0) {
            var entity = mc.level.getEntity(playback.packet.entityId());
            if (entity == null) { playback.index--; playback.waitingForEntity = true; return; }
            source = SoundTracker.getEtchedRecord(track.url(), track.getDisplayName(), entity, 16, false);
        } else source = SoundTracker.getEtchedRecord(track.url(), track.getDisplayName(), mc.level, BlockPos.containing(playback.packet.position()), AudioSource.AudioFileType.FILE);
        playback.seenActive = false;
        playback.waitingForEntity = false;
        int expectedIndex = playback.index;
        StopListeningSound wrapped = StopListeningSound.create(source, () -> mc.execute(() -> {
            if (PLAYING.get(playback.packet.storage()) == playback && playback.index == expectedIndex && playback.sound != null) {
                playback.sound.stopListening(); playback.sound = null; next(playback);
            }
        }));
        playback.sound = wrapped;
        StorageSoundHandler.playStorageSound(playback.packet.storage(), wrapped);
    }
    private static void stop(UUID storage) {
        Playback old = PLAYING.remove(storage);
        if (old == null) return;
        if (old.sound != null) { old.sound.stopListening(); Minecraft.getInstance().getSoundManager().stop(old.sound); }
        StorageSoundHandler.stopStorageSound(storage);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) for (UUID storage : new HashSet<>(PLAYING.keySet())) stop(storage);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        for (Playback playback : new ArrayList<>(PLAYING.values())) {
            if (playback.waitingForEntity) { next(playback); continue; }
            if (playback.sound == null) continue;
            boolean active = Minecraft.getInstance().getSoundManager().isActive(playback.sound);
            if (active) playback.seenActive = true;
            // Fallback for sound-engine cleanup paths that do not dispatch Etched's stop hook.
            else if (playback.seenActive) { playback.sound.stopListening(); playback.sound = null; next(playback); }
        }
    }
}
