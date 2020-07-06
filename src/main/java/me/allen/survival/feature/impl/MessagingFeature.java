package me.allen.survival.feature.impl;

import me.allen.survival.Survival;
import me.allen.survival.feature.Feature;
import me.allen.survival.util.command.Command;
import me.allen.survival.util.command.param.Parameter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessagingFeature extends Feature {
    public MessagingFeature(Survival survival) {
        super(survival);
    }

    @Command(
            names = {
                    "tell",
                    "t",
                    "msg",
                    "messaging",
                    "message"
            }
    )
    public static void tellCommand(Player sender, @Parameter(name = "target") Player target, @Parameter(name = "content", wildcard = true) String content) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You cannot message yourself.");
            return;
        }

        sender.sendMessage(ChatColor.WHITE + "To " + ChatColor.GRAY + target.getName() + ": " + ChatColor.WHITE + content);
        target.sendMessage(ChatColor.WHITE + "From " + ChatColor.GRAY + sender.getName() + ": " + ChatColor.WHITE + content);
    }
}
