package dev.karma.calamitouscore.mixin.etched;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.UUID;
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler", remap = false)
public interface StorageSoundAccessor {
    @Invoker("removeSoundInfo") static void calamitouscore$finish(ServerLevel level, UUID storage, boolean callback) { throw new AssertionError(); }
}
