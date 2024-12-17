package de.jeff_media.lumberjack.utils;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Leaves;

import java.util.Collection;
import java.util.HashSet;

public class DecayUtils {

    private static final int MAX_DISTANCE = 6;
    private static final int RADIUS = 5;

    public static Collection<Block> getLeaves(BlockState originalLeaf) {
        Collection<Block> blocks = new HashSet<>();
        int blockX = originalLeaf.getX();
        int blockY = originalLeaf.getY();
        int blockZ = originalLeaf.getZ();
        World world = originalLeaf.getWorld();
        for (int x = blockX - RADIUS; x <= blockX + RADIUS; x++) {
            for (int y = blockY - RADIUS; y <= blockY + RADIUS; y++) {
                for (int z = blockZ - RADIUS; z <= blockZ + RADIUS; z++) {
                    Block candidate = world.getBlockAt(x, y, z);
                    if(candidate == originalLeaf.getBlock()) continue;
                    if(candidate==null) continue;
                    if (candidate.getType().isAir()) continue;
                    if (!isLeaf(candidate)) {
                        continue;
                    }
                    if (!isMatchingLeaf(originalLeaf.getType(), candidate.getType())) {
                        continue;
                    }
                    Leaves leaves = (Leaves) candidate.getBlockData();
                    if (leaves.isPersistent()) {
                        continue;
                    }
                    if (leaves.getDistance() <= MAX_DISTANCE) {
                        continue;
                    }
                    blocks.add(candidate);
                }
            }
        }
        return blocks;
    }

    public static boolean isLeaf(Block block) {
        return isLeaf(block.getType());
    }

    private static boolean isLeaf(Material material) {
        return Tag.LEAVES.isTagged(material);
    }

    private static boolean isMatchingLeaf(Material leaf1, Material leaf2) {
        switch (leaf1) {
            case AZALEA_LEAVES:
            case FLOWERING_AZALEA_LEAVES:
                return leaf2 == Material.AZALEA_LEAVES || leaf2 == Material.FLOWERING_AZALEA_LEAVES;
            default:
                return leaf1 == leaf2;
        }
    }
}
