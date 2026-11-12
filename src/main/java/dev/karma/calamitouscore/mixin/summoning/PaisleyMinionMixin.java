package dev.karma.calamitouscore.mixin.summoning;

import com.fevzi.summonity.entity.SummonedMinion;
import dev.karma.calamitouscore.compat.summoning.SharedSummonState;
import java.util.UUID;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = {"net.paisleypug.terramitypaisleyssummoning.entity.AdolescentUvogreEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.ArmedFairyEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.DwarfGnomeEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.EchoBugSummonEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.FlareFairyEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.FrostKnightSummonEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.FuzzyEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.GoombaEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.GubDukeOfGnomesEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.HomunculusSkeletonEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.LittleMageSummonEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.OrbitalEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.RoyalDwarfGnomeEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.SummonedConjurlingEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.SummonedEffigyEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.SummonedFairyEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.SummonedRoyalDwarfEntity",
"net.paisleypug.terramitypaisleyssummoning.entity.SummonedSimpleTurretEntity"}, remap = false)
public abstract class PaisleyMinionMixin extends TamableAnimal implements SummonedMinion {
    @Unique private LivingEntity calamitouscore$focus;
    protected PaisleyMinionMixin(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }
    public void setOwner(Player owner) { tame(owner); }
    public UUID summonity$getOwnerId() { return getOwnerUUID(); }
    public Player getOwner() { LivingEntity owner = super.getOwner(); return owner instanceof Player player ? player : null; }
    public void summonity$setBaseDamage(double value) { var damage = getAttribute(Attributes.ATTACK_DAMAGE); if (damage != null) damage.setBaseValue(value); }
    public void setCommandedTarget(LivingEntity target) { calamitouscore$focus = target; setTarget(target); }
    public LivingEntity getCommandedTarget() { return calamitouscore$focus; }
    public void clearFocus() { calamitouscore$focus = null; setTarget(null); }
    public int summonity$getSlotCost() { return SharedSummonState.cost(this); }
}
