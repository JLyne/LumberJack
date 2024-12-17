package de.jeff_media.lumberjack.listeners;

import com.destroystokyo.paper.MaterialSetTag;
import com.jeff_media.jefflib.BlockTracker;
import de.jeff_media.lumberjack.LumberJack;
import de.jeff_media.lumberjack.NBTKeys;
import de.jeff_media.lumberjack.data.AxeMaterial;
import de.jeff_media.lumberjack.utils.TreeUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
    public void onLeavesBreak(BlockBreakEvent event) {

        if (!(event.getBlock().getBlockData() instanceof Leaves) && !MaterialSetTag.WART_BLOCKS.isTagged(event.getBlock().getType())) {
            return;
        }

        if (event.getBlock().getBlockData() instanceof Leaves) {
            if (((Leaves) event.getBlock().getBlockData()).isPersistent()) {
                return;
            }
        }
        if (BlockTracker.isPlayerPlacedBlock(event.getBlock())) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {

        //System.out.println(1);

        // checking in lower case for lazy admins
        if (plugin.disabledWorlds.contains(event.getBlock().getWorld().getName().toLowerCase())) {
            return;
        }

        //System.out.println(2);

        if (!plugin.treeUtils.isPartOfTree(event.getBlock())) {
            return;
        }

        //System.out.println(3);

        if (!plugin.treeUtils.isOnTreeGround(event.getBlock())) {
            //System.out.println("No valid tree ground");
            return;
        }

        if(!event.getPlayer().hasPermission("lumberjack.use")) {
            //System.out.println("No permission");
            return;
        }

        //System.out.println(4);

        // Tree gravity does not work for player placed blocks
        if (plugin.getConfig().getBoolean("only-natural-logs") && BlockTracker.isPlayerPlacedBlock(event.getBlock())) {
            return;
        }

        //System.out.println(5);

        // Dont show message when gravity is forced
        if ((!event.getPlayer().hasPermission("lumberjack.force") || event.getPlayer().hasPermission("lumberjack.force.ignore"))
                && event.getPlayer().hasPermission("lumberjack.use")) {
            Player p = event.getPlayer();
            if (!plugin.getPlayerSetting(p).gravityEnabled) {
                if (!plugin.getPlayerSetting(p).hasSeenMessage) {
                    plugin.getPlayerSetting(p).hasSeenMessage = true;
                    if (plugin.getConfig().getBoolean("show-message-when-breaking-log")) {
                        p.sendMessage(plugin.messages.MSG_COMMANDMESSAGE);
                    }
                }
                return;
            } else {
                if (!plugin.getPlayerSetting(p).hasSeenMessage) {
                    plugin.getPlayerSetting(p).hasSeenMessage = true;
                    if (plugin.getConfig().getBoolean("show-message-when-breaking-log-and-gravity-is-enabled")) {
                        p.sendMessage(plugin.messages.MSG_COMMANDMESSAGE2);
                    }
                }
            }
        }

        //System.out.println(6);

        // check if axe has to be used
        if (plugin.getConfig().getBoolean("must-use-axe")) {
            if (!MaterialSetTag.ITEMS_AXES.isTagged(event.getPlayer().getInventory().getItemInMainHand().getType())) {
                return;
            }
            AxeMaterial requiredAxe = AxeMaterial.get(plugin.getConfig().getString("requires-at-least"));
            if (!AxeMaterial.isAtLeast(event.getPlayer().getInventory().getItemInMainHand().getType(), requiredAxe)) {
                return;
            }
        }

        //System.out.println(7);

        // check if player must sneak
        if (plugin.getConfig().getBoolean("must-sneak")) {
            if (!event.getPlayer().isSneaking()) {
                return;
            }
        }

        //System.out.println(8);

        if(!MaterialSetTag.MANGROVE_LOGS.isTagged(event.getBlock().getType())) {
            // fix for torch bug part 2
            if (plugin.getConfig().getBoolean("prevent-torch-exploit") && !TreeUtils.isAboveNonSolidBlock(event.getBlock())) {
                return;
            }
        }

        //System.out.println(9);

        if (!plugin.getPlayerSetting(event.getPlayer()).gravityEnabled
                && event.getPlayer().hasPermission("lumberjack.force.ignore")) {
            return;
        }

        //System.out.println(10);

        if (!plugin.getPlayerSetting(event.getPlayer()).gravityEnabled
                && !event.getPlayer().hasPermission("lumberjack.force")) {
            return;
        }

        //System.out.println(11);

        ArrayList<Block> logs;

        // Atached logs fall down
        if (plugin.getConfig().getBoolean("attached-logs-fall-down")) {

            logs = new ArrayList<>();
            TreeUtils.getTreeTrunk2(event.getBlock().getRelative(BlockFace.UP), logs, event.getBlock().getType());
            logs.remove(event.getBlock());

            logs.sort(Comparator.comparingInt(Block::getY));

        } else {

            logs = new ArrayList<>(Arrays.asList(plugin.treeUtils.getLogsAbove(event.getBlock())));

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
                        .spawnFallingBlock(logAbove.getLocation().add(plugin.fallingBlockOffset), blockData);
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