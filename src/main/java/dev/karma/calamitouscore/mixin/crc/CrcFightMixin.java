package dev.karma.calamitouscore.mixin.crc;

import com.TBK.crc.server.entity.RexChicken;
import com.TBK.crc.common.Util;
import com.TBK.crc.server.StructureData;
import dev.karma.calamitouscore.compat.crc.CrcFightTimer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "com.TBK.crc.server.fight.CyberChickenFight", remap = false)
public abstract class CrcFightMixin implements CrcFightTimer {
    @Shadow private ServerLevel level;
    @Shadow private java.util.UUID chickenUUID;
    @Shadow private boolean cyberChickenDefeat, prevCyberChickenDefeat, needsStateScanning;
    @Shadow private int ticksChickenSeen;
    @Shadow @Final private BlockPos origin;
    @Shadow protected abstract RexChicken createNewRex();
    @Unique private long calamitouscore$due;
    @Unique private java.util.UUID calamitouscore$lastDeath;
    @Unique private int calamitouscore$arenaWarmup;
    @Unique private static final net.minecraft.server.level.TicketType<Long> calamitouscore$ticket =
            net.minecraft.server.level.TicketType.create("calamitouscore_crc_respawn", Long::compareTo, 80);

    @Inject(method = "tick", at = @At("HEAD"))
    private void calamitouscore$holdDefeatedState(CallbackInfo ci) {
        // CRC infers defeat from an exit portal during its first scan after restart.
        // A temporary portal must not cause that scan to bypass a pending countdown.
        if (calamitouscore$due > 0) { needsStateScanning = false; cyberChickenDefeat = true; }
    }

    @Inject(method = "setDragonKilled", at = @At("TAIL"))
    private void calamitouscore$schedule(RexChicken rex, CallbackInfo ci) {
        if (level == null || !rex.getUUID().equals(chickenUUID) || !cyberChickenDefeat
                || rex.getUUID().equals(calamitouscore$lastDeath)) return;
        calamitouscore$lastDeath = rex.getUUID();
        calamitouscore$due = level.getServer().overworld().getGameTime() + 1800;
        Component message = Component.literal("THE REX CHICKEN WILL APPEAR AGAIN IN 90 SECONDS!")
                .withStyle(style -> style.withColor(0x55AAFF));
        java.util.Set<net.minecraft.server.level.ServerPlayer> recipients = new java.util.HashSet<>(level.players());
        if (rex.getKillCredit() instanceof net.minecraft.server.level.ServerPlayer killer) recipients.add(killer);
        recipients.forEach(p -> p.sendSystemMessage(message));
        StructureData.get().setDirty();
    }
    @Inject(method = "serialise", at = @At("RETURN"))
    private void calamitouscore$save(CallbackInfoReturnable<CompoundTag> ci) {
        ci.getReturnValue().putLong("CalamitousCoreRexDue", calamitouscore$due);
        if (calamitouscore$lastDeath != null) ci.getReturnValue().putUUID("CalamitousCoreRexDeath", calamitouscore$lastDeath);
    }
    @Inject(method = "deserialise", at = @At("TAIL"))
    private void calamitouscore$load(CompoundTag tag, CallbackInfo ci) {
        calamitouscore$due = tag.contains("CalamitousCoreRexDue") ? tag.getLong("CalamitousCoreRexDue") : -1;
        calamitouscore$lastDeath = tag.hasUUID("CalamitousCoreRexDeath") ? tag.getUUID("CalamitousCoreRexDeath") : null;
    }
    @Override public void calamitouscore$serverTick() {
        if (level == null || calamitouscore$due <= 0) return;
        long now = level.getServer().overworld().getGameTime();
        if (now < calamitouscore$due - 40) return;
        // Preload before the deadline and allow entity data to attach before checking for duplicates.
        level.getChunkSource().addRegionTicket(calamitouscore$ticket, new net.minecraft.world.level.ChunkPos(origin), 9, 0L);
        if (calamitouscore$arenaWarmup++ == 0)
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) level.getChunk(x, z);
        if (now < calamitouscore$due || calamitouscore$arenaWarmup < 20) return;
        java.util.List<? extends RexChicken> bosses = Util.getRexChickens(level).stream()
                .filter(rex -> !rex.isDeath() && !rex.getUUID().equals(calamitouscore$lastDeath)).toList();
        RexChicken rex = bosses.isEmpty() ? createNewRex() : bosses.get(0);
        if (rex == null || rex.isRemoved() || level.getEntity(rex.getUUID()) != rex) return; // Retry on spawn failure.
        chickenUUID = rex.getUUID();
        cyberChickenDefeat = prevCyberChickenDefeat = needsStateScanning = false;
        ticksChickenSeen = 0;
        calamitouscore$due = -1;
        calamitouscore$arenaWarmup = 0;
        level.getChunkSource().removeRegionTicket(calamitouscore$ticket, new net.minecraft.world.level.ChunkPos(origin), 9, 0L);
        StructureData.get().setDirty();
    }
}
