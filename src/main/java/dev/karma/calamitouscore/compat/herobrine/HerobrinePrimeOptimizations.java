package dev.karma.calamitouscore.compat.herobrine;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Central runtime helper shared by every Herobrine Prime optimization mixin.
 *
 * <p>Herobrine Prime is a MCreator mod, so almost all of its "effects" are
 * implemented by dispatching vanilla commands through
 * {@link Commands#performPrefixedCommand} on the server command dispatcher.
 * The expensive ones are almost always {@code particle} commands, frequently
 * issued inside tight loops (per arc segment, per point on an expanding
 * sphere/ring, per beam point, every tick).
 *
 * <p>This class never removes a gameplay action. It rewrites only the
 * <em>count</em> argument of {@code particle} commands, and it can suppress a
 * small set of purely cosmetic commands that the AI re-issues on every tick
 * (spawnpoint refreshes, stop-sound refreshes, the phase-2 flame aura and the
 * bossbar value refresh). Damage assignment, attack timing, hitboxes,
 * animations, phases, lightning, chats and bossbar creation are untouched.
 */
public final class HerobrinePrimeOptimizations {

    private HerobrinePrimeOptimizations() {}

    // ------------------------------------------------------------------
    // Particle command rewriting
    // ------------------------------------------------------------------

    /**
     * Runs a command issued from an <em>ordinary</em> combat context (arc/melee
     * trails, soul-charge trails, beam points, the phase-2 aura).
     */
    public static int runOrdinary(Commands commands, CommandSourceStack source, String command) {
        return run(commands, source, command, false);
    }

    /**
     * Runs a command issued from a <em>heavy</em> visual context (the explosion
     * sphere and the shockwave ring), which receive the stronger multiplier.
     */
    public static int runHeavy(Commands commands, CommandSourceStack source, String command) {
        return run(commands, source, command, true);
    }

    /**
     * Runs a command from Herobrine's AI loop. Identical to {@link #runOrdinary}
     * but also eligible for the per-tick throttle list.
     */
    public static int runAiCommand(Commands commands, CommandSourceStack source, String command) {
        if (command != null && shouldThrottle(command)) {
            return 0;
        }
        return run(commands, source, command, false);
    }

    private static int run(Commands commands, CommandSourceStack source, String command, boolean heavy) {
        if (command == null) {
            return commands.performPrefixedCommand(source, command);
        }
        String rewritten = rewriteParticleCommand(command, heavy);
        if (rewritten == null) {
            // Particle command scaled to zero.
            return 0;
        }
        return commands.performPrefixedCommand(source, rewritten);
    }

    /**
     * Rewrites a {@code particle} command so its spawn count is scaled by the
     * configured multiplier. Non-particle commands are returned unchanged.
     * Returns {@code null} if the command should be skipped entirely.
     */
    public static String rewriteParticleCommand(String command, boolean heavy) {
        String trimmed = command.trim();
        if (!trimmed.startsWith("particle ")) {
            return command;
        }

        double multiplier = heavy
                ? HerobrineConfig.heavyEffectMultiplier()
                : HerobrineConfig.particleMultiplier();
        if (multiplier >= 1.0D) {
            return command;
        }
        if (multiplier <= 0.0D) {
            return null;
        }

        // particle <name> <x> <y> <z> <dx> <dy> <dz> <speed> <count> [force|normal] [<viewers>]
        // token index: 0 particle, 1 name, 2-4 pos, 5-7 delta, 8 speed, 9 count
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length < 10) {
            // Positional shorthand without an explicit count; nothing to scale.
            return command;
        }

        int countIndex = 9;
        int originalCount;
        try {
            originalCount = Integer.parseInt(tokens[countIndex]);
        } catch (NumberFormatException e) {
            return command;
        }
        if (originalCount <= 0) {
            return command;
        }

        int scaled = (int) Math.ceil(originalCount * multiplier);
        if (scaled < 1) {
            scaled = 1;
        }
        if (scaled >= originalCount) {
            return command;
        }
        tokens[countIndex] = Integer.toString(scaled);
        return String.join(" ", tokens);
    }

    // ------------------------------------------------------------------
    // Heavy sphere/ring point reduction
    // ------------------------------------------------------------------

    private static final java.util.concurrent.atomic.AtomicLong HEAVY_POINT_COUNTER =
            new java.util.concurrent.atomic.AtomicLong();

    /**
     * Whether the next point of an expanding explosion sphere or shockwave ring
     * should be processed.
     *
     * <p>These effects sample a fraction of their surface points every tick,
     * where each point both spawns a particle and scans for players inside a
     * small box. The total point count scales with {@code radius^2} (explosion)
     * or {@code radius} (shockwave), reaching tens of thousands of iterations
     * per tick. Skipping a fraction of the points reduces both the particle
     * traffic and the entity-scan cost.
     *
     * <p>Because the original sampling density is very high, the remaining
     * points still densely cover the surface, so the damage assignment (which
     * only needs <em>one</em> point to overlap a player) is preserved.
     */
    public static boolean shouldRunHeavyPoint() {
        double multiplier = HerobrineConfig.heavyEffectMultiplier();
        if (multiplier >= 1.0D) {
            return true;
        }
        if (multiplier <= 0.0D) {
            return false;
        }
        long denom = Math.max(1L, Math.round(1.0D / multiplier));
        long n = HEAVY_POINT_COUNTER.getAndIncrement();
        return Math.floorMod(n, denom) == 0L;
    }

    // ------------------------------------------------------------------
    // Redundant per-tick command throttling
    // ------------------------------------------------------------------

    /**
     * Decides whether a purely-cosmetic, per-tick command should be skipped on
     * this particular invocation. Returns {@code true} to skip.
     *
     * <p>Only a very small, explicitly-listed set is eligible:
     * <ul>
     *   <li>{@code spawnpoint @a ~ ~ ~} – a full command parse + player scan
     *       executed every tick for no benefit (the spawn point does not move).</li>
     *   <li>{@code stopsound @a music} – re-issued every tick.</li>
     *   <li>{@code bossbar set ... value <n>} – refreshed every tick; the client
     *       bossbar already interpolates, so refreshing a few times per second
     *       is visually identical.</li>
     *   <li>the phase-2 {@code particle soul_fire_flame ... 64 force} aura –
     *       re-issued every tick at a very large count.</li>
     * </ul>
     */
    public static boolean shouldThrottle(String command) {
        if (command == null || !HerobrineConfig.optimizeTickWork()) {
            return false;
        }
        String trimmed = command.trim();

        if (trimmed.startsWith("spawnpoint ")) {
            return true;
        }
        if (trimmed.startsWith("stopsound ")) {
            return true;
        }
        if (trimmed.startsWith("bossbar set ") && trimmed.contains(" value ")) {
            // Refresh the interpolating bossbar at most ~4x per second.
            return !interval("bossbar-value", 5);
        }
        if (trimmed.startsWith("particle soul_fire_flame ")) {
            // Phase-2 flame aura: keep it but only every few ticks.
            return !interval("phase2-aura", 3);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Sound de-duplication
    // ------------------------------------------------------------------

    private static final Map<SoundKey, Long> LAST_SOUND_TICK = new WeakHashMap<>();

    /**
     * Returns {@code true} if a given sound should be allowed to play on this
     * tick, {@code false} if an identical cue already played on the same tick.
     *
     * <p>The key is the sound's own identity (per dimension). Because the
     * redundant playback this guards against is a single combat cue fired many
     * times in one tick from a hovering boss, position is intentionally not part
     * of the key: distinct cues are unaffected, and genuinely different cues in
     * the same tick still play.
     */
    public static boolean allowSound(net.minecraft.world.level.Level level, Object sound) {
        if (!HerobrineConfig.optimizeSounds() || level == null || sound == null) {
            return true;
        }
        long tick = level.getGameTime();
        SoundKey key = new SoundKey(level, sound);
        Long last = LAST_SOUND_TICK.get(key);
        if (last != null && last == tick) {
            return false;
        }
        LAST_SOUND_TICK.put(key, tick);
        if (LAST_SOUND_TICK.size() > 4096) {
            LAST_SOUND_TICK.clear();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Per-tick interval helper (game-time based)
    // ------------------------------------------------------------------

    private static final Map<String, Long> LAST_RUN = new WeakHashMap<>();

    private static boolean interval(String key, int everyTicks) {
        long now = nowTicks();
        if (now < 0L) {
            return true;
        }
        Long last = LAST_RUN.get(key);
        if (last != null && now - last < everyTicks) {
            return false;
        }
        LAST_RUN.put(key, now);
        if (LAST_RUN.size() > 256) {
            LAST_RUN.clear();
        }
        return true;
    }

    private static long nowTicks() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return -1L;
        }
        net.minecraft.server.level.ServerLevel overworld = server.overworld();
        return overworld == null ? -1L : overworld.getGameTime();
    }

    private record SoundKey(Object level, Object sound) {}
}
