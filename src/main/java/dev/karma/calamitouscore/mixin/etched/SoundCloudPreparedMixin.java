package dev.karma.calamitouscore.mixin.etched;

import dev.karma.calamitouscore.compat.etched.*;
import gg.moonflower.etched.api.sound.source.AudioSource;
import gg.moonflower.etched.api.util.DownloadProgressListener;
import gg.moonflower.etched.client.sound.SoundCache;
import gg.moonflower.etched.client.sound.EmptyAudioStream;
import net.minecraft.Util;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.Sound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.io.*;
import java.util.concurrent.*;

@Pseudo
@Mixin(targets="gg.moonflower.etched.api.sound.AbstractOnlineSoundInstance",remap=false)
public abstract class SoundCloudPreparedMixin {
    @Shadow @Final private String url;
    @Shadow @Final private boolean stereo;
    @Shadow @Final private DownloadProgressListener progressListener;
    @Shadow @Final private AudioSource.AudioFileType type;
    @Inject(method="getStream(Lnet/minecraft/client/sounds/SoundBufferLibrary;Lnet/minecraft/client/resources/sounds/Sound;Z)Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"),cancellable=true)
    private void calamitouscore$prepare(SoundBufferLibrary library,Sound sound,boolean repeat,CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        if(!SoundCloudAudio.isSoundCloud(url) || !type.isFile())return;
        cir.setReturnValue(SoundCache.getAudioStream(url,progressListener,type).thenCompose(AudioSource::openStream).thenApplyAsync(input->{
            try {
                progressListener.progressStartLoading();
                BufferedInputStream stream=new BufferedInputStream(input);stream.mark(8);
                byte[] prefix=stream.readNBytes(4);stream.reset();
                if(prefix.length==4 && prefix[0]=='R' && prefix[1]=='I' && prefix[2]=='F' && prefix[3]=='F')
                    return (AudioStream)PreparedSoundCloudStream.prepare(stream,stereo,repeat);
                return (AudioStream)PreparedSoundCloudStream.prepareMp3(stream,stereo,repeat);
            }catch(Exception e){try{input.close();}catch(IOException ignored){}throw new CompletionException(e);}
        },Util.backgroundExecutor()).handleAsync((stream,error)->{
            if(error!=null){progressListener.onFail();com.mojang.logging.LogUtils.getLogger().error("Failed to prepare SoundCloud playback",error);return EmptyAudioStream.INSTANCE;}
            progressListener.onSuccess();return stream;
        },Util.backgroundExecutor()));
    }
}
