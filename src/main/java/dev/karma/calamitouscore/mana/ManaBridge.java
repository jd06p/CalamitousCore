package dev.karma.calamitouscore.mana;

import net.mcreator.thedulling.ManaPlayerData;
import net.mcreator.thedulling.client.ManaClientData;
import net.mcreator.thedulling.init.ThedullingModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes Brutality's mana API onto The Dulling's mana pool so both mods share a
 * single authoritative resource.
 *
 * <p>In Brutality 0.8.5 the player mana pool lives in the
 * {@code BrutalityCapabilities.PLAYER_MANA_CAP} capability
 * ({@code net.goo.brutality.entity.capabilities.EntityCapabilities$PlayerManaCap}),
 * a plain {@code float}. This bridge keeps The Dulling's integer NBT value
 * ({@code thedulling_current_mana}) as the authoritative pool and mirrors the
 * capability's float onto it; the fractional remainder that cannot be stored in
 * the integer pool is kept in a transient per-player fraction so no mana is lost
 * or gained to rounding.
 *
 * <p>Brutality's {@code brutality:max_mana} attribute (base 100 plus item and
 * curio bonuses) is mirrored onto The Dulling's {@code thedulling:mana}
 * attribute (base 0) with a stable transient modifier, so the shared pool grows
 * with Brutality progression while The Dulling's own permanent mana bonuses
 * keep stacking on top.
 *
 * <p>Brutality is not a compile-time dependency, so the capability and its
 * methods are accessed reflectively. Registry lookups use the registry IDs
 * {@code brutality:mana_regen} and {@code brutality:max_mana}, which are
 * unchanged across the repackaging.
 */
public final class ManaBridge {

    private static final UUID MAX_MANA_MIRROR_UUID = UUID.fromString("c0a57e10-4b2d-4f3e-8a11-9d5e6f7a0b21");
    private static final String MAX_MANA_MIRROR_NAME = "calamitouscore.brutality_max_mana";

    private static final UUID MANA_REGEN_BASE_UUID = UUID.fromString("a1b2c3d4-5e6f-4071-8293-a4b5c6d7e8f9");
    private static final String MANA_REGEN_BASE_NAME = "calamitouscore.brutality_mana_regen_base";

    private static final String BRUTALITY_NAMESPACE = "brutality";
    private static final String BRUTALITY_MAX_MANA = "max_mana";
    private static final String BRUTALITY_MANA_REGEN = "mana_regen";

    /** Brutality's shipped base value of {@code brutality:max_mana}. */
    private static final double BRUTALITY_BASE_MAX_MANA = 100D;
    /** Shared base maximum mana exposed through The Dulling's pool. */
    private static final double CALAMITOUS_BASE_MAX_MANA = 10D;

    /** Brutality's shipped base value of {@code brutality:mana_regen}. */
    private static final double BRUTALITY_BASE_MANA_REGEN = 10D;
    /** Shared base mana regeneration exposed to Brutality's regen routine. */
    private static final double CALAMITOUS_BASE_MANA_REGEN = 1D;

    private static final ConcurrentHashMap<UUID, Float> FRACTIONS = new ConcurrentHashMap<>();

    private ManaBridge() {
    }

