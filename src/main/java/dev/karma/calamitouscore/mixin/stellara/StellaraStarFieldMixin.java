package dev.karma.calamitouscore.mixin.stellara;

import java.util.*;
import dev.karma.calamitouscore.compat.stellara.SkyStarIterator;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.shatterbyte.stellara.client.sky.content.StarFieldSkyContent", remap = false)
public abstract class StellaraStarFieldMixin {
    @Shadow @Final private List<?> stars;
    @Shadow @Final private List<?> textures;
    @Unique private List<List<Object>> calamitouscore$byTexture;
    @Unique private int calamitouscore$texture;
    @Unique private final SkyStarIterator calamitouscore$iterator = new SkyStarIterator();

    @Inject(method = "render", at = @At("HEAD"))
    private void calamitouscore$begin(CallbackInfo ci) {
        if (calamitouscore$byTexture == null) {
            List<List<Object>> groups = new ArrayList<>(textures.size());
            for (int i = 0; i < textures.size(); i++) groups.add(new ArrayList<>());
            for (Object star : stars) {
                int texture = ((StellaraStarDataAccessor) star).calamitouscore$textureIndex();
                if (texture >= 0 && texture < groups.size()) groups.get(texture).add(star);
            }
            calamitouscore$byTexture = groups;
        }
        calamitouscore$texture = 0;
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<?> calamitouscore$group(List<?> original) {
        return calamitouscore$iterator.reset(calamitouscore$byTexture.get(calamitouscore$texture++));
    }
}

