package dev.karma.calamitouscore.mixin.terramity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// This procedure's only effect is the selected gun's occupied-offhand action-bar warning.
@Pseudo
@Mixin(targets = "net.mcreator.terramity.procedures.TwoHandedGunProcProcedure", remap = false)
public abstract class GunHandWarningMixin {
    @Redirect(method = "execute", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21206_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack calamitouscore$warningHand(LivingEntity shooter) {
        return ItemStack.EMPTY;
    }
}
