package org.nott.listener;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.nott.SimpleReward;
import org.nott.utils.SwUtil;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nott
 * @date 2024-10-17
 */
public class DailyRewardListener implements Listener {

    static ConcurrentHashMap<Player, BukkitTask> playerBukkitTaskMap = new ConcurrentHashMap<>(16);

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
        String logRewardTip = SimpleReward.MESSAGE.getString("log_reward_tip");
        Player player = event.getPlayer();
        Audience audience = SimpleReward.adventure.player(player);
        audience.sendMessage(MiniMessage.miniMessage().deserialize(logRewardTip));
        addRewardCount(player);
    }

    @NotNull
    private static BukkitTask addRewardCount(Player player) {
        BukkitTask task = SwUtil.runTaskLater(() -> {
            int count = SimpleReward.SAVE.getInt(player.getName(), 0);
            int chooseTime = SimpleReward.CONFIG.getInt("reward.choose", 3);
            ConfigurationSection section = SimpleReward.SAVE.createSection(player.getName());
            section.set("", count + chooseTime);
            SwUtil.sendMessage(player, SimpleReward.MESSAGE.getString("new_reward_tip"), ChatColor.AQUA);
            addRewardCount(player);
        }, SimpleReward.CONFIG.getInt("reward.interval", 3600) * 20L);
        playerBukkitTaskMap.put(player, task);
        return task;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (SwUtil.isNotNull(playerBukkitTaskMap.get(player))) {
            BukkitTask task = playerBukkitTaskMap.get(player);
            task.cancel();
            playerBukkitTaskMap.remove(player);
        }
    }


}
