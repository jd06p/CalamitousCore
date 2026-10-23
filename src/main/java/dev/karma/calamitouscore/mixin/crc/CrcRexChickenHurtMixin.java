package dev.karma.calamitouscore.mixin.crc;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dedicated-server crash fix for the CRC CyberChicken boss fight.
 *
 * <p>CRC's {@code RexChicken.m_6469_(DamageSource, float)} override calls
 * {@code ForgeHooks.onLivingHurt(this, source, amount)} unconditionally, on both the
 * logical server and the logical client. Vanilla {@code LivingEntity.hurt} only fires
 * that hook inside an {@code if (!this.level().isClientSide)} block, so CRC's override
 * deviates from vanilla semantics by firing {@code LivingHurtEvent} during the client's
 * local attack prediction (the client runs {@code MultiPlayerGameMode} -&gt;
 * {@code Player.attack} -&gt; {@code RexPart.hurt} -&gt; {@code RexChicken.hurt}).
 *
 * <p>Brutality listens to {@code LivingHurtEvent} and unconditionally calls
 * {@code PacketHandler.sendToAllClients(...)}, which routes through
 * {@code PacketDistributor.ALL} -&gt; {@code PacketDistributor.getServer()}. On the
 * physical client no server exists, so {@code getServer()} returns {@code null} and the
 * broadcast throws
 * {@code NullPointerException: ... because the return value of
 * "net.minecraftforge.network.PacketDistributor.getServer()" is null}, disconnecting the
 * client the moment the player hits the boss.
 *
 * <p>This redirect restores vanilla semantics: the Forge hook is only invoked when the
 * boss is not client-side. On the client the incoming amount is passed through
 * unchanged, exactly as vanilla would (vanilla does not fire the event client-side and
 * leaves the amount untouched). Server-side behaviour is completely unchanged, so the
 * boss encounter, its AI, attacks and damage all stay intact.
 */
@Pseudo
@Mixin(targets = "com.TBK.crc.server.entity.RexChicken", remap = false)
public abstract class CrcRexChickenHurtMixin {

    @Redirect(
            method = "m_6469_",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraftforge/common/ForgeHooks;onLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F"),
            remap = false)
    private float calamitouscore$hurtHookServerSideOnly(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide) {
            return amount;
        }
        return ForgeHooks.onLivingHurt(entity, source, amount);
    }
}
