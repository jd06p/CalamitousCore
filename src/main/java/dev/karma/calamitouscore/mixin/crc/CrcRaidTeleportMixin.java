package dev.karma.calamitouscore.mixin.crc;

import dev.karma.calamitouscore.compat.crc.CrcRaidData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.TBK.crc.server.entity.TeleportEntity", remap = false)
public abstract class CrcRaidTeleportMixin {
    @Shadow public LivingEntity target;
    @Shadow public int generationTimer, teleportTimer;
    @Shadow public net.minecraft.world.entity.EntityType<?> type;
    @Shadow public net.minecraft.core.BlockPos blockTeleport;
    @Shadow public boolean shield;
    // CRC leaves both NBT methods empty. Persist its delayed raid spawn so reload cannot duplicate a batch.
    @Inject(method = "m_7380_", at = @At("TAIL"))
    private void calamitouscore$save(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.getPersistentData().hasUUID(CrcRaidData.OWNER)) return;
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putInt("Generation", generationTimer); data.putInt("Teleport", teleportTimer);
        data.putString("Type", net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type).toString());
        data.putBoolean("Shield", shield);
        if (blockTeleport != null) data.putLong("Position", blockTeleport.asLong());
        tag.put("CalamitousCoreRaidTeleport", data);
    }
    @Inject(method = "m_7378_", at = @At("TAIL"))
    private void calamitouscore$load(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("CalamitousCoreRaidTeleport")) return;
        net.minecraft.nbt.CompoundTag data = tag.getCompound("CalamitousCoreRaidTeleport");
        generationTimer = data.getInt("Generation"); teleportTimer = data.getInt("Teleport");
        net.minecraft.world.entity.EntityType<?> saved = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(new net.minecraft.resources.ResourceLocation(data.getString("Type")));
        if (saved != null) type = saved;
        shield = data.getBoolean("Shield");
        if (data.contains("Position")) blockTeleport = net.minecraft.core.BlockPos.of(data.getLong("Position"));
    }
    @Inject(method = "summon", at = @At("HEAD"))
    private void calamitouscore$restoreTarget(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel level && self.getPersistentData().hasUUID(CrcRaidData.OWNER))
            target = level.getServer().getPlayerList().getPlayer(self.getPersistentData().getUUID(CrcRaidData.OWNER));
    }
    @Redirect(method = "summon", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z", remap = false))
    private boolean calamitouscore$trackChicken(Level level, Entity entity) {
        Entity self = (Entity) (Object) this;
        if (level instanceof ServerLevel sl && self.getPersistentData().hasUUID(CrcRaidData.OWNER)) {
            java.util.UUID owner = self.getPersistentData().getUUID(CrcRaidData.OWNER);
            entity.getPersistentData().putUUID(CrcRaidData.OWNER, owner);
            // Raid mobs must not vanish through natural despawning while their chunks are unloaded.
            if (entity instanceof Mob mob) mob.setPersistenceRequired();
            boolean added = level.addFreshEntity(entity);
            if (added) CrcRaidData.get(sl.getServer()).add(owner, entity.getUUID());
            return added;
        }
        return level.addFreshEntity(entity);
    }
}
