package de.jeff_media.lumberjack.commands;

import com.jeff_media.jefflib.BlockTracker;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.jeff_media.lumberjack.LumberJack;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public class CommandLumberjack {

    final LumberJack plugin;

    public CommandLumberjack(LumberJack plugin, Commands commands) {
        this.plugin = plugin;

        LiteralCommandNode<CommandSourceStack> reloadCommand = literal("reload")
                .requires(s -> s.getSender().hasPermission("lumberjack.reload"))
                .executes(c -> {
                    plugin.reload();
                    c.getSource().getSender().sendMessage(
                            Component.text("LumberJack has been reloaded.").color(NamedTextColor.GREEN));

                    return Command.SINGLE_SUCCESS;
                }).build();

        LiteralCommandNode<CommandSourceStack> debugCommand = literal("debug")
                .requires(s -> s.getSender().hasPermission("lumberjack.debug"))
                .executes(c -> {
                    CommandSender sender = c.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("You must be a in-game to run this command.");
                        return Command.SINGLE_SUCCESS;
                    }

                    Block target = player.getTargetBlock(null, 20);
                    player.sendMessage(String.valueOf(BlockTracker.isPlayerPlacedBlock(target)));

                    return Command.SINGLE_SUCCESS;
                }).build();

        commands.register(literal("lumberjack").executes(c -> {
            CommandSender sender = c.getSource().getSender();

            if (!(sender instanceof Player player)) {
                sender.sendMessage("You must be a in-game to run this command.");
                return Command.SINGLE_SUCCESS;
            }

            if (!player.hasPermission("lumberjack.use")) {
                player.sendMessage(Bukkit.permissionMessage());
                return Command.SINGLE_SUCCESS;
            }

            if (player.hasPermission("lumberjack.force") && !player.hasPermission("lumberjack.force.ignore")) {
                player.sendMessage(plugin.messages.MSG_CAN_NOT_DISABLE);
                return Command.SINGLE_SUCCESS;
            }

            plugin.togglePlayerSetting(player);

            if (plugin.getPlayerSetting(player).gravityEnabled) {
                sender.sendMessage(plugin.messages.MSG_ACTIVATED);
            } else {
                sender.sendMessage(plugin.messages.MSG_DEACTIVATED);
            }

            return Command.SINGLE_SUCCESS;
        }).then(reloadCommand).then(debugCommand).build(),
                          "Toggle gravity for tree trunks", List.of("tg", "treegravity", "gravity"));
    }
}