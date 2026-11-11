package dev.karma.calamitouscore.mixin.etched;
import com.google.common.cache.Cache;
import dev.karma.calamitouscore.compat.etched.SoundCloudAudio;
import gg.moonflower.etched.api.record.TrackData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CompletableFuture;
@Pseudo
@Mixin(targets = "gg.moonflower.etched.common.menu.EtchingMenu", remap = false)
public abstract class EtchingRetryMixin {
    @Shadow @Final private static Cache<String, CompletableFuture<TrackData[]>> DATA_CACHE;
    @Shadow private String url;
    @Inject(method = "setupResultSlot", at = @At("HEAD"))
    private void calamitouscore$retryFailedSoundCloud(CallbackInfo ci) {
        if (!SoundCloudAudio.isSoundCloud(url)) return;
        var future = DATA_CACHE.getIfPresent(url);
        if (future != null && future.isCompletedExceptionally()) DATA_CACHE.asMap().remove(url, future);
    }
}
