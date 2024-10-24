package org.nott.listener;

import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.nott.SimpleReward;
import org.nott.global.GlobalFactory;
import org.nott.manager.SqlLiteManager;
import org.nott.utils.SwUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
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
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        Plugin plugins = getPlugin();
        String uuid = player.getUniqueId().toString();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleReward.SCHEDULER.runTaskAsynchronously(plugins, () -> {
            Connection con = null;
            PreparedStatement ps = null;
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            String displayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());
            ConfigurationSection section = SimpleReward.CONFIG.getConfigurationSection("reward.week_daily");
            Set<String> keys = section.getKeys(false);
            if (SwUtil.isEmpty(keys)) return;
            try {
                String key = keys.stream().filter(element -> displayName.equals(element)).findAny().orElse(null);
                String realkey = StringUtils.isNotEmpty(key) ? key : "Other";
                String msg = section.getString(realkey + "message");
                String type = section.getString(realkey + "reward.type");
                String val = section.getString(realkey + "reward.value");
                boolean isRandomVal = "Random".equalsIgnoreCase(type);
                con = SqlLiteManager.getConnect();
                ps = con.prepareStatement("select last_log from log_info where uuid = ?");
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    ps = con.prepareStatement("insert into log_info(uuid,user_name,last_log) values ?,?,?");
                    ps.setString(1, uuid);
                    ps.setString(2, name);
                    ps.setLong(3, timestamp.getTime());
                    ps.execute();
                } else {
                    long last_log = 0;
                    while (rs.next()) {
                        last_log = rs.getLong("last_log");
                    }
                    Date date = new Date();
                    Timestamp lastLog = new Timestamp(last_log);
                    String lastLogDayStr = GlobalFactory.Formatter.YYYYMMDD.format(new Timestamp(last_log));
                    String NowDayStr = GlobalFactory.Formatter.YYYYMMDD.format(date);
                    Date date1 = GlobalFactory.Formatter.YYYYMMDD.parse(lastLogDayStr);
                    Date date2 = GlobalFactory.Formatter.YYYYMMDD.parse(NowDayStr);
                    if (date2.after(date1)) {
                        ps = con.prepareStatement("update log_info set last_log = ? where uuid = ?");
                        ps.setLong(1, timestamp.getTime());
                        ps.setString(2, uuid);
                    } else {
                        return;
                    }
                }
                getPlayerOnlineReward(isRandomVal, player, val);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(con);
                DbUtils.closeQuietly(ps);
            }
        });
    }

    private void getPlayerOnlineReward(boolean isRandom, Player player, String val) {
        Integer valInteger;
        if(isRandom){
            String[] split = val.split("-");
            valInteger = RandomUtils.nextInt(Integer.parseInt(split[0]),Integer.parseInt(split[1]));
        }else {
            valInteger = Integer.parseInt(val);
        }
        SimpleReward.ECONOMY.depositPlayer(player, Double.parseDouble(val));
    }

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.NORMAL)
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
                ps = connect.prepareStatement("insert into invite_data(id,code,verfiy_time,is_use) values ?,?,?,?");
                ps.setString(1, name);
                ps.setString(2, code + "");
                ps.setLong(3, verfiyTime.getTime());
                ps.setInt(4, 0);
                if (ps.execute()) {
                    String msg = String.format(SimpleReward.MESSAGE.getString("welcome"), name, code, code, SimpleReward.CONFIG.getInt("reward.invite"));
                    SimpleReward.adventure.player(player).sendMessage(
                            Component.text().content(msg)
                                    .color(TextColor.color(TextColor.fromHexString("#FFEBCD")))
                    );
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
