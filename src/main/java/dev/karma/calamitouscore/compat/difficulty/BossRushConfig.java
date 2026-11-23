package dev.karma.calamitouscore.compat.difficulty;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public final class BossRushConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> IDS;
    private static List<? extends String> previous;
    private static List<EntityType<?>> resolved = List.of();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("boss_spawn");
        IDS = builder.comment("Boss Rush only. Use summonable mob registry IDs. An empty list skips encounters.")
            .defineListAllowEmpty("enabled_bosses", List.of("minecraft:warden"), value -> value instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    // Called on the server thread. Cache only static registry types, never worlds or players.
    public static List<EntityType<?>> pool() {
        var configured = IDS.get();
        if (!configured.equals(previous)) {
            resolved = resolve(configured, id -> {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) return null;
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                return type != null && type.canSummon() ? type : null;
            });
            previous = List.copyOf(configured);
            if (resolved.isEmpty()) LogUtils.getLogger().warn("Boss Rush pool is empty. Scheduled encounters will be skipped.");
        }
        return resolved;
    }

    static <T> List<T> resolve(List<? extends String> entries, java.util.function.Function<ResourceLocation,T> registry) {
        var types = new LinkedHashSet<T>();
        for (String name : entries) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            T type = id != null && !name.startsWith(":") && name.contains(":") ? registry.apply(id) : null;
            if (type == null) LogUtils.getLogger().warn("Boss Rush ignoring invalid, unavailable or unsummonable entity '{}'", name);
            else types.add(type);
        }
        return List.copyOf(types);
    }

    static void reject(EntityType<?> invalid) {
        resolved = resolved.stream().filter(type -> type != invalid).toList();
        LogUtils.getLogger().warn("Boss Rush ignoring non-mob or uncreatable entity '{}'", ForgeRegistries.ENTITY_TYPES.getKey(invalid));
    }
}
