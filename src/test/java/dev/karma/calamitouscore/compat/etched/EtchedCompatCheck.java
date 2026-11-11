package dev.karma.calamitouscore.compat.etched;

import com.google.gson.JsonParser;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Deterministic transport, decoder and installed-bytecode checks; not an in-game audio test. */
public final class EtchedCompatCheck {
    private static int checks;
    @FunctionalInterface interface Checked { void run() throws Exception; }
    static void check(boolean result) { checks++; if (!result) throw new AssertionError("Check " + checks); }
    static void fails(Checked action) throws Exception { try { action.run(); } catch (IOException expected) { checks++; return; } throw new AssertionError("Expected failure"); }
    static InputStream text(String value) { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
    public static void main(String[] args) throws Exception {
        check(SoundCloudAudio.isSoundCloud("https://soundcloud.com/artist/track"));
        check(!SoundCloudAudio.isSoundCloud(null));
        check(!SoundCloudAudio.isSoundCloud("https://notsoundcloud.com/track"));
        check(!SoundCloudAudio.isSoundCloud("https://soundcloud.com.attacker.invalid/track"));
        var json = JsonParser.parseString("{\"media\":{\"transcodings\":["
                + "{\"format\":{\"protocol\":\"hls\",\"mime_type\":\"audio/ogg; codecs=opus\"},\"url\":\"bad\"},"
                + "{\"format\":{\"protocol\":\"hls\",\"mime_type\":\"audio/mpeg\"},\"url\":\"hls\"},"
                + "{\"format\":{\"protocol\":\"progressive\",\"mime_type\":\"audio/mpeg\"},\"url\":\"mp3\"}]}}").getAsJsonObject();
        List<String> requests = new ArrayList<>();
        var urls = SoundCloudAudio.resolve(json, (url, auth) -> { requests.add(url); return text("{\"url\":\"https://example.invalid/track.mp3\"}"); });
        check(requests.equals(List.of("mp3")) && urls.size() == 1);
        requests.clear();
        urls = SoundCloudAudio.resolve(json, (url, auth) -> {
            requests.add(url);
            if (url.equals("mp3")) throw new IOException("Unavailable progressive source");
            return url.equals("hls") ? text("{\"url\":\"https://example.invalid/music/list.m3u8\"}") : text("#EXTM3U\n#EXTINF:2,\n1.mp3\n2.mp3\n#EXT-X-ENDLIST\n");
        });
        check(requests.equals(List.of("mp3", "hls", "https://example.invalid/music/list.m3u8")));
        check(urls.get(0).toString().equals("https://example.invalid/music/1.mp3") && urls.size() == 2);
        URL base = new URL("https://example.invalid/a.m3u8");
        fails(() -> SoundCloudAudio.playlist(base, text("#EXTM3U\n1.mp3\n")));
        fails(() -> SoundCloudAudio.playlist(base, text("#EXTM3U\n#EXT-X-ENDLIST\n")));
        fails(() -> SoundCloudAudio.playlist(base, text("#EXTM3U\n#EXT-X-KEY:METHOD=AES-128\n1.mp3\n#EXT-X-ENDLIST")));
        var mapped=SoundCloudAudio.playlist(base, text("#EXTM3U\n#EXT-X-MAP:URI=\"init.mp4\"\n1.m4s\n#EXT-X-ENDLIST"));
        check(mapped.size()==2 && mapped.get(0).toString().equals("https://example.invalid/init.mp4"));
        var aac = JsonParser.parseString("{\"track_authorization\":\"test+authorization\",\"media\":{\"transcodings\":[{\"format\":{\"protocol\":\"hls\",\"mime_type\":\"audio/mp4; codecs=\\\"mp4a.40.2\\\"\"},\"url\":\"https://example.invalid/resolve\"}]}}").getAsJsonObject();
        requests.clear();
        var aacUrls=SoundCloudAudio.resolve(aac,(url,auth)->{
            requests.add(url);
            return auth?text("{\"url\":\"https://example.invalid/list.m3u8\"}"):text("#EXTM3U\n#EXT-X-VERSION:7\n#EXT-X-MAP:URI=\"init.mp4\"\n1.m4s\n#EXT-X-ENDLIST");
        });
        check(requests.get(0).contains("track_authorization=test%2Bauthorization") && aacUrls.size()==2);
        fails(() -> SoundCloudAudio.copy(text("abc"), new ByteArrayOutputStream(), 10, 100));
        fails(() -> SoundCloudAudio.copy(text("abcdef"), new ByteArrayOutputStream(), -1, 5));
        fails(() -> SoundCloudAudio.copy(text(""), new ByteArrayOutputStream(), 0, 100));
        check(SoundCloudAudio.copy(text("abc"), new ByteArrayOutputStream(), -1, 100) == 3);
        Path audio = Files.createTempFile("cc-mp3-check-", ".mp3");
        try {
            // Valid MPEG-1 Layer III silent frames, 128 kbps / 44.1 kHz.
            byte[] frames = new byte[417 * 100];
            for (int i = 0; i < 100; i++) { int offset = i * 417; frames[offset] = (byte) 0xff; frames[offset+1] = (byte) 0xfb; frames[offset+2] = (byte) 0x90; }
            Files.write(audio, frames); SoundCloudAudio.validate(audio, 2500); checks++;
            fails(() -> SoundCloudAudio.validate(audio, 30000));
            Files.writeString(audio, "<html>not audio</html>"); fails(() -> SoundCloudAudio.validate(audio, 0));
            fails(() -> SoundCloudAac.decode(audio,0));
        } finally { Files.deleteIfExists(audio); }
        method("etched-3.0.4.jar", "gg/moonflower/etched/common/sound/download/SoundCloudSource", "lambda$resolveUrl$1", "(Lgg/moonflower/etched/api/util/DownloadProgressListener;Ljava/net/Proxy;Lcom/google/gson/JsonObject;)Ljava/util/List;");
        method("etched-3.0.4.jar", "gg/moonflower/etched/common/sound/download/SoundCloudSource", "get", "(Ljava/lang/String;Lgg/moonflower/etched/api/util/DownloadProgressListener;Ljava/net/Proxy;IZ)Ljava/io/InputStream;");
        method("etched-3.0.4.jar", "gg/moonflower/etched/common/sound/download/SoundCloudSource", "resolve", "(Ljava/lang/String;Lgg/moonflower/etched/api/util/DownloadProgressListener;Ljava/net/Proxy;Lgg/moonflower/etched/common/sound/download/SourceRequest;)Ljava/lang/Object;");
        method("etched-3.0.4.jar", "gg/moonflower/etched/common/sound/download/SoundCloudSource", "resolveTracks", "(Ljava/lang/String;Lgg/moonflower/etched/api/util/DownloadProgressListener;Ljava/net/Proxy;)Ljava/util/List;");
        method("etched-3.0.4.jar", "gg/moonflower/etched/common/menu/EtchingMenu", "setupResultSlot", "()V");
        method("etched-3.0.4.jar", "gg/moonflower/etched/api/sound/download/SoundSourceManager", "getAudioSource", "(Ljava/lang/String;Lgg/moonflower/etched/api/util/DownloadProgressListener;Ljava/net/Proxy;Lgg/moonflower/etched/api/sound/source/AudioSource$AudioFileType;)Ljava/util/concurrent/CompletableFuture;");
        String core = "net/p3pp3rf1y/sophisticatedcore/upgrades/jukebox/ServerStorageSoundHandler";
        method("sophisticatedcore-1.20.1-1.3.74.2217.jar", core, "removeSoundInfo", "(Lnet/minecraft/server/level/ServerLevel;Ljava/util/UUID;Z)V");
        method("sophisticatedcore-1.20.1-1.3.74.2217.jar", core, "sendStopMessage", "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;)V");
        String handler = "com/absolutebuddies/sophisticatedbackpacksetchedintegration/EtchedDiscHandler";
        method("sophisticatedbackpacksetchedintegration-1.0.0.jar", handler, "playDisc", "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/util/UUID;Lnet/minecraft/world/item/ItemStack;Ljava/lang/Runnable;)V");
        method("sophisticatedbackpacksetchedintegration-1.0.0.jar", handler, "playDisc", "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;Lnet/minecraft/world/item/ItemStack;ILjava/lang/Runnable;)V");
        System.out.println("Etched compatibility: " + checks + " transport, MP3 validation and installed-method checks passed.");
        if(args.length>0) {
            Path decoded=SoundCloudAac.decode(Path.of(args[0]),args.length>1?Long.parseLong(args[1]):0);
            try {
                SoundCloudAudio.validate(decoded,0);
                try(var pcm=gg.moonflower.etched.api.util.WaveDataReader.getAudioInputStream(new BufferedInputStream(Files.newInputStream(decoded)))) {
                    long read=pcm.transferTo(OutputStream.nullOutputStream());
                    if(read!=Files.size(decoded)-44) throw new AssertionError("Etched WAV reader did not read the complete PCM body");
                    System.out.println("PASS: installed Etched 3.0.4 WAV reader consumed the complete decoded track.");
                }
                System.out.println("PASS: complete live AAC sample decoded and validated as PCM, " + Files.size(decoded) + " bytes");
                Path corrupt=Files.createTempFile("cc-truncated-aac-", ".mp4");
                try {
                    byte[] source=Files.readAllBytes(Path.of(args[0]));
                    Files.write(corrupt,Arrays.copyOf(source,source.length-10));
                    fails(()->SoundCloudAac.decode(corrupt,0));
                    Files.write(corrupt,Arrays.copyOf(Files.readAllBytes(decoded),1000));
                    fails(()->SoundCloudAudio.validate(corrupt,0));
                    fails(()->SoundCloudAac.decode(Path.of(args[0]),3600000));
                    System.out.println("PASS: truncated AAC, truncated PCM and incomplete advertised duration rejected.");
                } finally { Files.deleteIfExists(corrupt); }
            }
            finally {Files.deleteIfExists(decoded);}
        }
    }
    static void method(String jar, String owner, String name, String descriptor) throws Exception {
        try (JarFile archive = new JarFile("libs/" + jar)) {
            ClassNode node = new ClassNode(); new ClassReader(archive.getInputStream(archive.getJarEntry(owner + ".class"))).accept(node, ClassReader.SKIP_CODE);
            check(node.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)));
        }
    }
}
