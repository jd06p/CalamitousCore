package dev.karma.calamitouscore.mixin.difficulty;

import dev.karma.calamitouscore.compat.difficulty.SelectorPalette;
import com.misanthropy.linggango.difficulty_enhancement.client.ui.Coffee;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "com.misanthropy.linggango.difficulty_enhancement.client.ui.ModifierScreen", remap = false)
public abstract class ModifierScreenPaletteMixin {
    @Redirect(method = "renderCard", at = @At(value = "INVOKE",
        target = "Lcom/misanthropy/linggango/difficulty_enhancement/client/ui/Coffee;marquee(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIIZZ)V"))
    private void calamitouscore$cardSwitch(GuiGraphics g, Font font, String name, int x, int y,
            int width, int color, boolean shadow, boolean center, GuiGraphics cardGraphics,
            @Coerce Object card, boolean removed, int cardX, int cardY, int height,
            float hover, float enabled, boolean locked) {
        boolean fits = width >= 45 && !locked;
        Coffee.marquee(g, font, name, x, y, fits ? width - 17 : width, color, shadow, center);
        if (fits) {
            int left = x + width - 14;
            int rail = SelectorPalette.selector(enabled >= 0.5F ? 0x707070 : 0x333333);
            int knob = left + 1 + Math.round(7 * Math.max(0, Math.min(1, enabled)));
            g.fill(left, y, left + 14, y + 7, 0xFF000000 | rail);
            g.fill(knob, y + 1, knob + 5, y + 6, 0xFFFFFFFF);
        }
    }

    @ModifyConstant(method = {"renderKeep", "renderPresetPage", "renderDialog", "draw", "renderAsk", "renderMeter", "renderSidebar", "renderPerks", "renderLadder", "renderGrid", "renderCard"},
        constant = {
            @Constant(intValue = 0x8B8B8B),
            @Constant(intValue = 0x3F3F3F),
            @Constant(intValue = 0x6B6B6B),
            @Constant(intValue = 0x262626),
            @Constant(intValue = 0x1C1C1C),
            @Constant(intValue = 0x141414),
            @Constant(intValue = 0x0A0A0A),
            @Constant(intValue = 0x373737),
            @Constant(intValue = 0x2A2A2A),
            @Constant(intValue = 0x333333),
            @Constant(intValue = 0x707070),
            @Constant(intValue = 0x767676),
            @Constant(intValue = 0x2C2C2C),
            @Constant(intValue = 0xFFFF55),
            @Constant(intValue = 0xFFAA00),
            @Constant(intValue = 0xA0A0A0),
            @Constant(intValue = 0x808080)
        })
    private int calamitouscore$theme(int original) {
        return SelectorPalette.selector(original);
    }
}
