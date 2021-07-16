package com.github.ms5984.survivelist.survivelistevents.model;

import com.github.ms5984.survivelist.survivelistevents.util.DataFile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage player data which must persist across server connects.
 */
public class PlayerDataService {
    private final Map<UUID, DataFile> resolvedData = new ConcurrentHashMap<>();

    private DataFile resolvePlayer(Player player) {
        return resolvedData.computeIfAbsent(player.getUniqueId(), uid -> new DataFile("users", uid.toString() + ".yml"));
    }

    /**
     * Get the original location of the player when they joined the event.
     *
     * @param player the player
     * @return the location where the player joined
     */
    Location getOriginalLocation(Player player) {
        return resolvePlayer(player).getValueNow(fc -> fc.getLocation("original-location"));
    }

    /**
     * Set the location of the player before they joined the event.
     *
     * @param player the player
     */
    void setOriginalLocation(Player player) {
        final DataFile dataFile = resolvePlayer(player);
        final Location location = player.getLocation().clone();
        dataFile.update(fc -> fc.set("original-location", location)).whenComplete((n, e) -> dataFile.save());
    }

    /**
     * Clear data for the provided player.
     *
     * @param player the player
     */
    void clearData(Player player) {
        resolvePlayer(player).delete();
    }

    /**
     * Wipe the entire users data folder.
     * <p>
     * Always called onDisable.
     */
    public static void clearCache(JavaPlugin javaPlugin) {
        for (File userFile : new File(javaPlugin.getDataFolder(), "users").listFiles()) {
            if (userFile.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                userFile.delete();
            }
        }
    }
}
