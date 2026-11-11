package dev.karma.calamitouscore.compat.etched;

import com.google.gson.*;
import gg.moonflower.etched.api.sound.download.SoundDownloadSource;
import gg.moonflower.etched.javazoom.jl.decoder.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** SoundCloud-only transport. Never publishes a partial download or skips a failed segment. */
public final class SoundCloudAudio {
    public static final long MAX_BYTES = 100L * 1024 * 1024;
    private SoundCloudAudio() {}
    @FunctionalInterface public interface Request { InputStream get(String url, boolean authenticated) throws IOException; }

    public static boolean isSoundCloud(String url) {
        if (url == null) return false;
        try { String host = URI.create(url).getHost(); return host != null && (host.equals("soundcloud.com") || host.endsWith(".soundcloud.com")); }
        catch (IllegalArgumentException e) { return false; }
    }

    public static List<URL> resolve(JsonObject json, Request request) throws IOException {
        JsonArray media = json.getAsJsonObject("media").getAsJsonArray("transcodings");
        IOException failure = null;
        List<String> available = new ArrayList<>();
        for (String protocol : List.of("progressive", "hls")) {
            for (JsonElement element : media) {
                JsonObject entry = element.getAsJsonObject(), format = entry.getAsJsonObject("format");
                String mime = format.has("mime_type") ? format.get("mime_type").getAsString().toLowerCase(Locale.ROOT) : "unknown";
                String transport = format.has("protocol") ? format.get("protocol").getAsString().toLowerCase(Locale.ROOT) : "unknown";
                if (protocol.equals("progressive")) available.add(transport + " " + mime);
                boolean mp3 = mime.equals("audio/mpeg") || mime.startsWith("audio/mpeg;") || mime.equals("audio/mp3");
                boolean aac = mime.startsWith("audio/mp4") && mime.contains("mp4a.40.2");
                if ((!mp3 && !aac) || !protocol.equals(transport) || (entry.has("snipped") && entry.get("snipped").getAsBoolean())) continue;
                String endpoint=entry.get("url").getAsString();
                if(json.has("track_authorization") && !json.get("track_authorization").isJsonNull())
                    endpoint += (endpoint.contains("?") ? "&" : "?") + "track_authorization=" + URLEncoder.encode(json.get("track_authorization").getAsString(),StandardCharsets.UTF_8);
                try (InputStream in = request.get(endpoint, true)) {
                    String location = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().get("url").getAsString();
                    URL url = URI.create(location).toURL();
                    if (protocol.equals("progressive")) return List.of(url);
                    try (InputStream playlist = request.get(location, false)) { return playlist(url, playlist); }
                } catch (IOException | RuntimeException e) { failure = new IOException("SoundCloud audio source failed", e); }
            }
        }
        throw new IOException("No supported complete SoundCloud source; offered: " + String.join(", ", available)
                + (failure == null ? "" : "; " + failure.getCause().getMessage()), failure);
    }

