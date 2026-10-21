package dev.karma.calamitouscore.mixin;

import net.mcreator.herobrineprime.configuration.AttributesConfiguration;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures Herobrine Prime spawns with his full configured maximum health.
 *
 * <p>Herobrine Prime initialises his health only through its
 * {@code HerobrineOnSpawnProcedure}, which runs a vanilla command generator
 * {@code attribute @s minecraft:generic.max_health base set <Health>} taken from
 * its own config ({@code config/Attributes.toml -> Stats.Health}). Two problems
 * make that unreliable:
 *
 * <ol>
 *   <li>The entity's {@code createAttributes()} hard-codes a base of
 *       {@code 1024.0}. The {@code attribute ... base set} command still passes
 *       through {@code AttributeInstance#setBaseValue}, which clamps to the
 *       attribute's {@code [min,max]} range. Depending on load order the
 *       AttributeFix range widening may not be in effect for that call, so the
 *       base collapses back to 1024.</li>
 *   <li>The command only changes the maximum attribute; the entity's
 *       <em>current</em> health is set during construction and is not raised to
 *       the new maximum, so the boss can spawn below full health.</li>
 * </ol>
 *
 * <p>This injection runs once, at the tail of the spawn procedure (after the
 * original mod has issued its attribute commands), reads the mod's own
 * configurable {@code Stats.Health} value, applies it to the
 * {@code minecraft:generic.max_health} attribute via the normal attribute
 * system, and then sets the current health to the resulting maximum. Because it
 * hooks a spawn-only procedure it never fights combat, never re-heals during the
 * fight, and only touches the Herobrine entity.
 *
 * <p>AttributeFix is used as intended: it widens the attribute's allowed range,
 * so {@code setBaseValue} accepts values above the vanilla 1024 limit. No global
 * attribute system is replaced and no other mob is affected.
 */
@Pseudo
@Mixin(targets = "net.mcreator.herobrineprime.procedures.HerobrineOnSpawnProcedure", remap = false)
public abstract class HerobrineHealthMixin {

    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void calamitouscore$applyConfiguredHealth(LevelAccessor level, Entity entity, CallbackInfo ci) {
        if (entity == null || !(entity instanceof LivingEntity living)) return;
        if (entity.level().isClientSide()) return;

        double configured;
        try {
            configured = AttributesConfiguration.HEALTH.get();
        } catch (RuntimeException e) {
            return;
        }
        if (configured <= 0D) return;

        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        if (maxHealth.getBaseValue() != configured) {
            maxHealth.setBaseValue(configured);
        }

        float effectiveMax = living.getMaxHealth();
        if (effectiveMax <= 0F) return;
        if (living.getHealth() != effectiveMax) {
            living.setHealth(effectiveMax);
        }
    }
}
