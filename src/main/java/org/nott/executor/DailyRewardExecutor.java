package org.nott.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
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
        if (args.length == 1 && "reload".equals(args[0])) {
            if (commandSender.isOp()) {
                if("console".equals(commandSender.getName())){
                    SwUtil.log(SimpleReward.MESSAGE.getString("reloaded"));
                }else {
                    Audience audience = SimpleReward.adventure.player((Player) commandSender);
                    getPlugin().initConfigYml();
                    TextComponent component = Component.text(SimpleReward.MESSAGE.getString("reloaded"))
                            .color(NamedTextColor.YELLOW);
                    audience.sendMessage(component);
                }
                return true;
            } else {
                Audience audience = SimpleReward.adventure.player((Player) commandSender);
                TextComponent component = Component.text(SimpleReward.MESSAGE.getString("not_per"))
                        .color(NamedTextColor.DARK_RED);
                audience.sendMessage(component);
                return true;
            }
        }
        if(args.length == 1 && "open".equals(args[0])){
            Player player = (Player) commandSender;
            SimpleReward.SCHEDULER.runTask(getPlugin(), () -> SimpleReward.getGuiProvider().getMainMenu(player).show(player));
        }
        return false;
    }
}
