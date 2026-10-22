package dev.karma.calamitouscore.compat.lunar;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CalamitousCore configuration for the optional Lunar moon-tint tweak.
 *
 * <p>Lunar gives its lunar events a dynamic moon color via a
 * {@code RenderSystem.setShaderColor} multiplier applied on top of the event's
 * moon texture (for example a red wash over the Blood Moon art). This option
 * neutralizes that multiplier so the moon renders with its texture's original
 * colors, while every lunar event - and every custom event/resource-pack moon
 * texture - keeps working exactly as before.
 *
 * <p>Only the dynamic color multiplier is touched. The moon's texture
 * selection, position, phase selection, visibility, brightness and its
 * event-driven scale (Big/Tiny moon) are all left untouched.
 */
public final class LunarConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue DISABLE_LUNAR_MOON_TINT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Lunar moon-tint compatibility.",
                "Lunar events keep their gameplay, spawning, difficulty, custom moon",
                "textures and animations; only the dynamic event color multiplier",
                "applied over the moon is neutralized."
        ).push("lunar");

        DISABLE_LUNAR_MOON_TINT = builder
                .comment(
                        "When true, lunar events no longer apply their dynamic color tint",
                        "to the moon. Blood Moon no longer washes the moon red, but its",
                        "custom Blood Moon texture still renders with its own original",
                        "colors, as do all other event textures.",
                        "When false, Lunar's original event-specific moon tint is restored.",
                        "This affects the client's moon tint only and never disables the",
                        "lunar events or their textures."
                )
                .define("disableLunarMoonTint", true);

        builder.pop();
        SPEC = builder.build();
    }

    private LunarConfig() {}

    /** Whether Lunar's dynamic event-specific moon color tint should be neutralized. */
    public static boolean disableLunarMoonTint() {
        return DISABLE_LUNAR_MOON_TINT.get();
    }
}
