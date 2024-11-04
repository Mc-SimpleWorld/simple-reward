package org.nott.listener;

import fr.xephi.authme.events.RegisterEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.RandomUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.nott.SimpleReward;
import org.nott.manager.SqlLiteManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author Nott
 * @date 2024-10-23
 */
@AllArgsConstructor
@Data
public class LoginListener implements Listener {

    private Plugin plugin;

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRegister(RegisterEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        int code = RandomUtils.nextInt(10000, 99999);
        Timestamp verfiyTime = new Timestamp(new Date().getTime() + (3600 * 24 * 7) * 1000);
        SimpleReward.SCHEDULER.runTaskAsynchronously(getPlugin(), () -> {
            Connection connect = null;
            PreparedStatement ps = null;
            try {
                // Write Table
                connect = SqlLiteManager.getConnect();
                ps = connect.prepareStatement("insert into invite_data(id,code,verfiy_time,is_use) values (?,?,?,?)");
                ps.setString(1, name);
                ps.setString(2, code + "");
                ps.setLong(3, verfiyTime.getTime());
                ps.setInt(4, 0);
                int effect = ps.executeUpdate();
                if (effect > 0) {
                    String msg = String.format(SimpleReward.MESSAGE.getString("welcome"), name, code, code, SimpleReward.CONFIG.getInt("reward.invite"));
                    for (int i = 0; i < 3; i++) {
                        SimpleReward.adventure.player(player).sendMessage(
                                Component.text().content(msg)
                                        .color(TextColor.color(TextColor.fromHexString("#FFEBCD")))
                        );
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(connect);
                DbUtils.closeQuietly(ps);
            }
        });
    }


}
