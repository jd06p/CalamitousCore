package dev.karma.calamitouscore.mixin.difficulty;

import dev.karma.calamitouscore.compat.difficulty.BossRushEncounter;
import dev.karma.calamitouscore.compat.difficulty.BossRushSchedule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.misanthropy.linggango.difficulty_enhancement.server.BossRush", remap = false)
public abstract class BossRushMixin {
    @Redirect(method = "onTick", at = @At(value = "INVOKE", target = "Lcom/misanthropy/linggango/difficulty_enhancement/server/Fate;persisted(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag calamitouscore$sharedClock(Player player) {
        return BossRushSchedule.forPlayer((ServerPlayer) player);
    }

    @Redirect(method = "onTick", at = @At(value = "INVOKE", target = "Lcom/misanthropy/linggango/difficulty_enhancement/server/BossRush;summon(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private static boolean calamitouscore$encounter(ServerPlayer player) {
        return BossRushEncounter.summon(player);
    }
}
