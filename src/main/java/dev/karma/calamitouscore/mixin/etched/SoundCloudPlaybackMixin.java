package dev.karma.calamitouscore.mixin.etched;

import dev.karma.calamitouscore.compat.etched.SoundCloudAudio;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.net.Proxy;
import java.util.concurrent.*;

@Pseudo
@Mixin(targets = "gg.moonflower.etched.api.sound.download.SoundSourceManager", remap = false)
public abstract class SoundCloudPlaybackMixin {
    @Inject(method = "getAudioSource", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$completeDownload(String url, DownloadProgressListener listener, Proxy proxy, AudioSource.AudioFileType type, CallbackInfoReturnable<CompletableFuture<AudioSource>> cir) {
        if (!SoundCloudAudio.isSoundCloud(url) || !type.isFile()) return;
        cir.setReturnValue(dev.karma.calamitouscore.compat.etched.SoundCloudClientAudio.load(url, listener, proxy));
    }
}
