package org.nott.gui;

import com.google.common.base.Ascii;
import com.google.common.collect.Lists;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.commons.lang3.StringUtils;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Nott
 * @date 2024-10-17
 */
@Data
@AllArgsConstructor
public class RewardGui {

    private SimpleReward plugin;

    ConcurrentHashMap<Player,Vector<InventoryGui>> playerGUIMap = new ConcurrentHashMap<>(16);

    public InventoryGui getMainMenu(Player player) {
        String[] guiSetup = {
                "aaaaaaaaa",
                "anaaaraaa",
                "aaaaaaaaa"
        };
        Integer count = SimpleReward.playerRewardCountMap.get(player).get();
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
                SimpleReward.SCHEDULER.runTaskLater(plugin, () -> {
                    close(gui);
                    getRewardMenu((Player) click.getWhoClicked()).show(click.getWhoClicked());
                }, 1);
                return true;
            }, SimpleReward.MESSAGE.getString("enter_build_reward") + ChatColor.DARK_GREEN));
        }else {
            gui.addElement(new StaticGuiElement('r', new ItemStack(Material.AIR), ChatColor.LIGHT_PURPLE.toString()));
        }
        gui.setCloseAction(close -> false);
        return gui;
    }

    public InventoryGui getRewardMenu(Player player){
        AtomicInteger atomicInteger = SimpleReward.playerRewardCountMap.get(player);
        if(SwUtil.isNull(atomicInteger) || atomicInteger.get() <= 0){
            SwUtil.sendMessage(player,SimpleReward.MESSAGE.getString("not_reward"),ChatColor.RED);
            closeAll(player);
        }
        List<String> itemList = SimpleReward.CONFIG.getStringList("reward.item");
        String title = SimpleReward.MESSAGE.getString("title");
        String[] guiSetup = null;
        InventoryGui gui = null;
        if(SwUtil.isEmpty(itemList)){
            guiSetup = new String[]{
                    "aaaaaaaaa",
                    "aaaaaaaaa",
                    "aaaaaacab"
            };
            gui = new InventoryGui(plugin, title, guiSetup);
            gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR),ChatColor.LIGHT_PURPLE.toString()));
            gui.addElement(new StaticGuiElement('c', new ItemStack(Material.PLAYER_HEAD),
                    click -> false,
                    ChatColor.LIGHT_PURPLE + SimpleReward.MESSAGE.getString("not_have_reward")));
            SwUtil.log(SimpleReward.MESSAGE.getString("not_have_reward"));
            return gui;
        }else {
            Map<String, Integer> itemMap = new HashMap<>(16);
            List<List<String>> batchItemList = Lists.partition(itemList, 9);
            Integer minAsciiIndex = 33;
            Integer maxAsciiIndex = 126;
            int currentAsciiIndex = 33;
            StringBuffer sb = null;
            List<String> setUpList = new ArrayList<>();
            for (List<String> list : batchItemList) {
                sb = new StringBuffer();
                for (String itemName : list) {
                    char ch = (char) currentAsciiIndex;
                    sb.append(ch);
                    itemMap.put(itemName,currentAsciiIndex);
                    currentAsciiIndex++;
                }
                setUpList.add(sb.toString());
            }
            String lastLine = "aiaadacab";
            setUpList.add(lastLine);
            gui = new InventoryGui(plugin, title, setUpList.toArray(new String[0]));
            gui.addElement(new StaticGuiElement('a', new ItemStack(Material.AIR),ChatColor.LIGHT_PURPLE.toString()));
            for (String item : itemList) {
                Integer i = itemMap.get(item);
                int i1 = i;
                ItemStack itemStack = new ItemStack(Material.getMaterial(item));
                gui.addElement(new StaticGuiElement((char)(i1), itemStack,
                        item + "x" + SimpleReward.CONFIG.getInt("reward.stack"),
                        SimpleReward.MESSAGE.getString("choose")
                ));
            }

            gui.addElement(new DynamicGuiElement('i', (viewer) -> {
                return new StaticGuiElement('i', new ItemStack (Material.PAPER),
                        click -> {
                            click.getGui().draw(); // Update the GUI
                            return true;
                        },
                        String.format(SimpleReward.MESSAGE.getString("have_last"), SimpleReward.playerRewardCountMap.get(player).get()) + ChatColor.DARK_GREEN);
            }));
        }
        // Close GUI
        final InventoryGui finalGUI = gui;
        gui.addElement(new StaticGuiElement('b', new ItemStack(Material.PLAYER_HEAD),
                click -> {
                    this.close(finalGUI);
                    return true;
                },
                ChatColor.GOLD + SimpleReward.MESSAGE.getString("cancel")));

        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST);
        itemList.forEach(item ->{
            ItemStack itemStack = new ItemStack(Material.getMaterial(item));
            itemStack.setAmount(1);
            inv.addItem(itemStack);
        });
        return gui;
    }

    public void close(InventoryGui inventoryGui){
        inventoryGui.close();
        plugin.SCHEDULER.runTaskLater(plugin, () -> {
            inventoryGui.destroy();
        }, 1);
    }

    public void addGui(Player player,InventoryGui gui){
        Vector<InventoryGui> inventoryGuis = playerGUIMap.get(player);
        if(SwUtil.isEmpty(inventoryGuis)){
            Vector<InventoryGui> guiVector = new Vector<>();
            guiVector.add(gui);
            playerGUIMap.put(player,guiVector);
        }else {
            inventoryGuis.add(gui);
        }
    }

    public void closeAll(Player player){
        Vector<InventoryGui> inventoryGuis = playerGUIMap.get(player);
        inventoryGuis.forEach(gui -> close(gui));
    }
}
