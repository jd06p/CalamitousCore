package dev.karma.calamitouscore.mixin.summoning;
import dev.karma.calamitouscore.compat.summoning.SummonCombatBridge;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MeleeAttackGoal.class)
public abstract class SharedMeleeSpeedMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow private int ticksUntilNextAttack;
    @Inject(method = "resetAttackCooldown", at = @At("RETURN"))
    private void calamitouscore$speed(CallbackInfo ci) { ticksUntilNextAttack = SummonCombatBridge.cooldown(mob, ticksUntilNextAttack); }
}
