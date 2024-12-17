package de.jeff_media.lumberjack.utils;

import com.destroystokyo.paper.MaterialSetTag;
import de.jeff_media.lumberjack.LumberJack;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Map;

public class TreeUtils {

    private final LumberJack main;

    private static final Map<Tag<Material>, Material> treeFlavors = Map.of(
            MaterialSetTag.ACACIA_LOGS, Material.ACACIA_LOG,
            MaterialSetTag.BIRCH_LOGS, Material.BIRCH_LOG,
            MaterialSetTag.CRIMSON_STEMS, Material.CRIMSON_STEM,
            MaterialSetTag.CHERRY_LOGS, Material.CHERRY_LOG,
            MaterialSetTag.DARK_OAK_LOGS, Material.DARK_OAK_LOG,
            MaterialSetTag.JUNGLE_LOGS, Material.JUNGLE_LOG,
            MaterialSetTag.MANGROVE_LOGS, Material.MANGROVE_LOG,
            MaterialSetTag.OAK_LOGS, Material.OAK_LOG,
            MaterialSetTag.SPRUCE_LOGS, Material.SPRUCE_LOG,
            MaterialSetTag.WARPED_STEMS, Material.WARPED_STEM
    );

    public TreeUtils(LumberJack main) {
        this.main = main;
    }

    static Material[] getValidGroundTypes(Material mat) {
        if(MaterialSetTag.CRIMSON_STEMS.isTagged(mat)) {
            return new Material[]{
                    Material.CRIMSON_NYLIUM,
                    Material.NETHERRACK
            };
        }

        if(MaterialSetTag.WARPED_STEMS.isTagged(mat)) {
            return new Material[]{
                    Material.WARPED_NYLIUM,
                    Material.NETHERRACK
            };
        }

        if(MaterialSetTag.MANGROVE_LOGS.isTagged(mat)) {
            return new Material[] {
                    Material.MANGROVE_ROOTS,
                    Material.MUDDY_MANGROVE_ROOTS,
                    Material.MUD,
                    Material.AIR
            };
        }

        if(MaterialSetTag.LOGS.isTagged(mat)) {
            return new Material[]{
                    Material.DIRT,
                    Material.GRASS_BLOCK,
                    Material.MYCELIUM,
                    Material.COARSE_DIRT,
                    Material.PODZOL,
                    Material.SNOW_BLOCK,
                    Material.ROOTED_DIRT,
                    Material.MOSS_BLOCK
            };
        }

        if(Material.MANGROVE_ROOTS == mat) {
            return new Material[] {
                    Material.MUDDY_MANGROVE_ROOTS,
                    Material.MUD
            };
        }

        return null;
    }

    public static boolean matchesTrunkType(Material mat, Material mat2) {
        if(mat==mat2) return true;
        String n1 = mat.name().replace("STRIPPED_","").replace("_WOOD","_LOG");
        String n2 = mat2.name().replace("STRIPPED_","").replace("_WOOD","_LOG");
        return n1.equals(n2);
    }

    public static boolean isAboveNonSolidBlock(Block block) {

        for (int height = block.getY() - 1; height >= 0; height--) {
            Block candidate = block.getWorld().getBlockAt(block.getX(), height, block.getZ());
            if (candidate.getType().isSolid() || candidate.getType() == Material.MANGROVE_ROOTS) {
                return true;
            }
            if (candidate.getType() != Material.AIR) {
                return false;
            }

        }
        return true;
    }

    static Material getFlavor(Material mat) {
        for (Tag<Material> tag : treeFlavors.keySet()) {
            if(tag.isTagged(mat)) {
                return treeFlavors.get(tag);
            }
        }

        return null;
    }

    static ArrayList<Block> getAdjacent(Block block) {
        ArrayList<Block> blocks = new ArrayList<>();
        Block above = block.getRelative(BlockFace.UP);
        Material mat = block.getType();

        final BlockFace[] faces = {BlockFace.SOUTH, BlockFace.SOUTH_EAST, BlockFace.EAST, BlockFace.NORTH_EAST,
                BlockFace.NORTH, BlockFace.NORTH_WEST, BlockFace.WEST, BlockFace.SOUTH_WEST};

        if (matchesTrunkType(above.getType(),mat)) {
            blocks.add(above);
        }

        for (BlockFace face : faces) {
            if (block.getRelative(face).getType() == mat) blocks.add(block.getRelative(face));
        }

        for (BlockFace face : faces) {
            if (above.getRelative(face).getType() == mat) blocks.add(above.getRelative(face));
        }
        //blocks.forEach((b) -> System.out.println("  "+b.getType()+"@"+b.getLocation()));

        return blocks;
    }

    public static void getTreeTrunk2(Block block, ArrayList<Block> list, Material mat) {
        if (!matchesTrunkType(mat, block.getType())) return;
        if (!list.contains(block)) {
            list.add(block);
            //System.out.println("adding "+block.getType().name()+"@"+block.getLocation());

            for (Block next : getAdjacent(block)) {
                getTreeTrunk2(next, list, mat);
            }
        }
    }

    public boolean isPartOfTree(Block block) {
        return isPartOfTree(block.getType());
    }

    public boolean isOnTreeGround(Block block) {

        int maxAirInBetween = main.getConfig().getInt("max-air-in-trunk");
        int airInBetween = 0;
        Block currentBlock = block;

        while (isPartOfTree(currentBlock) || currentBlock.getType().isAir()) {

            if (currentBlock.getType().isAir()) {
                airInBetween++;
                if (airInBetween > maxAirInBetween) {
                    return false;
                }
            }

            currentBlock = currentBlock.getRelative(BlockFace.DOWN);
        }

        Material[] validGroundTypes = getValidGroundTypes(block.getType());

        if(validGroundTypes == null) return false;

        for (Material mat : validGroundTypes) {
            //System.out.println("Checking whether " + mat + " == " + currentBlock.getType());
            if (mat == currentBlock.getType()) {
                //System.out.println("YES");
                return true;
            }
        }
        return false;
    }

    boolean isPartOfTree(Material mat) {
        return MaterialSetTag.LOGS.isTagged(mat);
    }

    public Block[] getLogsAbove(Block block) {
        Material flavor = getFlavor(block.getType());
        ArrayList<Block> list = new ArrayList<>();
        Block currentBlock = block.getRelative(BlockFace.UP);
        while (isPartOfTree(currentBlock) && list.size() < main.maxTreeSize && getFlavor(currentBlock.getType()) == flavor) {
            list.add(currentBlock);
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }
        return list.toArray(new Block[0]);
    }
}
