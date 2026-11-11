package dev.karma.calamitouscore.mixin.etched;

import com.google.gson.JsonObject;
import dev.karma.calamitouscore.compat.etched.SoundCloudAudio;
import gg.moonflower.etched.api.record.TrackData;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.common.sound.download.SourceRequest;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.List;

@Pseudo
@Mixin(targets = "gg.moonflower.etched.common.sound.download.SoundCloudSource", remap = false)
public abstract class SoundCloudSourceMixin implements dev.karma.calamitouscore.compat.etched.SoundCloudDuration {
    @Shadow private InputStream get(String url, DownloadProgressListener listener, Proxy proxy, int attempt, boolean auth) throws IOException { throw new AssertionError(); }
    @Shadow private <T> T resolve(String url, DownloadProgressListener listener, Proxy proxy, SourceRequest<T> request) throws IOException { throw new AssertionError(); }

    // The newer source preserves the supplied URL, including SoundCloud share-link hosts.
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private <T> void calamitouscore$preserveUrl(String url, DownloadProgressListener listener, Proxy proxy, SourceRequest<T> request, CallbackInfoReturnable<T> cir) throws IOException {
        try (InputStream stream = get("https://api-v2.soundcloud.com/resolve?url=" + URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8), listener, proxy, 0, true);
             InputStreamReader reader = new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            String kind = json.get("kind").getAsString();
            if (!kind.equals("track") && !kind.equals("playlist")) throw new IOException("URL is not a track or album");
            if (kind.equals("track") && !json.get("streamable").getAsBoolean()) throw new IOException("URL is not streamable");
            if (kind.equals("playlist") && !json.get("is_album").getAsBoolean()) throw new IOException("URL is not a track or album");
            cir.setReturnValue(request.process(json));
        }
    }

    @Override public long calamitouscore$duration(String url, Proxy proxy) throws IOException {
        return resolve(url, null, proxy, json -> json.has("duration") ? json.get("duration").getAsLong() : 0L);
    }

    @Redirect(method = "get", at = @At(value = "INVOKE", target = "Ljava/net/URL;openConnection(Ljava/net/Proxy;)Ljava/net/URLConnection;"))
    private URLConnection calamitouscore$timeouts(URL url, Proxy proxy) throws IOException {
        URLConnection connection = url.openConnection(proxy);
        connection.setConnectTimeout(15000); connection.setReadTimeout(30000); return connection;
    }

    @Inject(method = "lambda$resolveUrl$1", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$selectCodec(DownloadProgressListener listener, Proxy proxy, JsonObject json, CallbackInfoReturnable<List<URL>> cir) throws IOException {
        cir.setReturnValue(SoundCloudAudio.resolve(json, (url, auth) -> get(url, null, proxy, 0, auth)));
    }

    // EtchingMenu awaits this future before publishing its result slot. A metadata-only success is insufficient.
    @Inject(method = "resolveTracks", at = @At("RETURN"))
    private void calamitouscore$validateBeforeEtching(String url, DownloadProgressListener listener, Proxy proxy, CallbackInfoReturnable<List<TrackData>> cir) throws IOException {
        List<TrackData> tracks = cir.getReturnValue();
        for (int i = tracks.size() > 1 ? 1 : 0; i < tracks.size(); i++) {
            String track = tracks.get(i).url();
            long duration = calamitouscore$duration(track, proxy);
            Files.deleteIfExists(SoundCloudAudio.download(((SoundDownloadSource) this).resolveUrl(track, listener, proxy), proxy, duration));
        }
    }
}
