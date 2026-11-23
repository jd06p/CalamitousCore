package dev.karma.calamitouscore.mixin.terramity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// MCreator inlines the same comparison in separate classes. Each execute method
// below has exactly one offhand read, used solely to compare its item with air.
@Pseudo
@Mixin(targets = {
    "net.mcreator.terramity.procedures.AdvancedAutomaticRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.AdvancedBurstRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.AdvancedPistolRightclickedProcedure",
    "net.mcreator.terramity.procedures.AntiMaterialRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.AntimatterRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.AsphodelRightclickedProcedure",
    "net.mcreator.terramity.procedures.BasicPistolRightclickedProcedure",
    "net.mcreator.terramity.procedures.BasicRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.BigIronRightclickedProcedure",
    "net.mcreator.terramity.procedures.BlasphemicRaptureRightclickedProcedure",
    "net.mcreator.terramity.procedures.BlunderbussRightclickedProcedure",
    "net.mcreator.terramity.procedures.CelestialSixtyRightclickedProcedure",
    "net.mcreator.terramity.procedures.ConductiteLaserRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.CosmicStormRightclickedProcedure",
    "net.mcreator.terramity.procedures.DavyJonesRightclickedProcedure",
    "net.mcreator.terramity.procedures.DivineInterventionRightclickedProcedure",
    "net.mcreator.terramity.procedures.EliteRifleRightclickedProcedure",
    "net.mcreator.terramity.procedures.FiveThousandMagnumRightclickedProcedure",
    "net.mcreator.terramity.procedures.FlintlockPistolRightclickedProcedure",
    "net.mcreator.terramity.procedures.FortunesFavorRightclickedProcedure",
    "net.mcreator.terramity.procedures.GaiasTempestRightclickedProcedure",
    "net.mcreator.terramity.procedures.HellfireFlurryRightclickedProcedure",
    "net.mcreator.terramity.procedures.NihilityRightclickedProcedure",
    "net.mcreator.terramity.procedures.OlympusRightclickedProcedure",
    "net.mcreator.terramity.procedures.OnyxStormRightclickedProcedure",
    "net.mcreator.terramity.procedures.PlaguePistolRightclickedProcedure",
    "net.mcreator.terramity.procedures.PlaguebringerRightclickedProcedure",
    "net.mcreator.terramity.procedures.PlanetBusterRightclickedProcedure",
    "net.mcreator.terramity.procedures.PumpActionShotgunRightclickedProcedure",
    "net.mcreator.terramity.procedures.RailgunRightclickedProcedure",
    "net.mcreator.terramity.procedures.RocketLauncherRightclickedProcedure",
    "net.mcreator.terramity.procedures.SawedOffShotgunRightclickedProcedure",
    "net.mcreator.terramity.procedures.SixShooterRightclickedProcedure",
    "net.mcreator.terramity.procedures.StairwayToHeavenRightclickedProcedure",
    "net.mcreator.terramity.procedures.SuppressedAdvancedPistolRightclickedProcedure",
    "net.mcreator.terramity.procedures.TitanomachyRightclickedProcedure"
}, remap = false)
public abstract class GunHandGateMixin {
    @Redirect(method = "execute", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21206_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack calamitouscore$availableHand(LivingEntity shooter) {
        return ItemStack.EMPTY;
    }
}
