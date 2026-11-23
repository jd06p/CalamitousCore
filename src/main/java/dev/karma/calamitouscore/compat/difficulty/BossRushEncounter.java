package dev.karma.calamitouscore.compat.difficulty;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class BossRushEncounter {
    public static boolean summon(ServerPlayer player) {
        var pool = BossRushConfig.pool();
        if (pool.isEmpty()) return true;
        ServerLevel level = player.serverLevel();
        var random = level.getRandom();
        var type = pool.get(random.nextInt(pool.size()));
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = 16 + random.nextInt(8);
            BlockPos column = player.blockPosition().offset((int)(Math.cos(angle) * distance), 0,
                (int)(Math.sin(angle) * distance));
            if (!level.hasChunkAt(column)) continue;
            BlockPos start = level.dimensionType().hasCeiling() ? column
                : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            int search = level.dimensionType().hasCeiling() ? 16 : 0;
            for (int i = 0; i <= search; i++) {
                int offset = i % 2 == 0 ? i / 2 : -(i + 1) / 2;
                BlockPos pos = start.above(offset);
                if (!clear(level, type.getAABB(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5))
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
                var entity = type.create(level, null, null, pos, MobSpawnType.EVENT, false, false);
                if (!(entity instanceof Mob mob)) {
                    BossRushConfig.reject(type);
                    return false;
                }
                if (!clear(level, mob.getBoundingBox())) continue;
                if (!level.tryAddFreshEntityWithPassengers(mob) || !mob.isAddedToWorld()) return false;
                mob.setTarget(player);
                mob.setPersistenceRequired();
                player.sendSystemMessage(Component.translatable("difficulty_enhancement.status.boss_rush",
                    mob.getDisplayName()).withStyle(ChatFormatting.RED));
                return true;
            }
        }
        return false;
    }

    private static boolean clear(ServerLevel level, AABB box) {
        return box.minY > level.getMinBuildHeight() && box.maxY <= level.getMaxBuildHeight()
            && level.getWorldBorder().isWithinBounds(box.minX, box.minZ)
            && level.getWorldBorder().isWithinBounds(box.maxX, box.maxZ)
            && level.hasChunksAt(BlockPos.containing(box.minX-1, box.minY-1, box.minZ-1),
                BlockPos.containing(box.maxX+1, box.maxY+1, box.maxZ+1))
            && level.noCollision(box) && !level.containsAnyLiquid(box);
    }
}
