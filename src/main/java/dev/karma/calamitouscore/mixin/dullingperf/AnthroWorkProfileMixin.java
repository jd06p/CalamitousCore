package dev.karma.calamitouscore.mixin.dullingperf;

import dev.karma.calamitouscore.compat.dulling.performance.DullingWorkProfile;
import net.mcreator.thedulling.*;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.event.server.ServerStoppedEvent;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.AnthroMind", remap = false)
public abstract class AnthroWorkProfileMixin {
    @Redirect(method = "onServerTick", at = @At(value = "INVOKE", target = "Lnet/mcreator/thedulling/AnthroResourceIndex;tick(Lnet/minecraft/server/MinecraftServer;)V"))
    private static void calamitouscore$resource(MinecraftServer server) {
        if (!DullingWorkProfile.ENABLED) { AnthroResourceIndex.tick(server); return; }
        long start = System.nanoTime();
        try { AnthroResourceIndex.tick(server); } finally { DullingWorkProfile.record(server, 0, System.nanoTime() - start); }
    }
    @Redirect(method = "onServerTick", at = @At(value = "INVOKE", target = "Lnet/mcreator/thedulling/AnthroBaseManager;tick(Lnet/minecraft/server/MinecraftServer;)V"))
    private static void calamitouscore$base(MinecraftServer server) {
        if (!DullingWorkProfile.ENABLED) { AnthroBaseManager.tick(server); return; }
        long start = System.nanoTime();
        try { AnthroBaseManager.tick(server); } finally { DullingWorkProfile.record(server, 1, System.nanoTime() - start); }
    }
    @Redirect(method = "onServerTick", at = @At(value = "INVOKE", target = "Lnet/mcreator/thedulling/AnthroProgressionManager;tick(Lnet/minecraft/server/MinecraftServer;)V"))
    private static void calamitouscore$progression(MinecraftServer server) {
        if (!DullingWorkProfile.ENABLED) { AnthroProgressionManager.tick(server); return; }
        long start = System.nanoTime();
        try { AnthroProgressionManager.tick(server); } finally { DullingWorkProfile.record(server, 2, System.nanoTime() - start); }
    }
    @Inject(method = "onServerStopped", at = @At("RETURN"))
    private static void calamitouscore$releaseProfile(ServerStoppedEvent event, CallbackInfo ci) { DullingWorkProfile.clear(); }
}
