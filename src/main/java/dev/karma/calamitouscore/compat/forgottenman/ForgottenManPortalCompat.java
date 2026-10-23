package dev.karma.calamitouscore.compat.forgottenman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "calamitouscore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgottenManPortalCompat {

    private record Pending(ServerPlayer player, ServerLevel target,
                           double x, double y, double z, float yaw, float pitch) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    public static void deferTeleport(ServerPlayer player, ServerLevel target,
                                     double x, double y, double z, float yaw, float pitch) {
        if (player == null || target == null) {
            return;
        }
        PENDING.removeIf(pending -> pending.player() == player);
        PENDING.add(new Pending(player, target, x, y, z, yaw, pitch));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            iterator.remove();
            ServerPlayer player = pending.player();
            if (player == null || player.isRemoved()) {
                continue;
            }
            if (player.level() == pending.target()) {
                continue;
            }
            player.teleportTo(pending.target(), pending.x(), pending.y(), pending.z(),
                    pending.yaw(), pending.pitch());
        }
    }

    private ForgottenManPortalCompat() {
    }
}
