package dev.karma.calamitouscore.mixin.performance;

import org.spongepowered.asm.mixin.*;

/** Plugin supplies per-invocation local references; all timer writes and sync calls stay original. */
@Pseudo
@Mixin(targets = "net.mcreator.enlightened_end.procedures.OozeRainChanceProcedure", remap = false)
public abstract class OozeLookupsMixin {}
