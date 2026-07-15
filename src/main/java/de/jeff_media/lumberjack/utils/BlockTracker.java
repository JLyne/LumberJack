/*
 * Copyright (c) 2023. JEFF Media GbR / mfnalex et al.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.jeff_media.lumberjack.utils;

import java.util.Collection;
import java.util.HashSet;

import de.jeff_media.lumberjack.LumberJack;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks player placed blocks.
 * <p>
 * Uses the chunk's PersistentDataContainer to store information about which blocks have been placed
 * by the player. You can track all block types or only certain ones.
 */
public final class BlockTracker implements Listener {
    private final NamespacedKey playerPlacedKey;
    private final Collection<Material> trackedTypes = new HashSet<>();

	private final LumberJack plugin;

	public BlockTracker(LumberJack plugin) {
		playerPlacedKey = new NamespacedKey(plugin, "playerplaced");
        this.plugin = plugin;
    }

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlace(final BlockPlaceEvent event) {
		if (!isTrackedBlockType(event.getBlock().getType())) return;
		setPlayerPlacedBlock(event.getBlock(), true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBreak(final BlockBreakEvent event) {
		if (isPlayerPlacedBlock(event.getBlock())) {
			plugin.getServer().getScheduler().runTaskLater(
					plugin, () -> setPlayerPlacedBlock(event.getBlock(), false), 1L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onStructureGrow(final StructureGrowEvent event) {
		for (final BlockState blockState : event.getBlocks()) {
			final Block block = blockState.getBlock();
			setPlayerPlacedBlock(block, false);
		}
	}

    /**
     * Adds a new material to the block tracker
     *
     * @param type material to track
     */
    public void addTrackedBlockType(final Material type) {
        trackedTypes.add(type);
    }

    /**
     * Gets a collection containing all tracked materials
     *
     * @return Collection containing all tracked materials
     */
    public Collection<Material> getTrackedBlockTypes() {
        return trackedTypes;
    }

    /**
     * Adds new materials to the block tracker
     *
     * @param types materials to track
     */
    public void addTrackedBlockTypes(final Collection<Material> types) {
        trackedTypes.addAll(types);
    }

    /**
     * Clears the list of tracked materials
     */
    public void clearTrackedBlockTypes() {
        trackedTypes.clear();
    }

    /**
     * Checks whether a given material is already one of the tracked block types.
     *
     * @param type Material to check
     * @return true when this material is already tracked, otherwise false
     */
    public boolean isTrackedBlockType(final Material type) {
        return trackedTypes.contains(type);
    }

    /**
     * Removes all given block types from the list of tracked block types
     *
     * @param types Collection of Materials to stop tracking
     */
    public void removeTrackedBlockTypes(final Collection<Material> types) {
        trackedTypes.removeAll(types);
    }

    /**
     * Checks whether a given block has been placed by a player
     *
     * @param block Block to check
     * @return true when the block was player-placed and tracked, otherwise false
     */
    public boolean isPlayerPlacedBlock(final Block block) {
        final PersistentDataContainer playerPlacedPDC = getPlayerPlacedPDC(block.getChunk());
        return playerPlacedPDC.has(getKey(block), PersistentDataType.BYTE);
    }

    private PersistentDataContainer getPlayerPlacedPDC(final PersistentDataHolder chunk) {
        final PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        return pdc.getOrDefault(playerPlacedKey, PersistentDataType.TAG_CONTAINER, pdc.getAdapterContext().newPersistentDataContainer());
    }

    /**
     * Creates a {@link NamespacedKey} for a block based on its chunk location
     *
     * @param block Block to create the key for
     * @return NamespacedKey for the block
     */
    @Contract("_ -> new")
    private NamespacedKey getKey(@NotNull final Block block) {
        final int x = block.getX() & 0x000F;
        final int y = block.getY();
        final int z = block.getZ() & 0x000F;

        //noinspection HardcodedFileSeparator
        return new NamespacedKey(plugin, String.format("%d/%d/%d", x, y, z));
    }

    /**
     * Gets a collection of all blocks that have been placed by players inside a chunk
     *
     * @param chunk Chunk to check
     * @return Collection of all blocks inside the chunk that have been placed by players
     */
    @NotNull
    public Collection<Block> getPlayerPlacedBlocks(final Chunk chunk) {
        final Collection<Block> blocks = new HashSet<>();
        final PersistentDataContainer pdc = getPlayerPlacedPDC(chunk);
        for (final NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(playerPlacedKey.getNamespace())) continue;
            final String[] parts = key.getKey().split("/");
            final int x = Integer.parseInt(parts[0]);
            final int y = Integer.parseInt(parts[1]);
            final int z = Integer.parseInt(parts[2]);
            blocks.add(chunk.getBlock(x, y, z));
        }
        return blocks;
    }

    /**
     * Manually sets whether a player placed this block
     *
     * @param block        Block
     * @param playerPlaced Whether the block was player placed
     */
    public void setPlayerPlacedBlock(final Block block, final boolean playerPlaced) {
        final PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        final PersistentDataContainer playerPlacedPDC = getPlayerPlacedPDC(block.getChunk());
        final NamespacedKey key = getKey(block);
        if (playerPlaced) {
            playerPlacedPDC.set(key, PersistentDataType.BYTE, (byte) 1);
        } else {
            playerPlacedPDC.remove(key);
        }
        pdc.set(playerPlacedKey, PersistentDataType.TAG_CONTAINER, playerPlacedPDC);
    }

}
