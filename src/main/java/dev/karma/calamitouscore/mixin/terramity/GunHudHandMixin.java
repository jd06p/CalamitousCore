package dev.karma.calamitouscore.mixin.terramity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.mcreator.terramity.procedures.TwoHandedConProcedure", remap = false)
public abstract class GunHudHandMixin {
    @Unique private static final TagKey<Item> calamitouscore$guns = ItemTags.create(new ResourceLocation("forge", "guns"));

    @Redirect(method = "execute", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_21206_()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack calamitouscore$hudHand(LivingEntity shooter) {
        ItemStack main = shooter.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(main.getItem());
        if (id != null && "terramity".equals(id.getNamespace()) && main.is(calamitouscore$guns)) return ItemStack.EMPTY;
        return shooter.getOffhandItem();
    }
}
