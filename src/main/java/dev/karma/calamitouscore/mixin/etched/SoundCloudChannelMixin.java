package dev.karma.calamitouscore.mixin.etched;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import dev.karma.calamitouscore.compat.etched.PreparedSoundCloudStream;
import net.minecraft.client.sounds.AudioStream;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.IOException;

@Mixin(Channel.class)
public abstract class SoundCloudChannelMixin {
    @Unique private SoundBuffer calamitouscore$ownedBuffer;
    @Inject(method="attachBufferStream",at=@At("HEAD"),cancellable=true)
    private void calamitouscore$attachPrepared(AudioStream stream,CallbackInfo ci) {
        if(!(stream instanceof PreparedSoundCloudStream prepared))return;
        try {
            calamitouscore$ownedBuffer=prepared.attachTo((Channel)(Object)this);ci.cancel();
        }catch(IOException error){throw new IllegalStateException("Could not attach prepared SoundCloud audio",error);}
    }
    @Inject(method="destroy",at=@At("TAIL"))
    private void calamitouscore$releasePrepared(CallbackInfo ci) {
        if(calamitouscore$ownedBuffer!=null){calamitouscore$ownedBuffer.discardAlBuffer();calamitouscore$ownedBuffer=null;}
    }
}