    /** Reflectively resolves Brutality's mana capability, or {@code null} if absent. */
    private static Capability<?> manaCapability() {
        try {
            Class<?> capsClass = Class.forName("net.goo.brutality.registry.BrutalityCapabilities");
            Object capability = capsClass.getField("PLAYER_MANA_CAP").get(null);
            return capability instanceof Capability<?> cap ? cap : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static Optional<?> manaCap(Player player) {
        Capability<?> capability = manaCapability();
        if (capability == null || player == null) return Optional.empty();
        return player.getCapability(capability).resolve();
    }

    /**
     * Reflectively binds the owning player onto Brutality's mana capability so
     * its redirected methods can reach the shared pool. Safe to call from any
     * context that has a player; no-op if Brutality is absent.
     */
    public static void bindOwner(Player player) {
        if (player == null) return;
        try {
            manaCap(player).ifPresent(cap -> {
                if (cap instanceof dev.karma.calamitouscore.mana.ManaCapOwner owner) {
                    owner.calamitouscore$bind(player);
                }
            });
        } catch (RuntimeException ignored) {
            // Capability unavailable; leave vanilla behaviour.
        }
    }

    /**
     * Current mana of the shared pool. Server side this is The Dulling's stored
     * pool plus the uncommitted fractional remainder; client side it is the
     * value received from The Dulling's mana sync packet.
     */
    public static float getMana(Player player) {
        if (player == null) return 0F;
        if (player.level().isClientSide) return ManaClientData.getCurrentMana();

        int max = ManaPlayerData.getMaxMana(player);
        if (ManaPlayerData.isInfiniteMana(player)) return max;

        int pool = ManaPlayerData.getCurrentMana(player);
        float fraction = FRACTIONS.getOrDefault(player.getUUID(), 0F);
        return Math.min(pool + fraction, max);
    }

    /**
     * Maximum mana of the shared pool: The Dulling's {@code thedulling:mana}
     * attribute value (base 0 + permanent mana + the Brutality mirror).
     */
    public static float getMaxMana(Player player) {
        if (player == null) return 0F;
        if (player.level().isClientSide) return ManaClientData.getMaxMana();
        return ManaPlayerData.getMaxMana(player);
    }

    /**
     * Brutality's view of the current pool as a raw float (pool + fraction),
     * used to satisfy the capability's {@code manaValue()} reads without
     * clamping, mirroring Brutality's own semantics.
     */
    public static float getBrutalityMana(Player player) {
        if (player == null) return 0F;
        if (player.level().isClientSide) return ManaClientData.getCurrentMana();
        int pool = ManaPlayerData.getCurrentMana(player);
        float fraction = FRACTIONS.getOrDefault(player.getUUID(), 0F);
        return pool + fraction;
    }

    /**
     * Sets the shared pool to an absolute value on the server, splitting it into
     * The Dulling's integer pool and the transient fraction. Client side calls
     * are ignored; the server value is synchronized through The Dulling's own
     * mana sync packet (sent by {@code ManaPlayerData.setCurrentMana}).
     */
    public static void setMana(Player player, float value) {
        if (player == null) return;
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (ManaPlayerData.isInfiniteMana(serverPlayer)) return;

        int max = ManaPlayerData.getMaxMana(serverPlayer);
        double total = value;
        if (total >= max) {
            total = max;
        } else if (total <= 0D) {
            total = 0D;
        }

        UUID id = serverPlayer.getUUID();
        int pool = ManaPlayerData.getCurrentMana(serverPlayer);
        float fraction = FRACTIONS.getOrDefault(id, 0F);

        int newPool = (int) total;
        float newFraction = (float) (total - newPool);

        if (newPool == pool && Math.abs(newFraction - fraction) < 1.0E-6F) return;

        FRACTIONS.put(id, newFraction);
        if (newPool != pool) {
            // Also clamps and synchronizes the pool to the owning client.
            ManaPlayerData.setCurrentMana(serverPlayer, newPool);
        }
    }

    /**
     * Applies a fractional change to the shared pool on the server. Client side
     * calls are ignored; the server value is synchronized through The Dulling's
     * own mana sync packet (sent by {@code ManaPlayerData.setCurrentMana}).
     */
    public static void modify(Player player, float delta) {
        if (player == null || delta == 0F) return;
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (ManaPlayerData.isInfiniteMana(serverPlayer)) return;

        setMana(serverPlayer, getBrutalityMana(serverPlayer) + delta);
    }

    /** Brutality's current mana ratio against its {@code brutality:max_mana} attribute. */
    public static float getCurrentManaRatio(Player player) {
        if (player == null) return 0F;
        Attribute max = ForgeRegistries.ATTRIBUTES.getValue(
                ResourceLocation.fromNamespaceAndPath(BRUTALITY_NAMESPACE, BRUTALITY_MAX_MANA));
        double maxValue = max == null ? 0D : player.getAttributeValue(max);
        if (maxValue <= 0D) return 0F;
        return (float) (getBrutalityMana(player) / maxValue);
    }

    /** Whether the shared pool is at Brutality's max mana. */
    public static boolean isMaxMana(Player player) {
        if (player == null) return false;
        Attribute max = ForgeRegistries.ATTRIBUTES.getValue(
                ResourceLocation.fromNamespaceAndPath(BRUTALITY_NAMESPACE, BRUTALITY_MAX_MANA));
        if (max == null) return false;
        AttributeInstance instance = player.getAttribute(max);
        if (instance == null) return false;
        return getBrutalityMana(player) >= instance.getValue();
    }

    /**
     * Mirrors the player's {@code brutality:max_mana} onto The Dulling's
     * {@code thedulling:mana} attribute with a stable transient modifier.
     *
     * <p>Brutality's attribute ships with a base of {@link #BRUTALITY_BASE_MAX_MANA};
     * that shipped base is replaced with {@link #CALAMITOUS_BASE_MAX_MANA} while
     * every item and curio bonus (for example the Rune of Delta's +20) is carried
     * across untouched. Equipping or removing gear changes the read value and the
     * modifier is updated on the next call, so no stale cap is left behind.
     */
    public static void applyMaxManaMirror(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(ThedullingModAttributes.MANA.get());
        if (instance == null) return;

        Attribute brutalityMaxMana = ForgeRegistries.ATTRIBUTES.getValue(
                ResourceLocation.fromNamespaceAndPath(BRUTALITY_NAMESPACE, BRUTALITY_MAX_MANA));
        double desired = CALAMITOUS_BASE_MAX_MANA;
        if (brutalityMaxMana != null) {
            double brutalityValue = player.getAttributeValue(brutalityMaxMana);
            desired = brutalityValue - BRUTALITY_BASE_MAX_MANA + CALAMITOUS_BASE_MAX_MANA;
        }
        if (desired < 0D) desired = 0D;

        AttributeModifier existing = instance.getModifier(MAX_MANA_MIRROR_UUID);
        if (existing == null) {
            if (desired != 0D) {
                instance.addTransientModifier(new AttributeModifier(
                        MAX_MANA_MIRROR_UUID, MAX_MANA_MIRROR_NAME, desired, AttributeModifier.Operation.ADDITION));
            }
        } else if (existing.getAmount() != desired) {
            instance.removeModifier(MAX_MANA_MIRROR_UUID);
            if (desired != 0D) {
                instance.addTransientModifier(new AttributeModifier(
                        MAX_MANA_MIRROR_UUID, MAX_MANA_MIRROR_NAME, desired, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /**
     * Lowers Brutality's base {@code brutality:mana_regen} from
     * {@link #BRUTALITY_BASE_MANA_REGEN} to {@link #CALAMITOUS_BASE_MANA_REGEN}
     * with a stable transient modifier, so the per-tick regeneration rate is
     * balanced without discarding any curio or equipment regen bonus.
     *
     * <p>Brutality regenerates {@code mana_regen / 20} mana each tick, and every
     * bonus is an additive contribution to the attribute, so lowering the base
     * scales the floor of regeneration while the Rune of Delta's light-level
     * bonus (up to +7) and any other bonuses keep their full effect on top.
     */
    public static void applyManaRegenBase(ServerPlayer player) {
        Attribute brutalityRegen = ForgeRegistries.ATTRIBUTES.getValue(
                ResourceLocation.fromNamespaceAndPath(BRUTALITY_NAMESPACE, BRUTALITY_MANA_REGEN));
        if (brutalityRegen == null) return;

        AttributeInstance instance = player.getAttribute(brutalityRegen);
        if (instance == null) return;

        double delta = CALAMITOUS_BASE_MANA_REGEN - BRUTALITY_BASE_MANA_REGEN;

        AttributeModifier existing = instance.getModifier(MANA_REGEN_BASE_UUID);
        if (existing == null) {
            instance.addTransientModifier(new AttributeModifier(
                    MANA_REGEN_BASE_UUID, MANA_REGEN_BASE_NAME, delta, AttributeModifier.Operation.ADDITION));
        } else if (existing.getAmount() != delta) {
            instance.removeModifier(MANA_REGEN_BASE_UUID);
            instance.addTransientModifier(new AttributeModifier(
                    MANA_REGEN_BASE_UUID, MANA_REGEN_BASE_NAME, delta, AttributeModifier.Operation.ADDITION));
        }
    }

    public static void clearFraction(UUID playerId) {
        FRACTIONS.remove(playerId);
    }
}
