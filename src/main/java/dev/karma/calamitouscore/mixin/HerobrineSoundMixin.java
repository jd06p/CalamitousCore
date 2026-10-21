package dev.karma.calamitouscore.mixin;

import dev.karma.calamitouscore.compat.herobrine.HerobrinePrimeOptimizations;
import net.mcreator.herobrineprime.procedures.DamageProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevents redundant combat-impact sound playback.
 *
 * <p>{@code DamageProcedure.execute} plays the {@code herobrine_prime:melee_impact}
 * sound, and Herobrine's AI calls {@code DamageProcedure.execute} every tick
 * while an uppercut/overhead/melee swing is active (roughly 20 consecutive
 * ticks). That stacking produces ~20 identical, fully overlapping sounds at the
 * same position, which is both audibly muddy and wasteful. This redirect keeps
 * the first playback per position per tick and drops the duplicates, leaving
 * the audible impact cue intact.
 *
 * <p>Only an exactly-duplicated sound (same cue at the same block position on
 * the same tick) is suppressed; distinct cues and genuinely separate hits are
 * unaffected.
 */
@Pseudo
@Mixin(value = DamageProcedure.class)
public abstract class HerobrineSoundMixin {

    @Redirect(method = "execute", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void calamitouscore$dedupImpactSound(Level level, Player player, BlockPos pos, SoundEvent sound,
                                                        SoundSource source, float volume, float pitch) {
        if (HerobrinePrimeOptimizations.allowSound(level, sound)) {
            level.playSound(player, pos, sound, source, volume, pitch);
        }
    }
}
