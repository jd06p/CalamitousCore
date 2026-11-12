package dev.karma.calamitouscore.mixin.summoning;
import com.fevzi.summonity.client.ClientSummonSlotsData;
import dev.karma.calamitouscore.compat.summoning.SummonEquipmentBridge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.fevzi.summonity.client.SummonHudOverlay", remap = false)
public abstract class SharedSummonHudMixin {
    @Inject(method = "isSummonerItem", at = @At("RETURN"), cancellable = true)
    private static void calamitouscore$items(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && SummonEquipmentBridge.hud(stack)) cir.setReturnValue(true);
    }
    @Inject(method = "hasSummonerItemInHand", at = @At("RETURN"), cancellable = true)
    private static void calamitouscore$active(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ClientSummonSlotsData.getUsedSlots() > 0) cir.setReturnValue(true);
    }
}
