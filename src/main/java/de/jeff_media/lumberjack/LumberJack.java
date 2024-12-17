package de.jeff_media.lumberjack;

import com.destroystokyo.paper.MaterialSetTag;
import com.jeff_media.jefflib.BlockTracker;
import com.jeff_media.jefflib.JeffLib;
import com.jeff_media.jefflib.pluginhooks.PlaceholderAPIUtils;
import com.jeff_media.morepersistentdatatypes.DataType;
import de.jeff_media.lumberjack.commands.CommandLumberjack;
import de.jeff_media.lumberjack.config.ConfigUpdater;
import de.jeff_media.lumberjack.config.Messages;
import de.jeff_media.lumberjack.data.PlayerSetting;
import de.jeff_media.lumberjack.listeners.BlockBreakListener;
import de.jeff_media.lumberjack.listeners.BlockPlaceListener;
import de.jeff_media.lumberjack.listeners.DecayListener;
import de.jeff_media.lumberjack.listeners.PlayerListener;
import de.jeff_media.lumberjack.utils.TreeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class LumberJack extends JavaPlugin {

    private static LumberJack instance;
    public final Vector fallingBlockOffset = new Vector(0.5, 0.0, 0.5);
    public final int maxTreeSize = 50;
    @SuppressWarnings("FieldCanBeLocal")
    private final int currentConfigVersion = 14;
    public TreeUtils treeUtils;
    public Messages messages;
    public ArrayList<String> disabledWorlds;
    boolean gravityEnabledByDefault = false;
    HashMap<Player, PlayerSetting> perPlayerSettings;
    boolean debug = false;
    private boolean usingMatchingConfig = true;
    public final Set<Integer> decayTasks = new HashSet<>();

    public HashSet<BukkitTask> getScheduledTasks() {
        return scheduledTasks;
    }

    private final HashSet<BukkitTask> scheduledTasks = new HashSet<>();

    public static LumberJack getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        JeffLib.registerBlockTracker();

        // %lumberjack_enabled%
        PlaceholderAPIUtils.register("enabled", (player) -> {
            if(!player.isOnline()) return "false";
            return String.valueOf(getPlayerSetting(player.getPlayer()).gravityEnabled);
        });

        createConfig();

        messages = new Messages(this);
        treeUtils = new TreeUtils(this);
        BlockBreakListener blockBreakListener = new BlockBreakListener(this);
        BlockPlaceListener blockPlaceListener = new BlockPlaceListener(this);
        DecayListener decayListener = new DecayListener();
        PlayerListener playerListener = new PlayerListener(this);
        CommandLumberjack commandLumberjack = new CommandLumberjack(this);
        Objects.requireNonNull(getCommand("lumberjack")).setExecutor(commandLumberjack);
        getServer().getPluginManager().registerEvents(blockBreakListener, this);
        getServer().getPluginManager().registerEvents(blockPlaceListener, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(decayListener, this);

        perPlayerSettings = new HashMap<>();

        gravityEnabledByDefault = getConfig().getBoolean("gravity-enabled-by-default");

        trackBlocks();
    }

    private void trackBlocks() {
        Set<Material> trackedBlocks = new HashSet<>(MaterialSetTag.LOGS.getValues());
        trackedBlocks.removeAll(MaterialSetTag.MANGROVE_LOGS.getValues());

        BlockTracker.addTrackedBlockTypes(trackedBlocks);
    }

    private void showOldConfigWarning() {
        getLogger().warning("==============================================");
        getLogger().warning("You were using an old config file. LumberJack");
        getLogger().warning("has updated the file to the newest version.");
        getLogger().warning("Your changes have been kept.");
        getLogger().warning("==============================================");
    }

    private void createConfig() {
        saveDefaultConfig();

        if (getConfig().getInt("config-version", 0) != currentConfigVersion) {
            showOldConfigWarning();
            ConfigUpdater configUpdater = new ConfigUpdater(this);
            configUpdater.updateConfig();
            usingMatchingConfig = true;
            //createConfig();
        }

        File playerDataFolder = new File(getDataFolder().getPath() + File.separator + "playerdata");
        if (!playerDataFolder.getAbsoluteFile().exists()) {
            playerDataFolder.mkdir();
        }

        getConfig().addDefault("gravity-enabled-by-default", false);
        getConfig().addDefault("use-pdc",true);
        getConfig().addDefault("check-for-updates", "true");
        getConfig().addDefault("show-message-again-after-logout", true);
        getConfig().addDefault("attached-logs-fall-down", true);
        getConfig().addDefault("prevent-torch-exploit", true);
        getConfig().addDefault("must-use-axe", true);
        getConfig().addDefault("max-air-in-trunk", 1);
        getConfig().addDefault("fast-leaves-decay", false);
        getConfig().addDefault("fast-leaves-decay-duration", 10);
        getConfig().addDefault("only-natural-logs", true);

        // Load disabled-worlds. If it does not exist in the config, it returns null. That's no problem
        disabledWorlds = (ArrayList<String>) getConfig().getStringList("disabled-worlds");

    }

    public PlayerSetting getPlayerSetting(Player p) {
        registerPlayer(p);
        return perPlayerSettings.get(p);
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            unregisterPlayer(p);
        }
        for(BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            if(task.getOwner() == this) {
                task.cancel();
            }
        }
    }

    public void togglePlayerSetting(Player p) {
        registerPlayer(p);
        boolean enabled = perPlayerSettings.get(p).gravityEnabled;
        perPlayerSettings.get(p).gravityEnabled = !enabled;
    }

    public void registerPlayer(Player p) {
        if (!perPlayerSettings.containsKey(p)) {

            File playerFile = new File(getDataFolder() + File.separator + "playerdata",
                    p.getUniqueId() + ".yml");
            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            if(getConfig().getBoolean("use-pdc")) {
                //System.out.println("PDC enabled");
                if(playerFile.exists()) {
                    playerFile.delete();
                }
                if(p.getPersistentDataContainer().has(new NamespacedKey(this,NBTKeys.SETTINGS),DataType.FILE_CONFIGURATION)) {
                    //System.out.println("Loaded PDC");
                    playerConfig = p.getPersistentDataContainer().get(new NamespacedKey(this, NBTKeys.SETTINGS),DataType.FILE_CONFIGURATION);
                }
            }

            boolean activeForThisPlayer;

            if (!playerConfig.isSet("gravityEnabled")) {
                activeForThisPlayer = gravityEnabledByDefault;
            } else {
                activeForThisPlayer = playerConfig.getBoolean("gravityEnabled");
            }

            PlayerSetting newSettings = new PlayerSetting(activeForThisPlayer);
            if (!getConfig().getBoolean("show-message-again-after-logout")) {
                newSettings.hasSeenMessage = playerConfig.getBoolean("hasSeenMessage");
            }
            perPlayerSettings.put(p, newSettings);
        }
    }

    public void unregisterPlayer(Player p) {
        if (perPlayerSettings.containsKey(p)) {
            PlayerSetting setting = getPlayerSetting(p);
            File playerFile = new File(getDataFolder() + File.separator + "playerdata",
                    p.getUniqueId() + ".yml");
            YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
            playerConfig.set("gravityEnabled", setting.gravityEnabled);
            playerConfig.set("hasSeenMessage", setting.hasSeenMessage);

            if(getConfig().getBoolean("use-pdc")) {
                p.getPersistentDataContainer().set(new NamespacedKey(this,NBTKeys.SETTINGS), DataType.FILE_CONFIGURATION,playerConfig);
            } else {
                try {
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            perPlayerSettings.remove(p);
        }
    }

    public void reload() {
        reloadConfig();
    }
}
	

