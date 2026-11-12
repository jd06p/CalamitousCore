package dev.karma.calamitouscore.compat.summoning;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact 0.4.1 summon roles, verified against both the old bridge and installed registry. */
public final class SummoningRoles {
    public record Role(String className, int cost) {}
    public static final Map<String, Role> ENTITIES;
    public static final Set<String> STAFFS;
    public static final List<String> TARGETS;
    static {
        try (var in = SummoningRoles.class.getResourceAsStream("/calamitouscore/summoning/roles.json")) {
            if (in == null) throw new IOException("Missing summon role manifest");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Role> roles = new LinkedHashMap<>();
            root.getAsJsonObject("entities").entrySet().forEach(entry -> {
                JsonObject value = entry.getValue().getAsJsonObject();
                roles.put(entry.getKey(), new Role(value.get("class").getAsString(), value.get("cost").getAsInt()));
            });
            ENTITIES = Map.copyOf(roles);
            Set<String> staffs = new HashSet<>(); root.getAsJsonArray("staffs").forEach(value -> staffs.add(value.getAsString())); STAFFS = Set.copyOf(staffs);
            List<String> targets = new ArrayList<>(); root.getAsJsonArray("targets").forEach(value -> targets.add(value.getAsString())); TARGETS = List.copyOf(targets);
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    private SummoningRoles() {}
}
