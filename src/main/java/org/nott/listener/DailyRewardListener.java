package org.nott.listener;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.nott.SimpleReward;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nott
 * @date 2024-10-17
 */
public class DailyRewardListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event){
        String logRewardTip = SimpleReward.MESSAGE.getString("log_reward_tip");
        Player player = event.getPlayer();
        Audience audience = SimpleReward.adventure.player(player);
        audience.sendMessage(MiniMessage.miniMessage().deserialize(logRewardTip));

    }


}
