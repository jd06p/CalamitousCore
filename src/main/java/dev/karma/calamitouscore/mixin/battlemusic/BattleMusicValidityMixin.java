package dev.karma.calamitouscore.mixin.battlemusic;

import dev.karma.calamitouscore.compat.battlemusic.BattleMusicLifecycle;
import net.minecraft.world.entity.Mob;
import java.util.Iterator;
import com.nekotune.battlemusic.BattleMusicInstance;
import dev.karma.calamitouscore.compat.battlemusic.BattleMusicGain;
import net.minecraft.client.Options;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.nekotune.battlemusic.BattleMusic", remap = false)
public abstract class BattleMusicValidityMixin {
    @Unique private static final ResourceLocation calamitouscore$golem = new ResourceLocation("fbt", "battle_tower_golem");
    @Unique private static final double calamitouscore$golemMusicRange = 64.0;

    @Redirect(method = "validEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;m_26883_(D)Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"))
    private static TargetingConditions calamitouscore$musicRange(TargetingConditions conditions, double range, Mob mob, boolean starting) {
        if (calamitouscore$golem.equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))) {
            range = Math.max(range, calamitouscore$golemMusicRange);
        }
        return conditions.range(range);
    }

    @Redirect(method = "updateEntitySoundData", at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private static Object calamitouscore$repairPackEntry(Iterator<?> entries) {
        Object entry = entries.next();
        return "thedulling:war:summit;6".equals(entry) ? "thedulling:war;battlemusic:summit;6" : entry;
    }

    @Redirect(method = "setVolume", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;m_92147_(Lnet/minecraft/sounds/SoundSource;)F"))
    private static float calamitouscore$sliderHandledAtPlayback(Options options, SoundSource category) {
        return 1.0F;
    }

    @Redirect(method = "setVolume", at = @At(value = "INVOKE", target = "Lcom/nekotune/battlemusic/BattleMusicInstance;m_7769_()F"), require = 2)
    private static float calamitouscore$readFadeGain(BattleMusicInstance sound) {
        return ((BattleMusicGain) sound).calamitouscore$unscaledGain();
    }

    @Inject(method = "validEntity", at = @At("HEAD"), cancellable = true)
    private static void calamitouscore$rejectStaleTarget(Mob mob, boolean toStart, CallbackInfoReturnable<Boolean> ci) {
        if (BattleMusicLifecycle.invalidTarget(mob)) ci.setReturnValue(false);
    }
}


