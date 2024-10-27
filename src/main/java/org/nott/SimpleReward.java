package org.nott;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.dbutils.DbUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.nott.executor.InviteExecutor;
import org.nott.global.GlobalFactory;
import org.nott.gui.RewardGui;
import org.nott.listener.LoginListener;
import org.nott.manager.SqlLiteManager;
import org.nott.utils.SwUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@SuppressWarnings("all")
public final class SimpleReward extends JavaPlugin {

    public static YamlConfiguration CONFIG;
    public static YamlConfiguration MESSAGE;
    public static YamlConfiguration SAVE;
    public static BukkitScheduler SCHEDULER;
    public static BukkitAudiences adventure;
    private static RewardGui guiProvider;
    public static Economy ECONOMY;

    public static RewardGui getGuiProvider() {
        return guiProvider;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveDefaultConfig();
        this.initConfigYml();
        this.initDb();
        this.registerComponent();

        SCHEDULER = this.getServer().getScheduler();
        adventure = BukkitAudiences.create(this);
        this.removeExpiredInviteInfoAtFixedRate();
        this.setupEconomy();
    }

    private void removeExpiredInviteInfoAtFixedRate() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String dateStr = CONFIG.getString("reward.remove_expired_invite_time","12:00");
                Date date = null;
                Date nowDate = null;
                try {
                    date = GlobalFactory.Formatter.HHMM.parse(dateStr);
                    nowDate = GlobalFactory.Formatter.HHMM.parse(GlobalFactory.Formatter.HHMM.format(new Date()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (date.compareTo(nowDate) <= 0) {
                    return;
                }
                Connection con = null;
                PreparedStatement ps = null;
                int effect = 0;
                try {
                    long time = new Timestamp(System.currentTimeMillis()).getTime();
                    con = SqlLiteManager.getConnect();
                    ps = con.prepareStatement("delete from invite_data where verfiy_time <= ?");
                    ps.setLong(1,time);
                    effect = ps.executeUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(con);
                    DbUtils.closeQuietly(ps);
                }
                SwUtil.log(String.format("清除了条%s过期邀请码", effect));
            }
        },5000L, 10000L);
    }

    @SuppressWarnings(value = "all")
    private void registerComponent() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new LoginListener(this),this);
        this.getCommand("invite").setExecutor(new InviteExecutor(this));
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        ECONOMY = rsp.getProvider();
    }

    private void initDb() {
        SqlLiteManager.checkDbFileIsExist(this);
        SqlLiteManager.createTableIfNotExist(GlobalFactory.INVITE_TABLE,GlobalFactory.CREATE_INVITE_TABLE);
        SqlLiteManager.createTableIfNotExist(GlobalFactory.LOG_TABLE,GlobalFactory.CREATE_LOG_TABLE);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SwUtil.log(GlobalFactory.MESSAGE_YML + " disabled");
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    public void initConfigYml() {
        String msgPath = this.getDataFolder() + File.separator + GlobalFactory.MESSAGE_YML;
        String configPath = this.getDataFolder() + File.separator + GlobalFactory.CONFIG_YML;
        String savePath = this.getDataFolder() + File.separator + GlobalFactory.SAVE_YML;
        MESSAGE = loadFile(msgPath,GlobalFactory.MESSAGE_YML);
        CONFIG = loadFile(configPath,GlobalFactory.CONFIG_YML);
        SAVE = loadFile(savePath,GlobalFactory.SAVE_YML);
    }

    private YamlConfiguration loadFile(String path,String def){
        YamlConfiguration fileYaml = new YamlConfiguration();
        File file = new File(path);
        if (!file.exists()) {
            this.saveResource(def, false);
            try {
                fileYaml.load(Objects.requireNonNull(this.getTextResource(GlobalFactory.MESSAGE_YML)));
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                fileYaml.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return fileYaml;
    }
}
