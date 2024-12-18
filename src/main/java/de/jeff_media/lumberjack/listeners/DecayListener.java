package de.jeff_media.lumberjack.listeners;

import de.jeff_media.lumberjack.LumberJack;
import de.jeff_media.lumberjack.tasks.DecayTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

public class DecayListener implements Listener {

    private final LumberJack plugin = LumberJack.getInstance();

    @EventHandler(ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!plugin.getConfig().getBoolean("fast-leaves-decay")) return;
        if(plugin.decayTasks.size()<plugin.getConfig().getInt("max-decay-tasks",1000)) {
            plugin.decayTasks.add(new DecayTask(event.getBlock().getState()).runTaskAsynchronously(plugin).getTaskId());
        }
    }


}
