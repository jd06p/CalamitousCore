package dev.karma.calamitouscore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.mcreator.thedulling.DullingCityManager$BuildRequest", remap = false)
public interface CityBuildRequestAccessor {
    @Accessor("gridX")
    int calamitouscore$getGridX();

    @Accessor("gridZ")
    int calamitouscore$getGridZ();
}
