package com.secretneighbor.furniture;

import com.secretneighbor.SecretNeighborPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DrawerManager {

    private static final int CMD_CLOSED = 1400;
    private static final int CMD_OPEN = 1401;
    private static final String DRAWER_TAG = "sn_drawer";

    private final SecretNeighborPlugin plugin;

    // Maps armor stand UUID -> its stored inventory contents
    private final Map<UUID, Inventory> drawerInventories = new HashMap<>();

    // Tracks which player has which drawer open (player UUID -> armor stand UUID)
    private final Map<UUID, UUID> openDrawers = new HashMap<>();

    public DrawerManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new drawer armor stand at the given location.
     */
    public ArmorStand spawnDrawer(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        
        // Snap spawn location to the grid center horizontally and bottom of block vertically
        Location spawnLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        // Snap yaw to the nearest 90 degrees
        float yaw = Math.round(location.getYaw() / 90.0f) * 90.0f;
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(0.0f);

        // Clear decorations in and around the drawer area
        clearDecorationsAround(blockLoc, false, yaw);

        // Place a physical barrier block for player collision
        blockLoc.getBlock().setType(Material.BARRIER);

        // Spawn the armor stand 1.020833 blocks lower to compensate for Minecraft's default head slot height offset.
        Location standLoc = spawnLoc.clone().add(0, -1.020833, 0);

        ArmorStand stand = standLoc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(false);
            as.setArms(false);
            as.setBasePlate(false);
            as.setCanPickupItems(false);
            as.customName(Component.text("§7Drawer"));
            as.setCustomNameVisible(false);
            as.addScoreboardTag(DRAWER_TAG);

            // Set closed drawer model on helmet
            ItemStack helmet = createModelItem(CMD_CLOSED);
            as.getEquipment().setHelmet(helmet);
        });

        // Create a 1-row (9 slot) inventory for this drawer
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("§8Drawer"));
        drawerInventories.put(stand.getUniqueId(), inv);

        return stand;
    }

    /**
     * Opens the drawer for a player.
     */
    public void openDrawer(Player player, ArmorStand stand) {
        UUID standId = stand.getUniqueId();
        if (openDrawers.containsKey(player.getUniqueId()) || openDrawers.containsValue(standId)) {
            return; // Already opening or open
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1f, 1.2f);

        ItemStack helmet = stand.getEquipment().getHelmet();
        int cmd = (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasCustomModelData()) ? helmet.getItemMeta().getCustomModelData() : 0;

        if (cmd == 1410) {
            stand.getEquipment().setHelmet(createModelItem(1411)); // Set open long drawer model
            Inventory inv = drawerInventories.computeIfAbsent(standId,
                    k -> Bukkit.createInventory(null, 27, Component.text("§8Long Drawer")));
            openDrawers.put(player.getUniqueId(), standId);
            player.openInventory(inv);
        } else if (cmd == 1420) {
            stand.getEquipment().setHelmet(createModelItem(1421)); // Set open long alt drawer model
            Inventory inv = drawerInventories.computeIfAbsent(standId,
                    k -> Bukkit.createInventory(null, 27, Component.text("§8Long Drawer")));
            openDrawers.put(player.getUniqueId(), standId);
            player.openInventory(inv);
        } else {
            // Run opening animation over 4 ticks (0.2s)
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
                        stand.getEquipment().setHelmet(createModelItem(1402)); // 25% open
                    } else if (step == 2) {
                        stand.getEquipment().setHelmet(createModelItem(1403)); // 50% open
                    } else if (step == 3) {
                        stand.getEquipment().setHelmet(createModelItem(1404)); // 75% open
                    } else if (step == 4) {
                        stand.getEquipment().setHelmet(createModelItem(CMD_OPEN)); // 100% open
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);

            Inventory inv = drawerInventories.computeIfAbsent(standId,
                    k -> Bukkit.createInventory(null, 9, Component.text("§8Drawer")));
            openDrawers.put(player.getUniqueId(), standId);
            player.openInventory(inv);
        }
    }

    /**
     * Called when a player closes the drawer inventory.
     */
    public void closeDrawer(Player player) {
        UUID standId = openDrawers.remove(player.getUniqueId());
        if (standId == null) return;

        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_CLOSE, 1f, 1.2f);

        // Find the armor stand and run closing animation
        player.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                .filter(as -> as.getUniqueId().equals(standId))
                .findFirst()
                .ifPresent(as -> {
                    ItemStack helmet = as.getEquipment().getHelmet();
                    int cmd = (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasCustomModelData()) ? helmet.getItemMeta().getCustomModelData() : 0;
                    if (cmd == 1411) {
                        as.getEquipment().setHelmet(createModelItem(1410)); // Set closed long drawer model
                    } else if (cmd == 1421) {
                        as.getEquipment().setHelmet(createModelItem(1420)); // Set closed long alt drawer model
                    } else {
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
                                    as.getEquipment().setHelmet(createModelItem(1404)); // 75% open
                                } else if (step == 2) {
                                    as.getEquipment().setHelmet(createModelItem(1403)); // 50% open
                                } else if (step == 3) {
                                    as.getEquipment().setHelmet(createModelItem(1402)); // 25% open
                                } else if (step == 4) {
                                    as.getEquipment().setHelmet(createModelItem(CMD_CLOSED)); // 0% open
                                    cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }
                });
    }

    /**
     * Returns true if this armor stand is a drawer.
     */
    public boolean isDrawer(ArmorStand stand) {
        return stand.getScoreboardTags().contains(DRAWER_TAG);
    }

    /**
     * Returns true if the player currently has a drawer open.
     */
    public boolean hasOpenDrawer(UUID playerUuid) {
        return openDrawers.containsKey(playerUuid);
    }

    /**
     * Pre-stocks a drawer with a specific item (e.g., a key).
     */
    public void addItemToDrawer(ArmorStand stand, ItemStack item) {
        Inventory inv = drawerInventories.get(stand.getUniqueId());
        if (inv != null) {
            inv.addItem(item);
        }
    }

    public Inventory getOrCreateInventory(UUID standId) {
        return drawerInventories.computeIfAbsent(standId,
                k -> Bukkit.createInventory(null, 9, Component.text("§8Drawer")));
    }

    public ArmorStand spawnLongDrawer(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        
        // Snap spawn location to the grid center horizontally and bottom of block vertically
        Location spawnLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        // Snap yaw to the nearest 90 degrees
        float yaw = Math.round(location.getYaw() / 90.0f) * 90.0f;
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(0.0f);

        // Clear decorations in and around the drawer area
        clearDecorationsAround(blockLoc, true, yaw);

        // Place 3 physical barrier blocks based on orientation
        blockLoc.getBlock().setType(Material.BARRIER);
        
        int absYaw = Math.abs((int) yaw) % 360;
        if (absYaw == 90 || absYaw == 270) {
            // Facing East or West -> width along Z axis
            blockLoc.clone().add(0, 0, 1).getBlock().setType(Material.BARRIER);
            blockLoc.clone().add(0, 0, -1).getBlock().setType(Material.BARRIER);
        } else {
            // Facing North or South -> width along X axis
            blockLoc.clone().add(1, 0, 0).getBlock().setType(Material.BARRIER);
            blockLoc.clone().add(-1, 0, 0).getBlock().setType(Material.BARRIER);
        }

        // Spawn the armor stand 1.020833 blocks lower to compensate for Minecraft's head slot offset
        Location standLoc = spawnLoc.clone().add(0, -1.020833, 0);

        ArmorStand stand = standLoc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(false);
            as.setArms(false);
            as.setBasePlate(false);
            as.setCanPickupItems(false);
            as.customName(Component.text("§7Long Drawer"));
            as.setCustomNameVisible(false);
            as.addScoreboardTag(DRAWER_TAG);
            as.addScoreboardTag("sn_drawer_long");

            // Set closed long drawer model on helmet
            ItemStack helmet = createModelItem(1410);
            as.getEquipment().setHelmet(helmet);
        });

        // Create a 3-row (27 slot) inventory for this long drawer
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8Long Drawer"));
        drawerInventories.put(stand.getUniqueId(), inv);

        return stand;
    }

    public ArmorStand spawnLongAltDrawer(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        
        Location spawnLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        float yaw = Math.round(location.getYaw() / 90.0f) * 90.0f;
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(0.0f);

        // Clear decorations in and around the drawer area
        clearDecorationsAround(blockLoc, true, yaw);

        // Place 3 physical barrier blocks based on orientation
        blockLoc.getBlock().setType(Material.BARRIER);
        
        int absYaw = Math.abs((int) yaw) % 360;
        if (absYaw == 90 || absYaw == 270) {
            blockLoc.clone().add(0, 0, 1).getBlock().setType(Material.BARRIER);
            blockLoc.clone().add(0, 0, -1).getBlock().setType(Material.BARRIER);
        } else {
            blockLoc.clone().add(1, 0, 0).getBlock().setType(Material.BARRIER);
            blockLoc.clone().add(-1, 0, 0).getBlock().setType(Material.BARRIER);
        }

        Location standLoc = spawnLoc.clone().add(0, -1.020833, 0);

        ArmorStand stand = standLoc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(false);
            as.setArms(false);
            as.setBasePlate(false);
            as.setCanPickupItems(false);
            as.customName(Component.text("§7Long Drawer"));
            as.setCustomNameVisible(false);
            as.addScoreboardTag(DRAWER_TAG);
            as.addScoreboardTag("sn_drawer_long");

            // Set closed long alt drawer model on helmet
            ItemStack helmet = createModelItem(1420);
            as.getEquipment().setHelmet(helmet);
        });

        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8Long Drawer"));
        drawerInventories.put(stand.getUniqueId(), inv);

        return stand;
    }

    /**
     * Removes all drawers and clears data.
     */
    public void removeAll() {
        openDrawers.clear();
        drawerInventories.clear();
    }

    /**
     * Clears all drawer entities and their corresponding physical barrier blocks from the given world.
     */
    public void clearDrawersFromWorld(org.bukkit.World world) {
        if (world == null) return;
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (isDrawer(stand)) {
                boolean isLong = stand.getScoreboardTags().contains("sn_drawer_long");
                Location centerBlockLoc = stand.getLocation().clone().add(0, 1.020833, 0).getBlock().getLocation();
                float yaw = stand.getLocation().getYaw();
                
                // Clear decorations around the drawer block first
                clearDecorationsAround(centerBlockLoc, isLong, yaw);

                stand.remove();
                if (centerBlockLoc.getBlock().getType() == Material.BARRIER) {
                    centerBlockLoc.getBlock().setType(Material.AIR);
                }
                if (isLong) {
                    int absYaw = Math.abs((int) yaw) % 360;
                    if (absYaw == 90 || absYaw == 270) {
                        Location loc1 = centerBlockLoc.clone().add(0, 0, 1);
                        Location loc2 = centerBlockLoc.clone().add(0, 0, -1);
                        if (loc1.getBlock().getType() == Material.BARRIER) loc1.getBlock().setType(Material.AIR);
                        if (loc2.getBlock().getType() == Material.BARRIER) loc2.getBlock().setType(Material.AIR);
                    } else {
                        Location loc1 = centerBlockLoc.clone().add(1, 0, 0);
                        Location loc2 = centerBlockLoc.clone().add(-1, 0, 0);
                        if (loc1.getBlock().getType() == Material.BARRIER) loc1.getBlock().setType(Material.AIR);
                        if (loc2.getBlock().getType() == Material.BARRIER) loc2.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        removeAll();
    }

    /**
     * Clears decorative blocks and hanging entities around a drawer to prevent clipping/visual clutter.
     */
    public void clearDecorationsAround(Location blockLoc, boolean isLong, float yaw) {
        org.bukkit.World world = blockLoc.getWorld();
        if (world == null) return;

        int cy = blockLoc.getBlockY();

        // Identify occupied block coordinates
        List<Location> occupied = new ArrayList<>();
        occupied.add(blockLoc);
        if (isLong) {
            int absYaw = Math.abs((int) yaw) % 360;
            if (absYaw == 90 || absYaw == 270) {
                occupied.add(blockLoc.clone().add(0, 0, 1));
                occupied.add(blockLoc.clone().add(0, 0, -1));
            } else {
                occupied.add(blockLoc.clone().add(1, 0, 0));
                occupied.add(blockLoc.clone().add(-1, 0, 0));
            }
        }

        // Determine bounding box
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Location loc : occupied) {
            int lx = loc.getBlockX();
            int lz = loc.getBlockZ();
            if (lx < minX) minX = lx;
            if (lx > maxX) maxX = lx;
            if (lz < minZ) minZ = lz;
            if (lz > maxZ) maxZ = lz;
        }

        // Expand bounds: 1 block horizontally, Y from cy to cy + 1
        minX -= 1;
        maxX += 1;
        int minY = cy;
        int maxY = cy + 1;
        minZ -= 1;
        maxZ += 1;

        // 1. Clear blocks matching decorative material types
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.Block b = world.getBlockAt(x, y, z);
                    Material mat = b.getType();
                    if (isCleanableMaterial(mat)) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }

        // 2. Clear decorative entities (Item Frames, Glow Item Frames, Paintings, Banners)
        Location center = blockLoc.clone().add(0.5, 0.5, 0.5);
        double range = isLong ? 2.5 : 1.5;
        world.getNearbyEntities(center, range, 1.5, range).forEach(entity -> {
            if (entity instanceof org.bukkit.entity.ItemFrame || 
                entity instanceof org.bukkit.entity.GlowItemFrame || 
                entity instanceof org.bukkit.entity.Painting) {
                entity.remove();
            }
        });
    }

    private boolean isCleanableMaterial(Material mat) {
        if (mat == null || mat == Material.AIR) return false;
        String name = mat.name();
        return name.endsWith("_TRAPDOOR") || 
               name.endsWith("_BANNER") || 
               name.endsWith("_WALL_BANNER") || 
               name.endsWith("_CARPET") || 
               name.endsWith("_BUTTON") || 
               name.contains("_SIGN") || 
               mat == Material.LADDER || 
               mat == Material.VINE || 
               mat == Material.GLOW_LICHEN;
    }

    private ItemStack createModelItem(int customModelData) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.displayName(Component.text("§7Drawer"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
