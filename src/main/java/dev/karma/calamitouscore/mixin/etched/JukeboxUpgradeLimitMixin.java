package dev.karma.calamitouscore.mixin.etched;
import dev.karma.calamitouscore.compat.etched.JukeboxUpgradeCompat;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase",remap=false)
public abstract class JukeboxUpgradeLimitMixin {
    @Inject(method="getUpgradesPerStorage",at=@At("HEAD"),cancellable=true)
    private void calamitouscore$itemLimit(String storage,CallbackInfoReturnable<Integer> cir) {
        Integer limit=JukeboxUpgradeCompat.nativeLimit((Item)(Object)this,storage,false);
        if(limit!=null) cir.setReturnValue(limit);
    }
    @Inject(method="getUpgradesInGroupPerStorage",at=@At("HEAD"),cancellable=true)
    private void calamitouscore$groupLimit(String storage,CallbackInfoReturnable<Integer> cir) {
        Integer limit=JukeboxUpgradeCompat.nativeLimit((Item)(Object)this,storage,true);
        if(limit!=null) cir.setReturnValue(limit);
    }
}
