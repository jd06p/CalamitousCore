package dev.karma.calamitouscore.mixin.difficulty;

import dev.karma.calamitouscore.compat.difficulty.SelectorPalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "com.misanthropy.linggango.difficulty_enhancement.client.ui.Coffee", remap = false)
public abstract class CoffeePaletteMixin {
    @ModifyConstant(method = {"frame", "well", "edge", "button", "steps", "section"},
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
    private static int calamitouscore$theme(int original) {
        return SelectorPalette.shared(original);
    }
}
