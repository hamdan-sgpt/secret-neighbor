package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class LocationManager {
    private final SecretNeighborPlugin plugin;

    public LocationManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getLobby() {
        return loadLocation("locations.lobby");
    }

    public void setLobby(Location loc) {
        saveLocation("locations.lobby", loc);
    }

    public Location getHouseEntrance() {
        return loadLocation("locations.house-entrance");
    }

    public void setHouseEntrance(Location loc) {
        saveLocation("locations.house-entrance", loc);
    }

    public Location getHome() {
        return loadLocation("locations.home");
    }

    public void setHome(Location loc) {
        saveLocation("locations.home", loc);
    }

    public Location getBasementDoor() {
        return loadLocation("locations.basement-door");
    }

    public void setBasementDoor(Location loc) {
        saveLocation("locations.basement-door", loc);
    }

    public Location getBasementDoorPlayer() {
        return loadLocation("locations.basement-door-player");
    }

    public void setBasementDoorPlayer(Location loc) {
        saveLocation("locations.basement-door-player", loc);
    }

    public boolean isBasementDoorFlipped() {
        return plugin.getConfig().getBoolean("locations.basement-door-flipped", false);
    }

    public void setBasementDoorFlipped(boolean flipped) {
        plugin.getConfig().set("locations.basement-door-flipped", flipped);
        plugin.saveConfig();
    }

    private Location loadLocation(String path) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path) || config.get(path) == null) {
            if (path.equals("locations.lobby")) {
                String worldName = config.getString("world-name", "secret_neighbor_map_2");
                World world = Bukkit.getWorld(worldName);
                if (world == null) return null;
                double x = config.getDouble("lobby-spawn.x", 88.088);
                double y = config.getDouble("lobby-spawn.y", -60.0);
                double z = config.getDouble("lobby-spawn.z", -10.996);
                float yaw = (float) config.getDouble("lobby-spawn.yaw", 0.0);
                float pitch = (float) config.getDouble("lobby-spawn.pitch", 0.0);
                return new Location(world, x, y, z, yaw, pitch);
            }
            if (path.equals("locations.home") || path.equals("locations.house-entrance")) {
                String worldName = config.getString("world-name", "secret_neighbor_map_2");
                World world = Bukkit.getWorld(worldName);
                if (world == null) return null;
                double x = -4.327;
                double y = -60.0;
                double z = -2.589;
                float yaw = -177.8f;
                float pitch = -4.8f;
                return new Location(world, x, y, z, yaw, pitch);
            }
            return null;
        }
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(String path, Location loc) {
        FileConfiguration config = plugin.getConfig();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        plugin.saveConfig();
    }
}
