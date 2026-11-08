package dev.karma.calamitouscore.mixin.crc;

import com.TBK.crc.server.capability.MultiArmCapability;
import com.TBK.crc.server.network.PacketHandler;
import com.TBK.crc.server.network.messager.PacketSyncPlayerData;
import dev.karma.calamitouscore.compat.crc.CrcRaidData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.TBK.crc.server.capability.MultiArmCapability", remap = false)
public abstract class CrcRaidMixin {
    @Shadow public boolean chickenEnemy;
    @Shadow public int wave, warningLevel, invokeTimer, timeLevelWarning, timeLevelWarning0, chickenAnimTime, chickenAnimTime0;
    @Shadow public boolean playChickenWarning, chickenSpoke;
    @Shadow Player player;
    @Shadow ServerBossEvent event;

    @Inject(method = "deserializeNBT", at = @At("TAIL"))
    private void calamitouscore$restoreClock(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("invokeTime")) invokeTimer = tag.getInt("invokeTime");
        playChickenWarning = tag.getBoolean("playAnim");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void calamitouscore$finish(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer sp)) {
            if (chickenEnemy && wave >= 3) { invokeTimer = 2; warningLevel = Math.min(warningLevel, 2); }
            return;
        }
        int count = sp.getPersistentData().getInt(CrcRaidData.COUNT);
        if (!chickenEnemy) { sp.getPersistentData().remove(CrcRaidData.COUNT); return; }
        // Migrate an already-running raid conservatively; CRC's wave is the number of batches spawned.
        count = Math.max(count, wave);
        sp.getPersistentData().putInt(CrcRaidData.COUNT, count);
        warningLevel = Math.min(warningLevel, 2);
        if (count < 3) return;
        invokeTimer = 2; // Freeze only the reinforcement clock; upgrades and other capability ticks still run.
        wave = 2;
        if (!CrcRaidData.get(sp.server).empty(sp.getUUID())) return;
        chickenEnemy = false;
        wave = warningLevel = timeLevelWarning = timeLevelWarning0 = chickenAnimTime = chickenAnimTime0 = 0;
        playChickenWarning = chickenSpoke = false;
        invokeTimer = 400;
        event.setVisible(false); event.removeAllPlayers();
        sp.getPersistentData().remove(CrcRaidData.COUNT);
        MultiArmCapability self = (MultiArmCapability) (Object) this;
        PacketHandler.sendToPlayer(new PacketSyncPlayerData(self.serializeNBT(), true, sp.getId()), sp);
    }

    @Inject(method = "teleportChicken", at = @At("HEAD"), cancellable = true)
    private void calamitouscore$countWave(CallbackInfo ci) {
        if (player.level().isClientSide) { ci.cancel(); return; }
        if (player instanceof ServerPlayer sp)
            sp.getPersistentData().putInt(CrcRaidData.COUNT, sp.getPersistentData().getInt(CrcRaidData.COUNT) + 1);
    }

    @Redirect(method = "teleportChicken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z", remap = false))
    private boolean calamitouscore$trackDelayedSpawn(Level level, Entity entity) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            entity.getPersistentData().putUUID(CrcRaidData.OWNER, sp.getUUID());
            boolean added = level.addFreshEntity(entity);
            if (added) CrcRaidData.get(sp.server).add(sp.getUUID(), entity.getUUID());
            return added;
        }
        return level.addFreshEntity(entity);
    }
}
