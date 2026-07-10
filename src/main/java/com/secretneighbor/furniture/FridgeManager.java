package com.secretneighbor.furniture;

import com.secretneighbor.SecretNeighborPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FridgeManager {

    private static final int CMD_CLOSED = 1600;
    private static final int CMD_HALF = 1601;
    private static final int CMD_OPEN = 1602;
    private static final String FRIDGE_TAG = "sn_fridge";

    private final SecretNeighborPlugin plugin;

    // Maps armor stand UUID -> its stored inventory contents
    private final Map<UUID, Inventory> fridgeInventories = new HashMap<>();

    // Tracks which player has which fridge open (player UUID -> armor stand UUID)
    private final Map<UUID, UUID> openFridges = new HashMap<>();

    public FridgeManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new refrigerator armor stand and double barrier block structure.
     */
    public ArmorStand spawnFridge(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        
        // Snap spawn location to the grid center horizontally and bottom of block vertically
        Location spawnLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        // Snap yaw to the nearest 90 degrees
        float yaw = Math.round(location.getYaw() / 90.0f) * 90.0f;
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(0.0f);

        // Place two physical barrier blocks for collision (double height)
        blockLoc.getBlock().setType(Material.BARRIER);
        blockLoc.clone().add(0, 1, 0).getBlock().setType(Material.BARRIER);

        // Spawn the armor stand 1.020833 blocks lower to compensate for Minecraft's head slot height offset
        Location standLoc = spawnLoc.clone().add(0, -1.020833, 0);

        ArmorStand stand = standLoc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(false);
            as.setArms(false);
            as.setBasePlate(false);
            as.setCanPickupItems(false);
            as.customName(Component.text("§7Refrigerator"));
            as.setCustomNameVisible(false);
            as.addScoreboardTag(FRIDGE_TAG);

            // Set closed model
            ItemStack helmet = createModelItem(CMD_CLOSED);
            as.getEquipment().setHelmet(helmet);
        });

        // Create a 9-slot inventory for this fridge
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§8Refrigerator"));
        
        // Populate with initial random loot
        Random rand = new Random();
        if (rand.nextDouble() < 0.75) {
            ItemStack tomato = new ItemStack(Material.FEATHER);
            ItemMeta meta = tomato.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§cTomato"));
                meta.setCustomModelData(1106);
                meta.setUnbreakable(true);
                tomato.setItemMeta(meta);
            }
            tomato.setAmount(1 + rand.nextInt(3));
            inv.setItem(rand.nextInt(9), tomato);
        }
        if (rand.nextDouble() < 0.50) {
            inv.setItem(rand.nextInt(9), new ItemStack(Material.APPLE, 1 + rand.nextInt(2)));
        }
        if (rand.nextDouble() < 0.40) {
            inv.setItem(rand.nextInt(9), new ItemStack(Material.MILK_BUCKET));
        }
        if (rand.nextDouble() < 0.35) {
            inv.setItem(rand.nextInt(9), new ItemStack(Material.COOKIE, 2 + rand.nextInt(3)));
        }

        fridgeInventories.put(stand.getUniqueId(), inv);
        return stand;
    }

    /**
     * Opens the refrigerator for a player.
     */
    public void openFridge(Player player, ArmorStand stand) {
        UUID standId = stand.getUniqueId();
        if (openFridges.containsKey(player.getUniqueId()) || openFridges.containsValue(standId)) {
            return; // Already opening or open
        }

        // Open sounds & mist particles
        Location soundLoc = stand.getLocation().clone().add(0, 2.020833, 0);
        soundLoc.getWorld().playSound(soundLoc, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1.1f);
        soundLoc.getWorld().playSound(soundLoc, Sound.BLOCK_SNOW_BREAK, 0.8f, 1.5f);
        
        soundLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, soundLoc, 8, 0.2, 0.3, 0.2, 0.02);
        soundLoc.getWorld().spawnParticle(Particle.CLOUD, soundLoc, 4, 0.1, 0.2, 0.1, 0.01);

        // Run opening animation over 2 ticks (Closed -> Half -> Open)
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (!stand.isValid()) {
                    cancel();
                    return;
                }
                step++;
                if (step == 1) {
                    stand.getEquipment().setHelmet(createModelItem(CMD_HALF));
                } else if (step == 2) {
                    stand.getEquipment().setHelmet(createModelItem(CMD_OPEN));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        Inventory inv = fridgeInventories.computeIfAbsent(standId,
                k -> Bukkit.createInventory(null, 9, Component.text("§8Refrigerator")));
        openFridges.put(player.getUniqueId(), standId);
        player.openInventory(inv);
    }

    /**
     * Called when a player closes the refrigerator inventory.
     */
    public void closeFridge(Player player) {
        UUID standId = openFridges.remove(player.getUniqueId());
        if (standId == null) return;

        Location soundLoc = player.getLocation();
        player.getWorld().playSound(soundLoc, Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1.1f);

        // Find the armor stand and run closing animation
        player.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                .filter(as -> as.getUniqueId().equals(standId))
                .findFirst()
                .ifPresent(as -> {
                    new BukkitRunnable() {
                        int step = 0;
                        @Override
                        public void run() {
                            if (!as.isValid()) {
                                cancel();
                                    return;
                            }
                            step++;
                            if (step == 1) {
                                as.getEquipment().setHelmet(createModelItem(CMD_HALF));
                            } else if (step == 2) {
                                as.getEquipment().setHelmet(createModelItem(CMD_CLOSED));
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 2L);
                });
    }

    public boolean isFridge(ArmorStand stand) {
        return stand.getScoreboardTags().contains(FRIDGE_TAG);
    }

    public boolean hasOpenFridge(UUID playerUuid) {
        return openFridges.containsKey(playerUuid);
    }

    public Inventory getOrCreateInventory(UUID standId) {
        return fridgeInventories.computeIfAbsent(standId,
                k -> Bukkit.createInventory(null, 9, Component.text("§8Refrigerator")));
    }

    public void removeAll() {
        openFridges.clear();
        fridgeInventories.clear();
    }

    private ItemStack createModelItem(int customModelData) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.displayName(Component.text("§7Refrigerator"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
