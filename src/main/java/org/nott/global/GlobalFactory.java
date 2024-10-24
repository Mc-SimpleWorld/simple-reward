package org.nott.global;

import java.text.SimpleDateFormat;


/**
 * @author Nott
 * @date 2024-10-17
 */
public class GlobalFactory {

    public static final String MESSAGE_YML = "message.yml";
    public static final String CONFIG_YML = "config.yml";
    public static final String SAVE_YML = "save.yml";
    public static final String PLUGIN_NAME = "simple-reward";
    public static final String INVITE_TABLE = "invite_data";
    public static final String LOG_TABLE = "log_info";
    public static final String CREATE_INVITE_TABLE = "CREATE TABLE \"invite_data\" (\"id\" text NOT NULL,\"code\" TEXT,\"verfiy_time\" DATE,\"is_use\" integer,\"invite_person\" TEXT,PRIMARY KEY (\"id\"))";
    public static final String CREATE_LOG_TABLE = "CREATE TABLE \"log_info\" (\"uuid\" text NOT NULL,\"user_name\" TEXT,\"last_log\" integer,PRIMARY KEY (\"uuid\"))";
    public static final String ERROR_COLOR_HEX = "#EE4B2B";
    public static final String SUCCESS_COLOR_HEX = "#228B22";

    public interface Formatter{
        SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

}
