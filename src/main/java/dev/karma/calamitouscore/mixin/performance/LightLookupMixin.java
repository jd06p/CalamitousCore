package dev.karma.calamitouscore.mixin.performance;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

/** Runs after Nostalgic Tweaks; the plugin identifies its merged handler by the exact cache field. */
@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 900)
public abstract class LightLookupMixin {}
