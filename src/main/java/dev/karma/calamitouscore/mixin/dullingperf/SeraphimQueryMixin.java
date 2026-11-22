package dev.karma.calamitouscore.mixin.dullingperf;

import dev.karma.calamitouscore.compat.dulling.performance.DullingFrameQueries;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "net.mcreator.thedulling.client.SeraphimWarningOverlay", remap = false)
public abstract class SeraphimQueryMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", remap = true))
    private static <T extends Entity> List<T> calamitouscore$frameQuery(ClientLevel level, Class<T> type, AABB box) {
        return DullingFrameQueries.seraphs(level, type, box);
    }
}
