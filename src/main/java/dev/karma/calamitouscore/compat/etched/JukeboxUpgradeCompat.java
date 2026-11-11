package dev.karma.calamitouscore.compat.etched;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;

/** Keep native backpack limits when the two tag-enabled Storage jukebox items are used in a backpack. */
public final class JukeboxUpgradeCompat {
    public static String counterpart(String storage, String id) {
        if (!"backpack".equals(storage) || id==null) return null;
        return switch (id) {
            case "sophisticatedstorage:jukebox_upgrade" -> "sophisticatedbackpacks:jukebox_upgrade";
            case "sophisticatedstorage:advanced_jukebox_upgrade" -> "sophisticatedbackpacks:advanced_jukebox_upgrade";
            default -> null;
        };
    }
    public static Integer nativeLimit(Item item, String storage, boolean group) {
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(item);
        String target=id==null?null:counterpart(storage,id.toString());
        if(target==null) return null;
        Item nativeItem=ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));
        if(!(nativeItem instanceof IUpgradeItem<?> upgrade)) return null;
        return group?upgrade.getUpgradesInGroupPerStorage(storage):upgrade.getUpgradesPerStorage(storage);
    }
}
