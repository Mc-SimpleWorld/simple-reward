package org.nott.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.dbutils.DbUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nott.SimpleReward;
import org.nott.manager.SqlLiteManager;
import org.nott.model.InviteTemp;
import org.nott.utils.SwUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Nott
 * @date 2024-10-24
 */
@Data
@AllArgsConstructor
public class InviteExecutor implements TabExecutor {

    private Plugin plugin;

    private static ConcurrentHashMap<String, InviteTemp> playerConfirmMap = new ConcurrentHashMap<>(16);

    private static ConcurrentHashMap<String, Timestamp> playerInviteMap = new ConcurrentHashMap<>(16);

    private static Vector<String> beInvitingList = new Vector<>();

    @SuppressWarnings("all")
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 0){
            throw new CommandException("未知命令");
        }
        String arg = args[0];
        switch (arg){
            default -> parseInviteCommand(commandSender,args);
            case "confirm" -> parseConfirmCommand(commandSender);
            case "cancel" -> parseCancelCommand(commandSender);
        }

        return true;
    }

    private void parseCancelCommand(@NotNull CommandSender commandSender) {
        String senderName = commandSender.getName();
        Player sender = (Player) commandSender;
        if (!playerConfirmMap.containsKey(senderName)) {
            SwUtil.sendErrorMsg(sender, "not_confirming");
            return;
        }
        InviteTemp temp = playerConfirmMap.get(senderName);
        String inviteName = temp.getInviteName();
        Player playerExact = getPlugin().getServer().getPlayerExact(inviteName);
        if(SwUtil.isNotNull(playerExact)){
            playerInviteMap.remove(inviteName);
            SwUtil.sendErrorMsg(sender, "be_cancel");
        }
        playerConfirmMap.remove(senderName);
        SwUtil.sendSuccessMsg(sender, "cancel_success");
    }

    private void parseConfirmCommand(@NotNull CommandSender commandSender) {
        String senderName = commandSender.getName();
        Player sender = (Player) commandSender;
        if (!playerConfirmMap.containsKey(senderName)) {
            SwUtil.sendErrorMsg(sender,"not_confirming");
            return;
        }
        InviteTemp inviteTemp = playerConfirmMap.get(senderName);
        String id = inviteTemp.getBeInvitedName();
        String inviteName = inviteTemp.getInviteName();
        Plugin plugins = getPlugin();
        SimpleReward.SCHEDULER.runTaskAsynchronously(plugins, () -> {
            Connection connect = null;
            PreparedStatement ps = null;
            try {
                Player playerExact = plugins.getServer().getPlayerExact(id);
                if(SwUtil.isNull(playerExact)){
                    SwUtil.sendErrorMsg(sender,"player_not_found");
                    return;
                }
                connect = SqlLiteManager.getConnect();
                ps = connect.prepareStatement("update invite_data set is_use = ?,invite_person = ? where id = ?");
                ps.setInt(1, 1);
                ps.setString(2, inviteName);
                ps.setString(3, id);
                int executed = ps.executeUpdate();
                if (executed == 0) {
                    SwUtil.sendErrorMsg(sender,"code_already_use");
                    return;
                }
                // deposit
                int inviteRewardCount = SimpleReward.CONFIG.getInt("reward.invite");
                EconomyResponse depositPlayer4BeInvited = SimpleReward.ECONOMY.depositPlayer(sender, inviteRewardCount);
                EconomyResponse depositPlayer4Invite = SimpleReward.ECONOMY.depositPlayer(plugin.getServer().getPlayerExact(inviteName), inviteRewardCount);
                SwUtil.sendErrorMsg(sender, "invite_success", inviteRewardCount + "");
                SwUtil.sendErrorMsg(playerExact, "invite_success", inviteRewardCount + "");
                playerInviteMap.remove(inviteName);
                playerConfirmMap.remove(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(connect);
                DbUtils.closeQuietly(ps);
            }
        });
    }

    private void parseInviteCommand(@NotNull CommandSender commandSender, @NotNull String @NotNull [] args) {
        String code = args[0];
        String senderName = commandSender.getName();
        Player sender = (Player) commandSender;
        Plugin plugins = getPlugin();
        if (playerInviteMap.containsKey(senderName)) {
            Timestamp timestamp = playerInviteMap.get(senderName);
            int diff = new Timestamp(System.currentTimeMillis()).compareTo(timestamp);
            if(diff < 0){
                playerInviteMap.remove(senderName);
            }
            SwUtil.sendErrorMsg(sender,"inviting_player",(diff / 1000) + "");
            SwUtil.runTaskLater(() -> {
                if (SwUtil.isNotNull(playerInviteMap.get(sender))) {
                    playerConfirmMap.remove(sender);
                }
            }, 120 * 20);
            return;
        }
        SimpleReward.SCHEDULER.runTaskAsynchronously(plugins, () -> {
            Connection connect = null;
            PreparedStatement ps = null;
            long time = new Timestamp(System.currentTimeMillis()).getTime();
            try {
                connect = SqlLiteManager.getConnect();
                ps = connect.prepareStatement("select * from invite_data where id = ?");
                ps.setString(1,senderName);
                ResultSet rs = ps.executeQuery();
                if(rs.next()){
                    SwUtil.sendErrorMsg(sender,"not_new_invite");
                    return;
                }
                ps = connect.prepareStatement("select * from invite_data where code = ? and verfiy_time >= ? and id != ?");
                ps.setString(1, code);
                ps.setLong(2, time);
                ps.setString(3,senderName);
                rs = ps.executeQuery();
                String id = null;
                while (rs.next()) {
                    id = rs.getString("id");
                }
                if (SwUtil.isNull(id)) {
                    SwUtil.sendErrorMsg(sender,"invalid_code");
                    return;
                }
                Player playerExact = plugins.getServer().getPlayerExact(id);
                if (SwUtil.isNull(playerExact)) {
                    SwUtil.sendErrorMsg(sender,"player_not_found", id + "");
                    return;
                }
                if(playerConfirmMap.containsKey(id)){
                    SwUtil.sendErrorMsg(sender,"already_confirming", id + "");
                    return;
                }
                InviteTemp inviteTemp = new InviteTemp();
                inviteTemp.setBeInvitedName(id);
                inviteTemp.setInviteName(senderName);
                inviteTemp.setCode(code);
                playerConfirmMap.put(playerExact.getName(), inviteTemp);
                SwUtil.sendSuccessMsg(playerExact,"invite_confirm", commandSender.getName());
                SwUtil.sendSuccessMsg(sender,"wait_confirm");
                BukkitTask bukkitTask = SwUtil.runTaskLater(() -> {
                    if (SwUtil.isNotNull(playerConfirmMap.get(playerExact.getName()))) {
                        playerConfirmMap.remove(playerExact.getName());
                    }
                    playerInviteMap.remove(senderName, "");
                }, 120 * 20);
                playerInviteMap.put(senderName,new Timestamp(System.currentTimeMillis()));

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                DbUtils.closeQuietly(connect);
                DbUtils.closeQuietly(ps);
            }
        });
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1){
            return Arrays.asList("<邀请码>", "confirm", "cancel");
        }
        return null;
    }
}
