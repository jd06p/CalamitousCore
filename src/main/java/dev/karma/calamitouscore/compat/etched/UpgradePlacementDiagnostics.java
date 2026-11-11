package dev.karma.calamitouscore.compat.etched;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Observation only. Never changes a click, item, slot, validation result or upgrade restriction. */
public final class UpgradePlacementDiagnostics {
    private static final Logger LOGGER=LogUtils.getLogger();
    private static StorageContainerMenuBase<?> pending;
    private static String attempted;
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        try { observe(event); }
        catch(RuntimeException error) { LOGGER.warn("[CalamitousCore Jukebox placement] Could not inspect click",error); }
    }
    private static void observe(ScreenEvent.MouseButtonPressed.Pre event) {
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !(screen.getMenu() instanceof StorageContainerMenuBase<?> menu)) return;
        var stack=menu.getCarried();
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(id==null || !(id.getNamespace().equals("sophisticatedbackpacks") || id.getNamespace().equals("sophisticatedstorage"))
                || !(id.getPath().equals("jukebox_upgrade") || id.getPath().equals("advanced_jukebox_upgrade"))) return;
        var slot=screen.getSlotUnderMouse();
        LOGGER.info("[CalamitousCore Jukebox placement] item={}, class={}, count={}, canceled={}, hoveredSlot={}, active={}, occupied={}",
                id,stack.getItem().getClass().getName(),stack.getCount(),event.isCanceled(),slot==null?"none":slot.getClass().getName(),
                slot!=null&&slot.isActive(),slot!=null&&!slot.getItem().isEmpty());
        var handler=menu.getStorageWrapper().getUpgradeHandler();
        if(stack.getItem() instanceof IUpgradeItem<?> upgrade) {
            int groupCount=0;
            for(int index=0;index<handler.getSlots();index++)
                if(handler.getStackInSlot(index).getItem() instanceof IUpgradeItem<?> other
                        && other.getUpgradeGroup().name().equals(upgrade.getUpgradeGroup().name())) groupCount++;
            String storage=menu.getStorageWrapper().getStorageType();
            LOGGER.info("[CalamitousCore Jukebox placement] nativeItemValid={}, group={}, existingInGroup={}, itemLimit={}, groupLimit={}, upgradeSlots={}",
                    handler.isItemValid(0,stack),upgrade.getUpgradeGroup().name(),groupCount,upgrade.getUpgradesPerStorage(storage),
                    upgrade.getUpgradesInGroupPerStorage(storage),handler.getSlots());
        }
        pending=menu;attempted=id.toString();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || pending==null) return;
        var menu=pending;pending=null;
        if(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu()==menu)
            LOGGER.info("[CalamitousCore Jukebox placement] after click item={}, carriedCount={}, nativeError={}",attempted,menu.getCarried().getCount(),
                    menu.getErrorUpgradeSlotChangeResult().flatMap(result->result.getErrorMessage()).map(message->message.getString()).orElse("none"));
    }
}
