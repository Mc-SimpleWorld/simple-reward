package org.nott.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nott.SimpleReward;
import org.nott.utils.SwUtil;

/**
 * @author Nott
 * @date 2024-10-17
 */
@Data
@AllArgsConstructor
public class DailyRewardExecutor implements CommandExecutor {

    private SimpleReward plugin;

    @Override
    @SuppressWarnings("all")
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if(args.length == 0){
            throw new CommandException("未知命令");
        }
        String arg = args[0];

        switch (arg){
            default -> {
                throw new CommandException("未知命令");
            }
            case "reload" -> parseReloadCommand(commandSender);
            case "open" -> {
                Player player = (Player) commandSender;
                SimpleReward.SCHEDULER.runTask(getPlugin(), () -> SimpleReward.getGuiProvider().getMainMenu(player).show(player));
            }
        }

        return true;
    }

    private void parseReloadCommand(CommandSender commandSender) {
        if (commandSender.isOp()) {
            if("console".equals(commandSender.getName())){
                SwUtil.log(SimpleReward.MESSAGE.getString("reloaded"));
            }else {
                Player player = (Player) commandSender;
                getPlugin().initConfigYml();
                SwUtil.sendSuccessMsg(player,SimpleReward.MESSAGE.getString("reloaded"));
            }
        } else {
            Player player = (Player) commandSender;
            SwUtil.sendErrorMsg(player,SimpleReward.MESSAGE.getString("not_per"));
        }
    }
}
