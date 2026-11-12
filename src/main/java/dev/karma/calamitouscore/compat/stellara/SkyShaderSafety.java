package dev.karma.calamitouscore.compat.stellara;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Resource packs/custom shader implementations may change vertex positions: fail open. */
@Mod.EventBusSubscriber(modid = "calamitouscore", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SkyShaderSafety {
    private SkyShaderSafety() {}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event) {
        SkyVisibility.colorShaderVerified = matches(event, "position_color", "vsh", "06bbdee533b943002342c17dd598ea804f083efe6a8c90873e211107f886a1a5")
                && matches(event, "position_color", "fsh", "173beea081a2a539cf78d3fc69f3bfaf294fa7887153dcf38868e021070dc26a");
        SkyVisibility.texturedShaderVerified = matches(event, "position_tex_color", "vsh", "f49ef060bb74ae5339219382ffa33baa56aad6603ad6b9e18618181665ed992a")
                && matches(event, "position_tex_color", "fsh", "b13d5eb9a77ccc8edd4e42300edec4da941c234fb10b8f4979e141420ac44f47");
    }
    private static boolean matches(RegisterShadersEvent event, String name, String extension, String expected) {
        try (var in = event.getResourceProvider().open(new ResourceLocation("minecraft", "shaders/core/" + name + "." + extension))) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
            return hash.equals(expected);
        } catch (Exception ignored) { return false; }
    }
}
