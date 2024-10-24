package org.nott.manager;


import org.apache.commons.dbutils.DbUtils;
import org.bukkit.plugin.Plugin;
import org.nott.SimpleReward;
import org.nott.global.GlobalFactory;
import org.nott.utils.SwUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Nott
 * @date 2024-10-12
 */
public class SqlLiteManager {

    public static final String DB_PATH = "plugins/" + GlobalFactory.PLUGIN_NAME + "/libs/database.db";

    public static Connection getConnect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:plugins/" + GlobalFactory.PLUGIN_NAME + "/libs/database.db");
    }

    public static void checkDbFileIsExist(Plugin plugin) {
        File file = new File(DB_PATH);
        if (file.exists()) {
            return;
        }
        plugin.saveResource("libs/database.db",false);
    }

    @SuppressWarnings("all")
    public static void createTableIfNotExist(String tableName, String sql) {
        Connection connect = null;
        PreparedStatement statement = null;
        try {
            connect = getConnect();
            statement = connect.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name= ?");
            statement.setString(1, tableName);
            ResultSet set = statement.executeQuery();
            boolean hasTable = false;
            while (set.next()){
                if (tableName.equals(set.getString("name"))) {
                    hasTable = true;
                }
            }
            if(!hasTable){
                PreparedStatement prepared = connect.prepareStatement(sql);
                prepared.execute();
            }
        } catch (Exception e) {
            SwUtil.log(String.format(SimpleReward.MESSAGE.getString("create_tab_error"), tableName) + e.getMessage());
        } finally {
            DbUtils.closeQuietly(connect);
            DbUtils.closeQuietly(statement);
        }
    }

}
