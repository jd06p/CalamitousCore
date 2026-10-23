package dev.karma.calamitouscore.mixin.forgottenman;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.karma.calamitouscore.compat.forgottenman.ForgottenManPortalCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@Pseudo
@Mixin(targets = "com.forgottenman.portal.PortalTravel", remap = false)
public abstract class ForgottenManPortalMixin {

    @Redirect(method = "enter", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;m_8999_(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",
            remap = false), remap = false)
    private static void calamitouscore$deferTreeRoomTeleport(ServerPlayer player, ServerLevel target,
                                                             double x, double y, double z,
                                                             float yaw, float pitch) {
        ForgottenManPortalCompat.deferTeleport(player, target, x, y, z, yaw, pitch);
    }
}
