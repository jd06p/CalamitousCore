package dev.karma.calamitouscore.mixin.etched;
import dev.karma.calamitouscore.compat.etched.EtchedJukeboxSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.UUID;
@Pseudo
@Mixin(targets = "com.absolutebuddies.sophisticatedbackpacksetchedintegration.EtchedDiscHandler", remap = false)
public abstract class EtchedDiscHandlerMixin {
    @Inject(method = "getMusicLengthInTicks", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$noGuessedDuration(ItemStack disc, net.minecraft.world.level.Level level, CallbackInfoReturnable<java.util.Optional<Integer>> cir) {
        cir.setReturnValue(java.util.Optional.empty());
    }
    @Inject(method = "playDisc(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/util/UUID;Lnet/minecraft/world/item/ItemStack;Ljava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$block(ServerLevel level, BlockPos pos, UUID storage, ItemStack disc, Runnable callback, CallbackInfo ci) {
        EtchedJukeboxSessions.start(level, Vec3.atCenterOf(pos), storage, disc, -1, callback); ci.cancel();
    }
    @Inject(method = "playDisc(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;Lnet/minecraft/world/item/ItemStack;ILjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$entity(ServerLevel level, Vec3 pos, UUID storage, ItemStack disc, int entity, Runnable callback, CallbackInfo ci) {
        EtchedJukeboxSessions.start(level, pos, storage, disc, entity, callback); ci.cancel();
    }
}
