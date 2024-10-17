package org.nott.listener;

import de.themoep.inventorygui.InventoryGui;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import org.nott.SimpleReward;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nott
 * @date 2024-10-17
 */
@AllArgsConstructor
@Data
public class InventoryListener implements Listener {

    private SimpleReward plugin;

    private final Map<Player, Long> lastGuiClick;
    private final Map<Player, AtomicInteger> tickCounter;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            handleClick(event);
            if (InventoryGui.getOpen(event.getWhoClicked()) == null) {
                event.setCancelled(true);
                BukkitTask task = SimpleReward.SCHEDULER.runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClickLow(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (inventory.getHolder() instanceof InventoryGui.Holder) {
            if (InventoryGui.getOpen(event.getWhoClicked()) == null) {
                event.setCancelled(true);
                SimpleReward.SCHEDULER.runTaskLater(plugin, () -> {
                    InventoryGui.clearHistory(event.getWhoClicked());
                    event.getWhoClicked().closeInventory();
                }, 1);
            }
        }
    }



    private void handleClick(InventoryInteractEvent event) {
        Player player = (Player) event.getWhoClicked();
        long gameTick = tickCounter.get(player).get();
        Long lastClick = lastGuiClick.get(player);
        if (lastClick == null || gameTick != lastClick) {
            lastGuiClick.put(player, gameTick);
        } else {
            event.setCancelled(true);
        }
    }

}
