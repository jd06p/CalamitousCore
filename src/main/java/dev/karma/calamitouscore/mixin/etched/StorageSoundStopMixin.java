package dev.karma.calamitouscore.mixin.etched;
import dev.karma.calamitouscore.compat.etched.EtchedJukeboxSessions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler", remap = false)
public abstract class StorageSoundStopMixin {
    @Inject(method = "sendStopMessage", at = @At("HEAD"))
    private static void calamitouscore$stop(ServerLevel level, Vec3 position, UUID storage, CallbackInfo ci) { EtchedJukeboxSessions.stop(level, storage); }
}
