package dev.karma.calamitouscore.compat.etched;

import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.common.sound.download.SoundCloudSource;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;
import org.apache.commons.codec.digest.DigestUtils;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.*;
import java.util.concurrent.*;

/** Persistent, atomic, validated cache. Each sound opens its own stream, including concurrent jukeboxes. */
public final class SoundCloudClientAudio {
    private static final String DECODER_VERSION = "pns-v2";
    private static final ConcurrentHashMap<String, CompletableFuture<Path>> REQUESTS = new ConcurrentHashMap<>();
    static Path cacheFolder(Path gameDirectory) {
        return gameDirectory.resolve("etched-sounds").resolve("calamitouscore-soundcloud").resolve(DECODER_VERSION);
    }
    public static CompletableFuture<AudioSource> load(String url, DownloadProgressListener listener, Proxy proxy) {
        CompletableFuture<Path> request = REQUESTS.computeIfAbsent(url, key -> CompletableFuture.supplyAsync(() -> {
            Path downloaded = null;
            try {
                Path folder = cacheFolder(Minecraft.getInstance().gameDirectory.toPath());
                Files.createDirectories(folder);
                String hash=DigestUtils.sha256Hex(key);
                Path cache = folder.resolve(hash + ".audio");
                Path legacy=folder.resolve(hash + ".mp3");
                if(!Files.exists(cache) && Files.exists(legacy)) {
                    try { SoundCloudAudio.validate(legacy,0); return legacy; }
                    catch(IOException invalid) { Files.deleteIfExists(legacy); }
                }
                if (Files.exists(cache)) {
                    try { SoundCloudAudio.validate(cache, 0); return cache; }
                    catch (IOException invalid) { Files.deleteIfExists(cache); }
                }
                var source = new SoundCloudSource();
                com.mojang.logging.LogUtils.getLogger().info("[CalamitousCore SoundCloud] downloading key={} decoder={}",hash,DECODER_VERSION);
                long duration = ((SoundCloudDuration) source).calamitouscore$duration(key, proxy);
                downloaded = SoundCloudAudio.download(source.resolveUrl(key, listener, proxy), proxy, duration);
                try { Files.move(downloaded, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unsupported) {
                    Path staging = Files.createTempFile(folder, "download-", ".part");
                    try {
                        Files.copy(downloaded, staging, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(staging, cache, StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(staging); }
                }
                com.mojang.logging.LogUtils.getLogger().info("[CalamitousCore SoundCloud] ready key={} decoder={} bytes={} expectedMs={}",hash,DECODER_VERSION,Files.size(cache),duration);
                return cache;
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("[CalamitousCore SoundCloud] rejected key={} decoder={}",DigestUtils.sha256Hex(key),DECODER_VERSION,e);
                throw new CompletionException("SoundCloud download failed validation", e);
            }
            finally { if (downloaded != null) try { Files.deleteIfExists(downloaded); } catch (IOException ignored) {} }
        }, HttpUtil.DOWNLOAD_EXECUTOR));
        request.whenComplete((path, error) -> REQUESTS.remove(url, request));
        return request.thenApply(file -> (AudioSource) () -> {
            try { return CompletableFuture.completedFuture(Files.newInputStream(file)); }
            catch (IOException e) { return CompletableFuture.failedFuture(e); }
        });
    }
}
