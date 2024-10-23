package org.nott.global;

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
    public static final String CREATE_INVITE_TABLE = "CREATE TABLE \"invite_data\" (\"id\" text NOT NULL,\"code\" TEXT,\"verfiy_time\" DATE,\"is_use\" integer,\"invite_person\" TEXT,PRIMARY KEY (\"id\"))";


}
