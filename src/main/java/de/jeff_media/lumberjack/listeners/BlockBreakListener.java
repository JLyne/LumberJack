package de.jeff_media.lumberjack.listeners;

import com.destroystokyo.paper.MaterialSetTag;
import de.jeff_media.lumberjack.LumberJack;
import de.jeff_media.lumberjack.NBTKeys;
import de.jeff_media.lumberjack.data.AxeMaterial;
import de.jeff_media.lumberjack.utils.TreeUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class BlockBreakListener implements Listener {

    final LumberJack plugin;
    final NamespacedKey fallingLogKey;

    public BlockBreakListener(LumberJack plugin) {
        this.plugin = plugin;
        this.fallingLogKey = new NamespacedKey(plugin, NBTKeys.IS_FALLING_LOG);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        // checking in lower case for lazy admins
        if (plugin.disabledWorlds.contains(event.getBlock().getWorld().getName().toLowerCase())) {
            return;
        }

        if (!TreeUtils.isPartOfTree(event.getBlock())) {
            return;
        }

        if (!TreeUtils.isOnTreeGround(event.getBlock())) {
            return;
        }

        if(!event.getPlayer().hasPermission("lumberjack.use")) {
            return;
        }

        // Dont show message when gravity is forced
        if ((!event.getPlayer().hasPermission("lumberjack.force") || event.getPlayer().hasPermission("lumberjack.force.ignore"))
                && event.getPlayer().hasPermission("lumberjack.use")) {
            Player p = event.getPlayer();
            if (!plugin.getPlayerSetting(p).gravityEnabled) {
                if (!plugin.getPlayerSetting(p).hasSeenMessage) {
                    plugin.getPlayerSetting(p).hasSeenMessage = true;
                    if (plugin.getConfig().getBoolean("show-message-when-breaking-log")) {
                        p.sendRichMessage(plugin.messages.MSG_COMMANDMESSAGE);
                    }
                }
                return;
            } else {
                if (!plugin.getPlayerSetting(p).hasSeenMessage) {
                    plugin.getPlayerSetting(p).hasSeenMessage = true;
                    if (plugin.getConfig().getBoolean("show-message-when-breaking-log-and-gravity-is-enabled")) {
                        p.sendRichMessage(plugin.messages.MSG_COMMANDMESSAGE2);
                    }
                }
            }
        }

        // check if axe has to be used
        if (plugin.getConfig().getBoolean("must-use-axe")) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

            if (!MaterialSetTag.ITEMS_AXES.isTagged(item.getType())) {
                return;
            }

            // Check axe level
            AxeMaterial requiredAxe = AxeMaterial.get(plugin.getConfig().getString("requires-at-least"));
            if (!AxeMaterial.isAtLeast(item.getType(), requiredAxe)) {
                return;
            }

            // Check enchantment
            if (plugin.requiredEnchantment != null) {
                if (!item.getEnchantments().containsKey(plugin.requiredEnchantment)) {
                    return;
                }
            }
        }

        // check if player must sneak
        if (plugin.getConfig().getBoolean("must-sneak")) {
            if (!event.getPlayer().isSneaking()) {
                return;
            }
        }

        if(!MaterialSetTag.MANGROVE_LOGS.isTagged(event.getBlock().getType())) {
            // fix for torch bug part 2
            if (plugin.getConfig().getBoolean("prevent-torch-exploit") && !TreeUtils.isAboveNonSolidBlock(event.getBlock())) {
                return;
            }
        }

        if (!plugin.getPlayerSetting(event.getPlayer()).gravityEnabled
                && event.getPlayer().hasPermission("lumberjack.force.ignore")) {
            return;
        }

        if (!plugin.getPlayerSetting(event.getPlayer()).gravityEnabled
                && !event.getPlayer().hasPermission("lumberjack.force")) {
            return;
        }

        ArrayList<Block> logs;

        // Atached logs fall down
        if (plugin.getConfig().getBoolean("attached-logs-fall-down")) {

            logs = new ArrayList<>();
            TreeUtils.getTreeTrunk2(event.getBlock().getRelative(BlockFace.UP), logs, event.getBlock().getType());
            logs.remove(event.getBlock());

            logs.sort(Comparator.comparingInt(Block::getY));

        } else {

            logs = new ArrayList<>(Arrays.asList(TreeUtils.getLogsAbove(event.getBlock())));

        }

        // I have really no idea what exactly I did here. There was a problem with
        // falling Blocks being spawned isntead of logs
        // that were on the ground, so they broke immediately and dropped themself. I
        // think I fixed this by the following line
        // if(logAbove.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
        // logs.contains(logAbove) ||
        // logs.contains(logAbove.getRelative(BlockFace.DOWN))) {
        for (Block logAbove : logs) {
            if (logAbove.getRelative(BlockFace.DOWN).getType() == Material.AIR || logs.contains(logAbove)
                    || logs.contains(logAbove.getRelative(BlockFace.DOWN))) {

                BlockData blockData = logAbove.getBlockData().clone();
                logAbove.setType(Material.AIR);
                FallingBlock fallingBlock = logAbove.getLocation().getWorld()
                        .spawn(logAbove.getLocation().add(plugin.fallingBlockOffset), FallingBlock.class,
                               e -> e.setBlockData(blockData));
                if (plugin.getConfig().getBoolean("prevent-torch-exploit")) {
                    fallingBlock.getPersistentDataContainer().set(fallingLogKey, PersistentDataType.BOOLEAN, true);
                }
                if (plugin.getConfig().getBoolean("prevent-torch-exploit-aggressive")) {
                    fallingBlock.setDropItem(false);
                }
            }

        }

    }

}