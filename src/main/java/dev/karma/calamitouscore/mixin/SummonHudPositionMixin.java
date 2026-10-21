package dev.karma.calamitouscore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Relocates Summonity's summon-slot HUD from the vanilla centre (where it
 * overlaps The Dulling's mana bar) to the bottom-left of the screen, sitting
 * directly above the vanilla armour bar in the same column as the hotbar and
 * health row.
 *
 * <p>Summonity's {@code renderSummonSlots} derives its origin from the two
 * scaled window dimensions:
 * <pre>x = window.getGuiScaledWidth()  / 2 + 10
 * y = window.getGuiScaledHeight() - 49</pre>
 * Both calls are redirected so the original arithmetic lands on:
 * <pre>x = window.getGuiScaledWidth()  / 2 - 91
 * y = window.getGuiScaledHeight() - leftHeight - 12</pre>
 * The {@code -91} column and {@code leftHeight}-relative baseline are exactly
 * what {@code ForgeGui} uses for the armour bar, so the summon bar stacks
 * cleanly above it at any GUI scale without overlapping the mana HUD.
 */
@Pseudo
@Mixin(targets = "com.fevzi.summonity.client.SummonHudOverlay", remap = false)
public abstract class SummonHudPositionMixin {

    /** Vertical gap between the summon bar and the top of the armour bar. */
    private static final int ARMOUR_GAP = 12;

    @Redirect(method = "renderSummonSlots", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;m_85445_()I"), remap = false)
    private static int calamitouscore$leftAlignedWidth(com.mojang.blaze3d.platform.Window window) {
        // Want x/2 + 10 == width/2 - 91, i.e. x == width - 202.
        return window.getGuiScaledWidth() - 202;
    }

    @Redirect(method = "renderSummonSlots", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;m_85446_()I"), remap = false)
    private static int calamitouscore$aboveArmourHeight(com.mojang.blaze3d.platform.Window window) {
        // Want y - 49 == height - leftHeight - 12, i.e. y == height - leftHeight + 37.
        int leftHeight = 39;
        if (Minecraft.getInstance().gui instanceof ForgeGui forgeGui) {
            leftHeight = forgeGui.leftHeight;
        }
        return window.getGuiScaledHeight() - leftHeight + 37;
    }
}
