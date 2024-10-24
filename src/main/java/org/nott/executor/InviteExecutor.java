package org.nott.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.dbutils.DbUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.nott.SimpleReward;
import org.nott.global.GlobalFactory;
import org.nott.manager.SqlLiteManager;
import org.nott.model.InviteTemp;
import org.nott.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nott
 * @date 2024-10-24
 */
@Data
@AllArgsConstructor
public class InviteExecutor implements CommandExecutor {

    private Plugin plugin;

    private static ConcurrentHashMap<Player, InviteTemp> playerConfirmMap = new ConcurrentHashMap<>(16);

    private static ConcurrentHashMap<Player, String> playerInviteMap = new ConcurrentHashMap<>(16);

    @SuppressWarnings("all")
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && !"confirm".equals(args[0])) {
            String code = args[0];
            String senderName = commandSender.getName();
            Player sender = (Player) commandSender;
            Plugin plugins = getPlugin();
            if (playerInviteMap.contains(sender)) {
                SimpleReward.adventure.sender(commandSender).sendMessage(
                        Component.text(SimpleReward.MESSAGE.getString("inviting_player"))
                                .color(TextColor.color(TextColor.fromHexString(GlobalFactory.ERROR_COLOR_HEX))));
                SwUtil.runTaskLater(() -> {
                    if (SwUtil.isNotNull(playerInviteMap.get(sender))) {
                        playerConfirmMap.remove(sender);
                    }
                }, 120 * 20);
                return true;
            }
            SimpleReward.SCHEDULER.runTaskAsynchronously(plugins, () -> {
                Connection connect = null;
                PreparedStatement ps = null;
                long time = new Timestamp(System.currentTimeMillis()).getTime();
                try {
                    connect = SqlLiteManager.getConnect();
                    ps = connect.prepareStatement("select * from invite_data where code = ? and verfiy_time < ?");
                    ps.setString(1, code);
                    ps.setLong(2, time);
                    ResultSet rs = ps.executeQuery();
                    String id = null;
                    while (rs.next()) {
                        id = rs.getString("id");
                    }
                    if (SwUtil.isNull(id)) {
                        SimpleReward.adventure.sender(commandSender).sendMessage(
                                Component.text(SimpleReward.MESSAGE.getString("invalid_code"))
                                        .color(TextColor.color(TextColor.fromHexString(GlobalFactory.ERROR_COLOR_HEX))));
                        return;
                    }
                    Player playerExact = plugins.getServer().getPlayerExact(id);
                    if (SwUtil.isNull(playerExact)) {
                        String msg = String.format(SimpleReward.MESSAGE.getString("player_not_found"), id);
                        SimpleReward.adventure.sender(commandSender).sendMessage(
                                Component.text(msg)
                                        .color(TextColor.color(TextColor.fromHexString(GlobalFactory.ERROR_COLOR_HEX))));
                    }
                    InviteTemp inviteTemp = new InviteTemp();
                    inviteTemp.setBeInvitedName(id);
                    inviteTemp.setInviteName(senderName);
                    inviteTemp.setCode(code);
                    playerConfirmMap.put(playerExact, inviteTemp);
                    String inviteConfirm = String.format(SimpleReward.MESSAGE.getString("invite_confirm"), commandSender.getName());
                    SimpleReward.adventure.player(playerExact).sendMessage(
                            Component.text(inviteConfirm)
                                    .color(TextColor.color(TextColor.fromHexString(GlobalFactory.SUCCESS_COLOR_HEX))));

                    SwUtil.runTaskLater(() -> {
                        if (SwUtil.isNotNull(playerConfirmMap.get(playerExact))) {
                            playerConfirmMap.remove(playerExact);
                        }
                    }, 120 * 20);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(connect);
                    DbUtils.closeQuietly(ps);
                }
            });
            return true;
        }
        if (args.length == 1 && "confirm".equals(args[0])) {
            String senderName = commandSender.getName();
            Player sender = (Player) commandSender;
            if (!playerConfirmMap.contains(sender)) {
                SimpleReward.adventure.sender(commandSender).sendMessage(
                        Component.text(SimpleReward.MESSAGE.getString("confirm_expired"))
                                .color(TextColor.color(TextColor.fromHexString(GlobalFactory.ERROR_COLOR_HEX))));
                return true;
            }
            InviteTemp inviteTemp = playerConfirmMap.get(sender);
            String id = inviteTemp.getBeInvitedName();
            String inviteName = inviteTemp.getInviteName();
            Plugin plugins = getPlugin();
            SimpleReward.SCHEDULER.runTaskAsynchronously(plugins, () -> {
                Connection connect = null;
                PreparedStatement ps = null;
                try {
                    connect = SqlLiteManager.getConnect();
                    ps = connect.prepareStatement("update invite_data set is_use = ?,invite_person = ? where id = ?");
                    ps.setInt(1, 1);
                    ps.setString(2, inviteName);
                    ps.setString(3, id);
                    int executed = ps.executeUpdate();
                    if (executed == 0) {
                        SimpleReward.adventure.player(sender).sendMessage(
                                Component.text(SimpleReward.MESSAGE.getString("code_already_use"))
                                        .color(TextColor.color(TextColor.fromHexString(GlobalFactory.ERROR_COLOR_HEX))));
                        return;
                    }
                    // deposit
                    int inviteRewardCount = SimpleReward.CONFIG.getInt("reward.invite");
                    EconomyResponse depositPlayer4BeInvited = SimpleReward.ECONOMY.depositPlayer(sender, inviteRewardCount);
                    EconomyResponse depositPlayer4Invite = SimpleReward.ECONOMY.depositPlayer(plugin.getServer().getPlayerExact(inviteName), inviteRewardCount);
                    String msg = String.format(SimpleReward.MESSAGE.getString("invite_success"), inviteRewardCount);
                    SimpleReward.adventure.player(sender).sendMessage(
                            Component.text(msg)
                                    .color(TextColor.color(TextColor.fromHexString(GlobalFactory.SUCCESS_COLOR_HEX))));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(connect);
                    DbUtils.closeQuietly(ps);
                }
            });

        }
        return false;
    }
}