    public static List<URL> playlist(URL base, InputStream stream) throws IOException {
        List<URL> urls = new ArrayList<>();
        boolean header = false, end = false; String initialization = null; int segments = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                line = line.trim();
                if (line.equals("#EXTM3U")) header = true;
                else if (line.equals("#EXT-X-ENDLIST")) end = true;
                else if (line.startsWith("#EXT-X-KEY:") && !line.contains("METHOD=NONE")) throw new IOException("Encrypted SoundCloud playlist is unsupported");
                else if (line.startsWith("#EXT-X-MAP:")) {
                    var match=java.util.regex.Pattern.compile("(?:^|,)URI=\"([^\"]+)\"").matcher(line.substring(11));
                    if(!match.find() || line.contains("BYTERANGE=") || segments>0) throw new IOException("Unsupported SoundCloud initialization map");
                    String map=new URL(base,match.group(1)).toString();
                    if(initialization!=null && !initialization.equals(map)) throw new IOException("Changing SoundCloud initialization map");
                    if(initialization==null) { initialization=map; urls.add(new URL(map)); }
                }
                else if (line.startsWith("#EXT-X-BYTERANGE:") || line.startsWith("#EXT-X-STREAM-INF:"))
                    throw new IOException("Unsupported SoundCloud media playlist layout");
                else if (!line.isEmpty() && !line.startsWith("#")) { urls.add(new URL(base, line)); segments++; }
            }
        }
        if (!header || !end || segments==0) throw new IOException("Incomplete SoundCloud playlist");
        return urls;
    }

    public static Path download(List<URL> urls, Proxy proxy, long expectedMillis) throws IOException {
        Path file = Files.createTempFile("calamitouscore-soundcloud-", ".mp3");
        boolean success = false;
        try {
            long total = 0;
            try (OutputStream out = Files.newOutputStream(file)) {
                for (URL url : urls) {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
                    connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
                    SoundDownloadSource.getDownloadHeaders().forEach(connection::setRequestProperty);
                    try {
                        if (connection.getResponseCode() != 200) throw new IOException("SoundCloud audio HTTP " + connection.getResponseCode());
                        try (InputStream in = connection.getInputStream()) { total += copy(in, out, connection.getContentLengthLong(), MAX_BYTES - total); }
                    } finally { connection.disconnect(); }
                }
            }
            byte[] prefix;
            try (InputStream input=Files.newInputStream(file)) { prefix=input.readNBytes(8); }
            if(prefix.length==8 && new String(prefix,4,4,StandardCharsets.US_ASCII).equals("ftyp")) {
                Path decoded=SoundCloudAac.decode(file,expectedMillis);
                Files.deleteIfExists(file); success=true; return decoded;
            }
            validate(file, expectedMillis);
            success = true; return file;
        } finally { if (!success) Files.deleteIfExists(file); }
    }

    public static long copy(InputStream in, OutputStream out, long expected, long limit) throws IOException {
        long count = 0; byte[] buffer = new byte[16384];
        for (int read; (read = in.read(buffer)) != -1;) {
            count += read;
            if (count > limit) throw new IOException("SoundCloud audio exceeds Etched's 100 MiB limit");
            out.write(buffer, 0, read);
        }
        if (count == 0 || (expected >= 0 && count != expected)) throw new IOException("Incomplete SoundCloud audio response");
        return count;
    }

    public static void validate(Path file, long expectedMillis) throws IOException {
        try (RandomAccessFile input=new RandomAccessFile(file.toFile(),"r")) {
            if(input.length()>=44 && input.readInt()==0x52494646) {
                byte[] raw=new byte[44];input.seek(0);input.readFully(raw);
                var wave=java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                int channels=Short.toUnsignedInt(wave.getShort(22)), rate=wave.getInt(24), size=wave.getInt(40);
                if(!new String(raw,8,8,StandardCharsets.US_ASCII).equals("WAVEfmt ") || wave.getInt(16)!=16 || wave.getShort(20)!=1
                        || !new String(raw,36,4,StandardCharsets.US_ASCII).equals("data") || wave.getShort(34)!=16
                        || channels<1 || channels>2 || rate<=0 || size<=0 || wave.getInt(4)+8L!=input.length()
                        || wave.getInt(28)!=rate*channels*2 || wave.getShort(32)!=channels*2 || input.length()!=44L+size)
                    throw new IOException("Invalid or truncated SoundCloud PCM cache");
                checkDuration(size*1000.0/(rate*channels*2),expectedMillis); return;
            }
        }
        double duration = 0; int frames = 0;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
            Bitstream bits = new Bitstream(input); Decoder decoder = new Decoder();
            try {
                for (Header header; (header = bits.readFrame()) != null;) {
                    decoder.decodeFrame(header, bits); duration += header.ms_per_frame(); frames++; bits.closeFrame();
                }
            } finally { bits.close(); }
        } catch (Exception e) { throw new IOException("SoundCloud audio failed MP3 validation", e); }
        if (frames == 0) throw new IOException("SoundCloud audio is empty");
        checkDuration(duration,expectedMillis);
    }
    static void checkDuration(double duration,long expectedMillis) throws IOException {
        if(expectedMillis>0 && duration+Math.max(3000,expectedMillis*.02)<expectedMillis)
            throw new IOException("SoundCloud audio is shorter than the advertised track");
    }
}
