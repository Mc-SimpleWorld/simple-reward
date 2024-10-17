package org.nott.gui;

import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nott.SimpleReward;
import org.nott.utils.SwUtil;

import java.util.List;

/**
 * @author Nott
 * @date 2024-10-17
 */
@Data
@AllArgsConstructor
public class RewardGui {

    private SimpleReward plugin;

    public InventoryGui getMainMenu(Player player) {
        String[] guiSetup = {
                "aaaaaaaaa",
                "naaaraaaa",
                "aaaaaaaaa"
        };
        Integer count = SimpleReward.playerRewardCountMap.get(player);
        String title = SimpleReward.MESSAGE.getString("title");
        InventoryGui gui = new InventoryGui(plugin, title, guiSetup);
        gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        // Player Reward Info('name')
        gui.addElement(new StaticGuiElement('n',
                new ItemStack(Material.PLAYER_HEAD),
                1, // Display a number as the item count
                click -> true,
                ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player,SimpleReward.MESSAGE.getString("player_info")),
                ChatColor.DARK_GREEN + String.format(SimpleReward.MESSAGE.getString("have_last",
                        SwUtil.isNull(count) ? "0" : count + ""))
        ));
        // Enter Reward gui(reward)
        if(count > 0){
            gui.addElement(new StaticGuiElement('r', new ItemStack(Material.AIR), click -> {
                SimpleReward.SCHEDULER.runTaskLater(plugin, () -> getRewardMenu((Player) click.getWhoClicked()).show(click.getWhoClicked()), 1);
                return true;
            }, ""));
        }else {
            gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        }
        gui.setCloseAction(close -> false);
        return gui;
    }

    public InventoryGui getRewardMenu(Player player){
        String[] guiSetup = {
                "aaaaaaaaa",
                "aaaaaaaaa",
                "aaaaaacab"
        };
        String title = SimpleReward.MESSAGE.getString("title");
        InventoryGui gui = new InventoryGui(plugin, title, guiSetup);
        List<String> itemList = SimpleReward.CONFIG.getStringList("reward.item");
        if(SwUtil.isEmpty(itemList)){
            SwUtil.log(SimpleReward.MESSAGE.getString("not_have_reward"));
            return null;
        }
        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST);
        itemList.forEach(item ->{
            ItemStack itemStack = new ItemStack(Material.getMaterial(item));
            itemStack.setAmount(1);
            inv.addItem(itemStack);
        });
        return
    }

}
