package dev.karma.calamitouscore.compat.difficulty;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.Reader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "calamitouscore", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SelectorPalette {
    private static final ResourceLocation RESOURCE = new ResourceLocation("calamitouscore", "difficulty/selector_palette.json");
    private static volatile Int2IntOpenHashMap colors = new Int2IntOpenHashMap();

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> {
            try (Reader reader = resources.getResourceOrThrow(RESOURCE).openAsReader()) {
                var next = new Int2IntOpenHashMap();
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry ->
                    next.put(rgb(entry.getKey()), rgb(entry.getValue().getAsString())));
                colors = next;
            } catch (Exception invalid) {
                LogUtils.getLogger().warn("Unable to load difficulty selector palette", invalid);
            }
        });
    }

    private static int rgb(String value) {
        if (!value.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException(value);
        return Integer.parseInt(value, 16);
    }

    public static int selector(int color) {
        return colors.getOrDefault(color, color);
    }

    public static int shared(int color) {
        var screen = Minecraft.getInstance().screen;
        return screen != null && "com.misanthropy.linggango.difficulty_enhancement.client.ui.ModifierScreen".equals(screen.getClass().getName())
            ? selector(color) : color;
    }
}
