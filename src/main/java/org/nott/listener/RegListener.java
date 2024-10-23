package org.nott.listener;

import fr.xephi.authme.events.RegisterEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.RandomUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.nott.SimpleReward;
import org.nott.manager.SqlLiteManager;
import org.nott.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Nott
 * @date 2024-10-23
 */
@AllArgsConstructor
@Data
public class RegListener implements Listener {

    private Plugin plugin;

    @EventHandler
    public void onPlayerRegister(RegisterEvent event){
        Player player = event.getPlayer();
        String name = player.getName();
        int code = RandomUtils.nextInt(10000, 99999);
        Timestamp verfiyTime = new Timestamp(new Date().getTime() + (3600 * 24 * 7) * 1000);
        SimpleReward.SCHEDULER.runTaskAsynchronously(getPlugin(),()->{
            Connection connect = null;
            PreparedStatement ps = null;
            try {
                SqlLiteManager.getConnect();
                //TODO Write Table
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(connect);
                DbUtils.closeQuietly(ps);
            }
        });
        String msg = String.format(SimpleReward.MESSAGE.getString("welcome"), code,code);
        SimpleReward.adventure.player(player).sendMessage(
                Component.text().content(msg)
                        .color(TextColor.color(TextColor.fromHexString("#FFEBCD")))
        );
    }
}
