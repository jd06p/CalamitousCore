package dev.karma.calamitouscore.mixin.lunar;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mrbysco.lunar.client.MoonHandler;
import dev.karma.calamitouscore.compat.lunar.LunarConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Disables only Lunar's event-specific moon <em>color tint</em>, while leaving
 * Lunar's custom moon textures and the rest of its rendering pipeline fully
 * intact.
 *
 * <p>Lunar applies its lunar-event visuals through the client-only
 * {@code com.mrbysco.lunar.client.MoonHandler}. Crucially, the event color and
 * the event texture are two <em>independent</em> mechanisms:
 *
 * <ul>
 *   <li>{@code colorTheMoon(...)} calls
 *       {@code RenderSystem.setShaderColor(moonColor[0], moonColor[1], moonColor[2], 1)}
 *       exactly once (bytecode offset 22), guarded by {@code isEventActive()}.
 *       This is the dynamic color multiplier / tint layered on top of whatever
 *       moon texture is currently bound (e.g. red over the Blood Moon).</li>
 *   <li>{@code getMoonTexture(defaultTexture)} selects the event's custom
 *       texture (e.g. {@code lunar:textures/environment/blood.png}). This is
 *       the texture selection, and it is what makes Lunar's custom moon art -
 *       including any custom art supplied through a resource pack - appear.</li>
 * </ul>
 *
 * <p>Because the tint is a {@code setShaderColor} multiplier applied to the
 * already-correctly-bound texture, removing the tint requires <em>only</em>
 * neutralizing that one call to {@code (1, 1, 1, 1)}. The custom texture keeps
 * rendering with its original colors; nothing about texture selection, size,
 * orientation, phase or transparency is touched.
 *
 * <p>This mixin therefore {@code @Redirect}s the single
 * {@code RenderSystem.setShaderColor} invocation inside {@code colorTheMoon}:
 * when {@link LunarConfig#disableLunarMoonTint()} is {@code true} it substitutes
 * neutral {@code (1, 1, 1, 1)}; otherwise it passes Lunar's original values
 * through unchanged. {@code colorTheMoon} still runs to completion - only the
 * color values differ - so Lunar's control flow and event state are untouched.
 *
 * <p>The previous, over-broad implementation additionally cancelled
 * {@code getMoonTexture} to force the vanilla texture; that removed Lunar's
 * custom event textures (and any resource-pack moon art) as well. That
 * interception has been removed entirely.
 *
 * <p>This mixin is {@code @Pseudo} and lives in the {@code client} mixin list, so
 * it is silently skipped when Lunar is absent and is never applied on a
 * dedicated server. The sun, stars, clouds, fog, terrain and other mods'
 * rendering are never affected, because the redirect is scoped to Lunar's own
 * {@code colorTheMoon} method.
 */
@Pseudo
@Mixin(value = MoonHandler.class, remap = false)
public abstract class LunarMoonRenderingMixin {

    @Redirect(method = "colorTheMoon", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    private static void calamitouscore$neutralizeMoonTint(float r, float g, float b, float a) {
        if (LunarConfig.disableLunarMoonTint()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            RenderSystem.setShaderColor(r, g, b, a);
        }
    }
}
