package de.jeff_media.lumberjack.tasks;

import com.jeff_media.jefflib.Ticks;
import de.jeff_media.lumberjack.LumberJack;
import de.jeff_media.lumberjack.utils.DecayUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Random;

public class DecayTask extends BukkitRunnable {

    private static final LumberJack plugin = LumberJack.getInstance();
    private static final Random rand = new Random();
	private final Collection<Block> leaves;

    public DecayTask(BlockState leaf) {
		this.leaves = DecayUtils.getLeaves(leaf);
    }

    @Override
    public void run() {
        if (!isCancelled()) {
            for (Block leaf : leaves) {
                if (isCancelled()) return;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    if (DecayUtils.isLeaf(leaf)) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            leaf.breakNaturally();
                        });
                    }
                }, rand.nextInt((int) Ticks.fromSeconds(plugin.getConfig().getDouble("fast-leaves-decay-duration"))));
            }
        }
        plugin.decayTasks.remove(getTaskId());
    }
}
