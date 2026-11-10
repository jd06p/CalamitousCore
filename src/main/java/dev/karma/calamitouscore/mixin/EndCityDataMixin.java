package dev.karma.calamitouscore.mixin;

import net.mcreator.thedulling.DullingCityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DullingCityData.class, remap = false)
public abstract class EndCityDataMixin {
    @Shadow
    private static DullingCityData load(CompoundTag tag) {
        throw new AssertionError();
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$useVoidCityRecords(ServerLevel level,
            CallbackInfoReturnable<DullingCityData> cir) {
        // Leave the old End city file and blocks intact without consuming the void city's slot.
        if (Level.END.equals(level.dimension())) {
            cir.setReturnValue(level.getDataStorage().computeIfAbsent(EndCityDataMixin::load,
                    DullingCityData::new, "calamitouscore_end_void_cities"));
        }
    }
}
