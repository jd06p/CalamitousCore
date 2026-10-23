package dev.karma.calamitouscore.mixin.crc;

import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dedicated-server crash fix for the CRC CyberChicken ({@code RexChicken})
 * stomp/ground-slam attack.
 *
 * <p>Inside {@code RexChicken.m_8119_()} (the entity tick), when the boss is
 * performing its stomp attack, CRC scans the diagonal column of blocks in front
 * of itself. If every scanned block is air it sets the local {@code canSummon}
 * flag and then, guarded by {@code if (canSummon)}, spawns block-break particles
 * with:
 *
 * <pre>{@code
 * Minecraft.getInstance().particleEngine.destroy(pos, Direction.UP);
 * }</pre>
 *
 * <p>That expression is executed on <em>both</em> logical sides and is not
 * guarded by {@code isClientSide}. On a dedicated server the JVM therefore tries
 * to load {@code net.minecraft.client.Minecraft} while ticking the boss, and
 * Forge's {@code RuntimeDistCleaner} aborts the tick:
 *
 * <pre>
 * java.lang.RuntimeException: Attempted to load class net/minecraft/client/Minecraft
 *     for invalid dist DEDICATED_SERVER
 *   at com.TBK.crc.server.entity.RexChicken.m_8119_(RexChicken.java:471)
 * </pre>
 *
 * <p>This is a distinct CRC defect from the earlier boss <em>hurt</em> crash
 * (handled by {@code CrcRexChickenHurtMixin}). The particle effect is purely
 * client-side decoration, so the fix forces the {@code canSummon} gate that
 * guards it to {@code false} on the dedicated server and leaves it untouched on
 * the client. No gameplay state is changed: {@code canSummon} is a throwaway
 * local used only to decide whether to emit the decorative particles.
 *
 * <p>The handler operates purely on a {@code boolean} local and therefore this
 * common mixin contains no reference to any {@code net.minecraft.client} type,
 * so it loads and applies cleanly on both the client and the dedicated server.
 */
@Pseudo
@Mixin(targets = "com.TBK.crc.server.entity.RexChicken", remap = false)
public abstract class CrcRexChickenTickMixin {

    @ModifyVariable(
            method = "m_8119_",
            at = @At(value = "STORE"),
            name = "canSummon",
            remap = false)
    private boolean calamitouscore$suppressServerStompParticles(boolean canSummon) {
        if (FMLEnvironment.dist.isClient()) {
            return canSummon;
        }
        return false;
    }
}
