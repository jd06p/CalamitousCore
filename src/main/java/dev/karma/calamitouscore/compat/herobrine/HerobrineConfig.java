package dev.karma.calamitouscore.compat.herobrine;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CalamitousCore configuration for the optional Herobrine Prime boss-fight
 * performance optimizations.
 *
 * <p>Everything here only ever affects the Herobrine Prime encounter. The
 * values are read live through the static accessors below so a user can tune
 * the visual intensity from {@code config/calamitouscore-common.toml} without
 * recompiling the mod.
 *
 * <p>All multipliers are clamped to {@code [0.0, 1.0]}. A multiplier of
 * {@code 1.0} reproduces the original mod behaviour exactly; {@code 0.0}
 * removes the corresponding decorative particles entirely. Damage, hitboxes,
 * attack timing, animations, phases, sounds and bossbar progression are never
 * affected by these settings.
 */
public final class HerobrineConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue OPTIMIZE_FIGHT;
    private static final ForgeConfigSpec.DoubleValue PARTICLE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue HEAVY_EFFECT_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue OPTIMIZE_SOUNDS;
    private static final ForgeConfigSpec.BooleanValue OPTIMIZE_TICK_WORK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Herobrine Prime boss-fight performance optimizations.",
                "These only affect the Herobrine Prime encounter and never change",
                "damage, attack timing, hitboxes, animations, phases or difficulty.",
                "Set optimizeHerobrineFight=false to disable all of them."
        ).push("herobrinePrime");

        OPTIMIZE_FIGHT = builder
                .comment("Master switch for all Herobrine Prime boss-fight optimizations.")
                .define("optimizeHerobrineFight", true);

        PARTICLE_MULTIPLIER = builder
                .comment(
                        "Multiplier for ordinary combat particles (arc trails, melee soul-fire,",
                        "soul-charge trails). 0.5 keeps half of them, matching the default",
                        "50% reduction. 1.0 = original, 0.0 = none."
                )
                .defineInRange("herobrineParticleMultiplier", 0.5D, 0.0D, 1.0D);

        HEAVY_EFFECT_MULTIPLIER = builder
                .comment(
                        "Multiplier for particularly expensive particle bursts (the",
                        "explosion sphere and the ground shockwave ring). These scale with",
                        "radius^2 / radius and are by far the heaviest effects. 0.3 keeps",
                        "30% (a 70% reduction). 1.0 = original, 0.0 = none."
                )
                .defineInRange("herobrineHeavyEffectMultiplier", 0.3D, 0.0D, 1.0D);

        OPTIMIZE_SOUNDS = builder
                .comment(
                        "Prevents redundant sound playback during the fight without muting",
                        "important cues. Currently deduplicates repeated combat-impact sounds",
                        "fired on the same position within the same tick."
                )
                .define("optimizeHerobrineSounds", true);

        OPTIMIZE_TICK_WORK = builder
                .comment(
                        "Throttles redundant per-tick work issued by Herobrine's AI loop",
                        "(repeated spawnpoint / stopsound / bossbar-value commands and the",
                        "phase-2 particle command). The boss's behaviour is unchanged; only",
                        "the frequency of these cosmetic/refresh commands is reduced."
                )
                .define("optimizeHerobrineTickWork", true);

        builder.pop();
        SPEC = builder.build();
    }

    private HerobrineConfig() {}

    /** Whether all Herobrine Prime boss-fight optimizations are enabled. */
    public static boolean optimizeFight() {
        return OPTIMIZE_FIGHT.get();
    }

    /** Whether redundant sound playback should be deduplicated. */
    public static boolean optimizeSounds() {
        return optimizeFight() && OPTIMIZE_SOUNDS.get();
    }

    /** Whether redundant per-tick AI commands should be throttled. */
    public static boolean optimizeTickWork() {
        return optimizeFight() && OPTIMIZE_TICK_WORK.get();
    }

    /** Multiplier for ordinary combat particle counts, clamped to [0,1]. */
    public static double particleMultiplier() {
        if (!optimizeFight()) return 1.0D;
        return clamp(PARTICLE_MULTIPLIER.get());
    }

    /** Multiplier for heavy particle bursts, clamped to [0,1]. */
    public static double heavyEffectMultiplier() {
        if (!optimizeFight()) return 1.0D;
        return clamp(HEAVY_EFFECT_MULTIPLIER.get());
    }

    private static double clamp(double v) {
        if (v < 0.0D) return 0.0D;
        if (v > 1.0D) return 1.0D;
        return v;
    }
}
