package com.secretneighbor.map;

import com.secretneighbor.SecretNeighborPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MapGenerator {
    private final SecretNeighborPlugin plugin;

    public MapGenerator(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Extracts and loads the map under its official name 'secret_neighbor_map_2'
     * inside Bukkit's World Container.
     */
    public void generate(CommandSender sender) {
        String worldName = "secret_neighbor_map_2";
        sender.sendMessage("§e[1/4] Checking active world session...");
        plugin.getLogger().info("Generating fresh game world: '" + worldName + "'");

        // 1. Unload world if currently loaded
        World gameWorld = Bukkit.getWorld(worldName);
        if (gameWorld != null) {
            sender.sendMessage("§eUnloading active '" + worldName + "' session...");
            World defaultWorld = Bukkit.getWorlds().get(0);
            for (Player p : gameWorld.getPlayers()) {
                p.teleport(defaultWorld.getSpawnLocation());
                p.sendMessage("§e[Secret Neighbor] Map is being regenerated. Teleporting you to safety.");
            }
            boolean unloaded = Bukkit.unloadWorld(gameWorld, false);
            if (!unloaded) {
                sender.sendMessage("§cWarning: Failed to unload world '" + worldName + "' cleanly. Trying to proceed...");
            }
        }

        // 2. Copy the map files
        sender.sendMessage("§e[2/4] Copying world files...");
        
        // We run the copy asynchronously to prevent server lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String path = plugin.getConfig().getString("map-folder-path", "plugins/SecretNeighbor/map_template");
                File sourceDir = new File(path);
                
                // If template folder doesn't exist and matches default, extract it from JAR first
                if (path.equals("plugins/SecretNeighbor/map_template") && !sourceDir.exists()) {
                    sender.sendMessage("§eTemplate map tidak ditemukan. Mengekstrak map bawaan ke " + path + "...");
                    sourceDir.mkdirs();
                    extractMapZipTo(sourceDir, sender);
                }

                if (!sourceDir.exists()) {
                    throw new java.io.FileNotFoundException("Source folder " + path + " tidak ditemukan! Periksa setting 'map-folder-path' di config.yml.");
                }

                File targetDir = new File(Bukkit.getWorldContainer(), worldName);
                if (targetDir.exists()) {
                    deleteDirectory(targetDir);
                }
                targetDir.mkdirs();
                
                copyDirectory(sourceDir, targetDir);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§7Successfully copied map files.");
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§cError during file copy: " + e.getMessage());
                });
                plugin.getLogger().severe("Failed to copy map folder: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Verify file existence in Spigot's World Container
            File targetDir = new File(Bukkit.getWorldContainer(), worldName);
            File levelDat = new File(targetDir, "level.dat");
            File regionDir = new File(targetDir, "region");

            // Load and finalize world synchronously on the main server thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§b[Debug] Folder: " + targetDir.getName());
                sender.sendMessage("§b[Debug] level.dat exists: " + levelDat.exists() + " (" + levelDat.length() + " bytes)");
                sender.sendMessage("§b[Debug] region folder exists: " + regionDir.exists());

                // 3. Load the world using Bukkit's WorldCreator
                sender.sendMessage("§e[3/4] Creating and loading world into server...");
                World world;
                try {
                    world = Bukkit.createWorld(new WorldCreator(worldName));
                } catch (Exception e) {
                    sender.sendMessage("§cError loading world into Bukkit: " + e.getMessage());
                    plugin.getLogger().severe("Failed to load world into Bukkit: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }

                if (world != null) {
                    // 4. Set Gamerules
                    sender.sendMessage("§e[4/4] Finalizing configuration...");
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setGameRule(GameRule.MOB_GRIEFING, false);
                    world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                    world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
                    world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);

                    // Set spawn location in the world
                    Location spawnLoc = new Location(world, 88.088, -60.0, -10.996, 0.0f, 0.0f);
                    world.setSpawnLocation(spawnLoc);

                    // Clear all lingering floating items or displays that might have been saved in the map template
                    for (org.bukkit.entity.Entity ent : world.getEntities()) {
                        if (ent instanceof org.bukkit.entity.Item || ent instanceof org.bukkit.entity.ItemDisplay) {
                            ent.remove();
                        }
                    }

                    // Spawn preset drawers immediately so they are visible on the map
                    plugin.getGameManager().spawnPresetDrawers();

                    // 5. Teleport player and notify
                    if (sender instanceof Player player) {
                        player.teleport(spawnLoc);
                    }
                    
                    Location lobby = plugin.getLocationManager().getLobby();
                    if (lobby != null) {
                        for (com.secretneighbor.player.SNPlayer snp : plugin.getGameManager().getPlayers().values()) {
                            Player p = Bukkit.getPlayer(snp.getUuid());
                            if (p != null) {
                                p.teleport(lobby);
                                p.sendMessage("§a§l✔ Map has been reset. Teleported you to the Lobby!");
                            }
                        }
                    }
                    
                    sender.sendMessage("§a§l✔ Map generated and loaded successfully as '" + worldName + "'!");
                } else {
                    sender.sendMessage("§cError: Bukkit returned null when creating world '" + worldName + "'. Check console for errors.");
                }
            });
        });
    }

    private void extractMapZipTo(File targetDir, CommandSender sender) throws IOException {
        try (InputStream is = plugin.getResource("Secret_Neighbor_Map_2.zip")) {
            if (is == null) {
                throw new java.io.FileNotFoundException("Secret_Neighbor_Map_2.zip not found in JAR resources!");
            }
            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                int count = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith("session.lock")) {
                        zis.closeEntry();
                        continue;
                    }
                    String entryName = entry.getName().replace('\\', '/');
                    File file = new File(targetDir, entryName);
                    if (!file.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
                        zis.closeEntry();
                        continue;
                    }
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        File parent = file.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        count++;
                    }
                    zis.closeEntry();
                }
                
                final int finalCount = count;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§7Successfully decompressed " + finalCount + " map files to template directory.");
                });
            }
        }
    }

    private void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            path.delete();
        }
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            if (source.getName().equals("session.lock")) {
                return;
            }
            try (java.io.InputStream in = new java.io.FileInputStream(source);
                 java.io.OutputStream out = new java.io.FileOutputStream(destination)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
