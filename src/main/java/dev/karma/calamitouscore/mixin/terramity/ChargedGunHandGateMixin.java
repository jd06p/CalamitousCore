package dev.karma.calamitouscore.mixin.terramity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {
    "net.mcreator.terramity.procedures.RailgunRightclickedProcedure",
    "net.mcreator.terramity.procedures.PlanetBusterRightclickedProcedure"
}, remap = false)
public abstract class ChargedGunHandGateMixin {
    @Redirect(method = {"lambda$execute$0", "lambda$execute$1"}, require = 2, allow = 2,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21206_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack calamitouscore$chargedHand(LivingEntity shooter) {
        return ItemStack.EMPTY;
    }
}
