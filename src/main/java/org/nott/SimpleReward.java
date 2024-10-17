package org.nott;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.nott.global.GlobalFactory;
import org.nott.gui.RewardGui;
import org.nott.utils.SwUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class SimpleReward extends JavaPlugin {

    public static YamlConfiguration CONFIG;
    public static YamlConfiguration MESSAGE;
    public static BukkitScheduler SCHEDULER;
    public static BukkitAudiences adventure;
    private static RewardGui guiProvider;

    public static Map<Player,Integer> playerRewardCountMap = new ConcurrentHashMap<>();

    public static RewardGui getGuiProvider() {
        return guiProvider;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveDefaultConfig();
        this.initConfigYml();
        SCHEDULER = this.getServer().getScheduler();
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
        saveConfig();
        CONFIG = (YamlConfiguration) this.getConfig();
        YamlConfiguration message = new YamlConfiguration();
        String path = this.getDataFolder() + File.separator + GlobalFactory.MESSAGE_YML;
        File file = new File(path);
        if (!file.exists()) {
            this.saveResource(GlobalFactory.MESSAGE_YML, false);
            try {
                message.load(Objects.requireNonNull(this.getTextResource(GlobalFactory.MESSAGE_YML)));
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                message.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        MESSAGE = message;
    }
}
