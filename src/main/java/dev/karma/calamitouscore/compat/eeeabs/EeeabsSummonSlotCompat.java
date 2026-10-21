package dev.karma.calamitouscore.compat.eeeabs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.UUID;

/**
 * Bridge between EEEABs Mobs and the active Paisley&nbsp;x&nbsp;Summonity integration.
 *
 * <p>In the integrated pack the authoritative summoning capacity is Summonity's
 * {@code summonity:minion_slots} attribute ({@code RangedAttribute}, base 0.0),
 * which gates both the summon count and the HUD. Paisley's own
 * {@code terramity_paisleys_summoning:summon_slots} attribute is stripped by the
 * compatibility layer, so bonuses written to it have no effect.
 *
 * <p>While {@code eeeabsmobs:soul_summon_necklace} is equipped in a Curios slot this
 * class keeps a single, uniquely-identified +1.0 ADDITION modifier on Summonity's
 * {@code minion_slots} attribute, and removes it again once the necklace is
 * unequipped. The modifier is reconciled every server tick (and on login/respawn/
 * dimension change), so no item or capability implementation in either dependency is
 * touched and the bonus never stacks.
 *
 * <p>The attribute is resolved by registry id rather than a compile dependency, so the
 * class is a no-op in packs where Summonity is absent.
 */
public final class EeeabsSummonSlotCompat {

    private EeeabsSummonSlotCompat() {
    }

    public static final ResourceLocation SUMMON_SLOTS_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("summonity", "minion_slots");

    public static final ResourceLocation SOUL_SUMMON_NECKLACE =
            ResourceLocation.fromNamespaceAndPath("eeeabsmobs", "soul_summon_necklace");

    private static final UUID MODIFIER_UUID =
            UUID.fromString("c0de0001-7a41-4e5b-8f2d-0a1b2c3d4e5f");

    private static final String MODIFIER_NAME = "calamitouscore.eeeabs_summon_necklace";

    private static final double BONUS_SLOTS = 1.0;

    private static Attribute summonSlotsAttribute() {
        return ForgeRegistries.ATTRIBUTES.getValue(SUMMON_SLOTS_ATTRIBUTE);
    }

    private static Item soulSummonNecklace() {
        return ForgeRegistries.ITEMS.getValue(SOUL_SUMMON_NECKLACE);
    }

    private static boolean necklaceEquipped(ServerPlayer player, Item necklace) {
        LazyOptional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        return optional.map(handler -> handler.isEquipped(necklace)).orElse(false);
    }

    public static void reconcile(ServerPlayer player) {
        Attribute attribute = summonSlotsAttribute();
        Item necklace = soulSummonNecklace();
        if (attribute == null || necklace == null) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        boolean shouldHave = necklaceEquipped(player, necklace);
        boolean has = instance.getModifier(MODIFIER_UUID) != null;

        if (shouldHave && !has) {
            instance.addPermanentModifier(new AttributeModifier(
                    MODIFIER_UUID, MODIFIER_NAME, BONUS_SLOTS, AttributeModifier.Operation.ADDITION));
        } else if (!shouldHave && has) {
            instance.removeModifier(MODIFIER_UUID);
        }
    }

    @Mod.EventBusSubscriber(modid = "calamitouscore", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {

        private Events() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.player instanceof ServerPlayer serverPlayer) {
                reconcile(serverPlayer);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                reconcile(serverPlayer);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                reconcile(serverPlayer);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                reconcile(serverPlayer);
            }
        }
    }
}
