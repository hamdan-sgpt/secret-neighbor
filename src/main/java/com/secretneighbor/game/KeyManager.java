package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.*;

public class KeyManager implements Listener {

    private final SecretNeighborPlugin plugin;
    private final int[] activeKeys = new int[6];
    private final Map<Integer, ItemDisplay> padlockEntities = new HashMap<>();
    private boolean unlocked = false;

    public KeyManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getInsertedCount() {
        return 6 - padlockEntities.size();
    }

    public int getKeysInserted() {
        return getInsertedCount();
    }

    /**
     * Finds the locations of all drawers that currently contain the real keys.
     */
    public List<org.bukkit.Location> getActiveKeyLocations() {
        List<org.bukkit.Location> locs = new java.util.ArrayList<>();
        org.bukkit.World world = org.bukkit.Bukkit.getWorld("secret_neighbor_map_2");
        if (world == null) return locs;

        for (org.bukkit.entity.ArmorStand stand : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class)) {
            if (plugin.getDrawerManager().isDrawer(stand)) {
                org.bukkit.inventory.Inventory inv = plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
                if (inv != null) {
                    for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
                        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                            int cmd = item.getItemMeta().getCustomModelData();
                            for (int k : activeKeys) {
                                if (k == cmd) {
                                    locs.add(stand.getLocation());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return locs;
    }

    /**
     * Get the drawer inventory for a drawer armor stand.
     * Delegates to DrawerManager.
     */
    public org.bukkit.inventory.Inventory getDrawerInventory(org.bukkit.entity.ArmorStand stand) {
        if (stand == null) return null;
        return plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
    }

    public void clear() {
        java.util.Arrays.fill(activeKeys, 0);
        unlocked = false;
        clearPadlocks();
    }

    public void spawnPadlocks(Location doorLoc) {
        clearPadlocks();
        if (doorLoc == null || !(doorLoc.getBlock().getBlockData() instanceof Door)) return;

        Door doorData = (Door) doorLoc.getBlock().getBlockData();
        World world = doorLoc.getWorld();

        // 2x3 grid positions on the door face
        double[] hOffsets = {-0.2, 0.2};
        double[] vOffsets = {0.3, 0.0, -0.3};

        // Randomize the 6 keys
        int[] allKeys = {1000, 1001, 1002, 1003, 1004, 1005};
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            activeKeys[i] = allKeys[rand.nextInt(allKeys.length)];
        }

        int idx = 0;
        for (double v : vOffsets) {
            for (double h : hOffsets) {
                if (idx >= 6) break;
                int keyCmd = activeKeys[idx];
                int padlockCmd = keyCmd + 500; // 1000 -> 1500, etc.

                Location loc = calculatePadlockLocation(doorLoc, doorData, h, v, 0.53);

                ItemDisplay display = world.spawn(loc, ItemDisplay.class, entity -> {
                    entity.setItemStack(createModelItem(padlockCmd));
                    entity.addScoreboardTag("sn_padlock");
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    
                    Transformation transform = entity.getTransformation();
                    transform.getScale().set(0.4f, 0.4f, 0.4f); // 0.4x scale
                    entity.setTransformation(transform);
                });

                padlockEntities.put(idx, display);
                idx++;
            }
        }

        // Scatter the keys and loot as 3D items in the map
        scatter3DItems(world);
    }

    public void updatePadlockPositions(Location doorLoc) {
        if (doorLoc == null || !(doorLoc.getBlock().getBlockData() instanceof Door)) return;
        Door doorData = (Door) doorLoc.getBlock().getBlockData();

        double[] hOffsets = {-0.2, 0.2};
        double[] vOffsets = {0.3, 0.0, -0.3};

        for (Map.Entry<Integer, ItemDisplay> entry : padlockEntities.entrySet()) {
            int slotIdx = entry.getKey();
            ItemDisplay display = entry.getValue();
            if (display != null && display.isValid()) {
                double h = hOffsets[slotIdx % 2];
                double v = vOffsets[slotIdx / 2];
                Location loc = calculatePadlockLocation(doorLoc, doorData, h, v, 0.53);
                display.teleport(loc);
            }
        }
    }

    private Location findSafeSurface(Location drawerLoc) {
        Random rand = new Random();
        for (int attempts = 0; attempts < 15; attempts++) {
            double rx = drawerLoc.getX() + (rand.nextDouble() * 5.0 - 2.5);
            double rz = drawerLoc.getZ() + (rand.nextDouble() * 5.0 - 2.5);
            double ry = drawerLoc.getY(); // start at drawer level

            Location check = new Location(drawerLoc.getWorld(), rx, ry, rz);
            // Search vertically around drawer height to find a floor
            for (int dy = -2; dy <= 2; dy++) {
                Location candidate = check.clone().add(0, dy, 0);
                Block b = candidate.getBlock();
                Block under = candidate.clone().add(0, -1, 0).getBlock();

                if (b.getType() == Material.AIR && under.getType().isSolid() && under.getType() != Material.BARRIER) {
                    // Calculate precise Y on top of the floor block
                    double targetY = under.getY() + 1.0;
                    if (under.getType().name().contains("SLAB")) {
                        targetY = under.getY() + 0.5;
                    } else if (under.getType().name().contains("CARPET")) {
                        targetY = under.getY() + 0.0625;
                    }
                    return new Location(drawerLoc.getWorld(), rx, targetY, rz);
                }
            }
        }
        // Fallback: place on top of drawer (1 block above)
        return drawerLoc.clone().add(0, 1.0, 0);
    }

    private void spawn3DItem(Location loc, ItemStack itemStack) {
        World world = loc.getWorld();
        int cmd = 0;
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasCustomModelData()) {
            cmd = itemStack.getItemMeta().getCustomModelData();
        }

        float scale = com.secretneighbor.listener.ThrowableListener.getItemScale(cmd);
        float halfHeight = com.secretneighbor.listener.ThrowableListener.getItemHalfHeight(cmd);

        // Precise Y offset: raise by halfHeight so the item bottom sits on the floor
        double yOffset = halfHeight;
        Location adjustedLoc = loc.clone().add(0, yOffset, 0);

        world.spawn(adjustedLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack);
            entity.addScoreboardTag("sn_3d_item");
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

            Transformation transform = entity.getTransformation();
            transform.getScale().set(scale, scale, scale);

            // Random yaw for natural scatter, pitch -90 to lie flat on the ground
            float yaw = new Random().nextFloat() * 360f;
            float pitch = -90f + (new Random().nextFloat() * 10f - 5f); // ±5° tilt
            entity.setRotation(yaw, pitch);

            entity.setTransformation(transform);
        });
    }

    private void scatter3DItems(World world) {
        List<org.bukkit.entity.ArmorStand> drawers = new ArrayList<>();
        for (org.bukkit.entity.ArmorStand stand : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class)) {
            if (plugin.getDrawerManager().isDrawer(stand)) {
                drawers.add(stand);
            }
        }

        if (drawers.isEmpty()) {
            plugin.getLogger().warning("[SecretNeighbor] No drawers found in world to scatter keys/loot!");
            return;
        }

        // Clear all drawer inventories first so we can stock them properly
        for (org.bukkit.entity.ArmorStand stand : drawers) {
            org.bukkit.inventory.Inventory inv = plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
            if (inv != null) inv.clear();
        }

        Random rand = new Random();

        // 1. Shuffle drawers list to distribute keys evenly (no stacking/clustering of keys in same drawers)
        List<org.bukkit.entity.ArmorStand> shuffledDrawers = new ArrayList<>(drawers);
        Collections.shuffle(shuffledDrawers);

        int drawerIndex = 0;

        // Distribute active matching keys in unique random drawers (nyebar)
        for (int keyCmd : activeKeys) {
            if (drawerIndex >= shuffledDrawers.size()) {
                drawerIndex = 0; // fallback in case of extremely few drawers
            }
            org.bukkit.entity.ArmorStand stand = shuffledDrawers.get(drawerIndex++);
            org.bukkit.inventory.Inventory inv = plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
            if (inv != null) {
                inv.addItem(createKeyItem(keyCmd));
            }
        }

        // Distribute 2 to 4 extra "fake/useless" keys in other unique drawers
        int extraKeyCount = 2 + rand.nextInt(3);
        int[] allKeys = {1000, 1001, 1002, 1003, 1004, 1005};
        for (int i = 0; i < extraKeyCount; i++) {
            if (drawerIndex >= shuffledDrawers.size()) {
                drawerIndex = 0;
            }
            int fakeKeyCmd = allKeys[rand.nextInt(allKeys.length)];
            org.bukkit.entity.ArmorStand stand = shuffledDrawers.get(drawerIndex++);
            org.bukkit.inventory.Inventory inv = plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
            if (inv != null) {
                inv.addItem(createKeyItem(fakeKeyCmd));
            }
        }

        // Arrays of items to distribute randomly
        int[] throwCmds = {1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110};
        String[] throwNames = {
            "§6Box", "§eChair", "§8TV", "§cSofa",
            "§aPainting", "§4Book", "§cTomato", "§eBroom",
            "§6Basketball", "§fPillow", "§6Hat"
        };

        int[] utilCmds = {1010, 1011, 1012, 1013};
        String[] utilNames = {"§7Crowbar", "§fFlashlight", "§aRadar", "§cHammer"};

        int[] cardCmds = {1014, 1015, 1016};
        String[] cardNames = {"§bKeycard Lvl 1", "§6Keycard Lvl 2", "§cKeycard Lvl 3"};

        // Populate drawers with random items (Throwables, Utilities, Keycards)
        // Bikin se-random mungkin!
        for (org.bukkit.entity.ArmorStand stand : drawers) {
            org.bukkit.inventory.Inventory inv = plugin.getDrawerManager().getOrCreateInventory(stand.getUniqueId());
            if (inv == null) continue;

            // Roll for Throwables: 50% chance to have 1 or 2 throwables
            if (rand.nextDouble() < 0.50) {
                int count = 1 + rand.nextInt(2);
                for (int c = 0; c < count; c++) {
                    int throwIdx = rand.nextInt(throwCmds.length);
                    inv.addItem(createCustomItem(throwNames[throwIdx], throwCmds[throwIdx]));
                }
            }

            // Roll for Utilities: 30% chance to have a utility item
            if (rand.nextDouble() < 0.30) {
                int utilIdx = rand.nextInt(utilCmds.length);
                inv.addItem(createCustomItem(utilNames[utilIdx], utilCmds[utilIdx]));
            }

            // Roll for Keycards: 15% chance to have a keycard
            if (rand.nextDouble() < 0.15) {
                int cardIdx = rand.nextInt(cardCmds.length);
                inv.addItem(createCustomItem(cardNames[cardIdx], cardCmds[cardIdx]));
            }
        }

        // --- PHYSICAL 3D MAP SCATTER (THROWABLES ONLY) ---
        // Keep some 3D throwables scattered around the map too for visual immersion
        List<org.bukkit.entity.ArmorStand> shuffledForScatter = new ArrayList<>(drawers);
        Collections.shuffle(shuffledForScatter);

        List<Location> spawnedLocations = new ArrayList<>();
        int maxThrowables = 20;
        int spawnedCount = 0;

        for (org.bukkit.entity.ArmorStand stand : shuffledForScatter) {
            if (spawnedCount >= maxThrowables) break;

            Location loc = findSafeSurface(stand.getLocation());
            if (loc == null) continue;

            // Check if it's too close to other throwables (minimum 8 blocks)
            boolean tooClose = false;
            for (Location existing : spawnedLocations) {
                if (existing.getWorld().equals(loc.getWorld()) && existing.distanceSquared(loc) < 64.0) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            int throwIdx = rand.nextInt(throwCmds.length);
            int cmd = throwCmds[throwIdx];

            ItemStack throwItem = createCustomItem(throwNames[throwIdx], cmd);
            spawn3DItem(loc, throwItem);

            spawnedLocations.add(loc);
            spawnedCount++;
        }

        plugin.getLogger().info("[SecretNeighbor] Placed keys/utils/throwables in drawers and scattered " + spawnedCount + " 3D throwables.");
    }

    private ItemStack createCustomItem(String name, int cmd) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.setCustomModelData(cmd);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createKeyItem(int keyCmd) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(getKeyName(keyCmd)));
            meta.setCustomModelData(keyCmd);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Location calculatePadlockLocation(Location doorLoc, Door doorData, double h, double v, double normalOffset) {
        BlockFace face = doorData.getFacing();
        boolean isOpen = doorData.isOpen();
        Door.Hinge hinge = doorData.getHinge();
        
        boolean flipped = plugin.getLocationManager().isBasementDoorFlipped();
        double signedOffset = flipped ? -normalOffset : normalOffset;

        // 1. Calculate closed position (relative to block center 0.5, 0.5)
        double relX = 0.5;
        double relZ = 0.5;
        float yaw = 0;

        switch (face) {
            case SOUTH -> {
                relX = 0.5 + h;
                relZ = 0.5 + signedOffset;
            }
            case NORTH -> {
                relX = 0.5 - h;
                relZ = 0.5 - signedOffset;
            }
            case EAST -> {
                relX = 0.5 + signedOffset;
                relZ = 0.5 - h;
            }
            case WEST -> {
                relX = 0.5 - signedOffset;
                relZ = 0.5 + h;
            }
            default -> {}
        }

        // Determine padlock yaw based on the side it renders on
        BlockFace side = flipped ? face.getOppositeFace() : face;
        switch (side) {
            case SOUTH -> yaw = 0;
            case NORTH -> yaw = 180;
            case EAST -> yaw = 270;
            case WEST -> yaw = 90;
            default -> yaw = 0;
        }

        // 2. If open, rotate around hinge pivot
        if (isOpen) {
            double pivotX = 0.5;
            double pivotZ = 0.5;
            double doorPlaneOffset = 0.1875;
            double edgePos = 0.5 + (0.5 - doorPlaneOffset); // 0.8125
            double oppositeEdgePos = 0.5 - (0.5 - doorPlaneOffset); // 0.1875

            // Find hinge pivot based on facing and hinge side
            switch (face) {
                case SOUTH -> {
                    pivotZ = edgePos;
                    pivotX = (hinge == Door.Hinge.LEFT) ? 0.0 : 1.0;
                }
                case NORTH -> {
                    pivotZ = oppositeEdgePos;
                    pivotX = (hinge == Door.Hinge.LEFT) ? 1.0 : 0.0;
                }
                case EAST -> {
                    pivotX = edgePos;
                    pivotZ = (hinge == Door.Hinge.LEFT) ? 1.0 : 0.0;
                }
                case WEST -> {
                    pivotX = oppositeEdgePos;
                    pivotZ = (hinge == Door.Hinge.LEFT) ? 0.0 : 1.0;
                }
                default -> {}
            }

            boolean ccw = (hinge == Door.Hinge.LEFT);
            
            // Rotate relX, relZ around pivotX, pivotZ by 90 degrees
            double dx = relX - pivotX;
            double dz = relZ - pivotZ;
            
            if (ccw) {
                relX = pivotX - dz;
                relZ = pivotZ + dx;
                yaw = (yaw + 90) % 360;
            } else {
                relX = pivotX + dz;
                relZ = pivotZ - dx;
                yaw = (yaw - 90 + 360) % 360;
            }
        }

        Location loc = doorLoc.clone();
        if (doorData.getHalf() == Bisected.Half.TOP) {
            loc.add(0, -1, 0);
        }
        Location finalLoc = new Location(loc.getWorld(), loc.getBlockX() + relX, loc.getBlockY() + 1.0 + v, loc.getBlockZ() + relZ, yaw, 0);
        Bukkit.getLogger().info("[SN-Debug] calcPadlock: face=" + face + ", flipped=" + flipped + ", offset=" + signedOffset + ", yaw=" + yaw + ", relX=" + relX + ", relZ=" + relZ + " -> final: X=" + finalLoc.getX() + ", Y=" + finalLoc.getY() + ", Z=" + finalLoc.getZ() + ", yaw=" + finalLoc.getYaw());
        return finalLoc;
    }

    public void removePadlock(int slotIdx) {
        ItemDisplay display = padlockEntities.remove(slotIdx);
        if (display != null && display.isValid()) {
            display.remove();
            display.getWorld().playSound(display.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1.5f);
            display.getWorld().spawnParticle(Particle.CRIT, display.getLocation(), 15, 0.1, 0.1, 0.1, 0.1);
        }
    }

    public void clearPadlocks() {
        for (ItemDisplay display : padlockEntities.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        padlockEntities.clear();

        // Safety: clean up any dangling padlock or 3D item entities
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display != null && (display.getScoreboardTags().contains("sn_padlock") || display.getScoreboardTags().contains("sn_3d_item"))) {
                    display.remove();
                }
            }
        }
    }

    private ItemStack createModelItem(int cmd) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(cmd);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getKeyName(int cmd) {
        return switch (cmd) {
            case 1000 -> "§cRed Key";
            case 1001 -> "§9Blue Key";
            case 1002 -> "§aGreen Key";
            case 1003 -> "§eYellow Key";
            case 1004 -> "§5Purple Key";
            case 1005 -> "§6Orange Key";
            default -> "§7Key";
        };
    }

    @EventHandler
    public void onPlayerRightClick3DItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        // Find nearest 3D item looking at
        ItemDisplay nearest = null;
        double nearestDist = 2.8; // max reach distance

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        for (ItemDisplay display : player.getWorld().getEntitiesByClass(ItemDisplay.class)) {
            if (!display.getScoreboardTags().contains("sn_3d_item")) continue;

            Location loc = display.getLocation().add(0, 0.2, 0); // item center offset
            double dist = loc.distance(eye);
            if (dist < nearestDist) {
                Vector toItem = loc.toVector().subtract(eye.toVector()).normalize();
                double dot = dir.dot(toItem);
                if (dot > 0.85) {
                    nearestDist = dist;
                    nearest = display;
                }
            }
        }

        if (nearest != null) {
            event.setCancelled(true);

            // Try to pick up the item
            ItemStack item = nearest.getItemStack();
            if (item == null) return;

            // Check if player's inventory is full
            boolean hasSpace = false;
            for (ItemStack content : player.getInventory().getStorageContents()) {
                if (content == null || content.getType() == Material.AIR) {
                    hasSpace = true;
                    break;
                }
            }

            if (!hasSpace) {
                player.sendActionBar(Component.text("§cYour inventory is full!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            // Add to inventory
            player.getInventory().addItem(item);
            nearest.remove();

            // Play pickup sound and particles
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            player.spawnParticle(Particle.CRIT, nearest.getLocation().add(0, 0.2, 0), 10, 0.1, 0.1, 0.1, 0.05);

            // Inform player
            String itemName = "Item";
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                itemName = item.getItemMeta().getDisplayName();
            }
            player.sendActionBar(Component.text("§aPicked up " + itemName));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        
        // Handle unlocked keycard doors (anyone can open/close)
        if (type == Material.OXIDIZED_COPPER_DOOR) {
            event.setCancelled(true);
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
                Block bottomBlock = (door.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) ? block.getRelative(BlockFace.DOWN) : block;
                if (bottomBlock.getBlockData() instanceof org.bukkit.block.data.type.Door bottomDoor) {
                    boolean newOpenState = !bottomDoor.isOpen();
                    bottomDoor.setOpen(newOpenState);
                    bottomBlock.setBlockData(bottomDoor);
                    Sound sound = newOpenState ? Sound.BLOCK_COPPER_DOOR_OPEN : Sound.BLOCK_COPPER_DOOR_CLOSE;
                    bottomBlock.getWorld().playSound(bottomBlock.getLocation(), sound, 1f, 1f);
                }
            }
            return;
        }

        boolean isKeycardDoor = (type == Material.COPPER_DOOR || type == Material.EXPOSED_COPPER_DOOR || type == Material.WEATHERED_COPPER_DOOR);
        if (isKeycardDoor) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
            if (snp == null) return;

            if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
                player.sendMessage("§cThe game is not active!");
                return;
            }

            boolean canOpen = false;
            int reqLvl = 0;
            if (type == Material.COPPER_DOOR) {
                reqLvl = 1;
            } else if (type == Material.EXPOSED_COPPER_DOOR) {
                reqLvl = 2;
            } else if (type == Material.WEATHERED_COPPER_DOOR) {
                reqLvl = 3;
            }

            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.FEATHER && held.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = held.getItemMeta();
                if (meta.hasCustomModelData()) {
                    int cmd = meta.getCustomModelData();
                    if (reqLvl == 1 && cmd == 1014) canOpen = true;
                    else if (reqLvl == 2 && cmd == 1015) canOpen = true;
                    else if (reqLvl == 3 && cmd == 1016) canOpen = true;
                }
            }

            if (canOpen) {
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
                    Block bottomBlock = (door.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) ? block.getRelative(BlockFace.DOWN) : block;
                    Location bottomLoc = bottomBlock.getLocation();
                    Location topLoc = bottomLoc.clone().add(0, 1, 0);

                    if (bottomBlock.getBlockData() instanceof org.bukkit.block.data.type.Door bottomDoor) {
                        org.bukkit.block.BlockFace facing = bottomDoor.getFacing();
                        org.bukkit.block.data.type.Door.Hinge hinge = bottomDoor.getHinge();

                        // Unlocks permanently and keycard is consumed (if child)!
                        if (!snp.isNeighbor()) {
                            held = player.getInventory().getItemInMainHand();
                            if (held != null && held.getType() != Material.AIR) {
                                held.setAmount(held.getAmount() - 1);
                            }
                        }

                        org.bukkit.block.data.type.Door bottomDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.OXIDIZED_COPPER_DOOR);
                        bottomDoorData.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
                        bottomDoorData.setFacing(facing);
                        bottomDoorData.setHinge(hinge);
                        bottomDoorData.setOpen(true); // Auto-open upon unlock

                        org.bukkit.block.data.type.Door topDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.OXIDIZED_COPPER_DOOR);
                        topDoorData.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
                        topDoorData.setFacing(facing);
                        topDoorData.setHinge(hinge);
                        topDoorData.setOpen(true);

                        bottomBlock.setType(Material.OXIDIZED_COPPER_DOOR, false);
                        topLoc.getBlock().setType(Material.OXIDIZED_COPPER_DOOR, false);

                        topLoc.getBlock().setBlockData(topDoorData, false);
                        bottomBlock.setBlockData(bottomDoorData, true);

                        bottomBlock.getWorld().playSound(bottomBlock.getLocation(), Sound.BLOCK_COPPER_DOOR_OPEN, 1f, 1.2f);
                        bottomBlock.getWorld().playSound(bottomBlock.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                        player.sendMessage("§a§l✔ Door unlocked permanently with Keycard!");
                        player.sendActionBar(Component.text("§aDoor Unlocked (Green indicator)"));
                    }
                }
            } else {
                String colorName = (reqLvl == 1) ? "§bLevel 1 (Blue)" : ((reqLvl == 2) ? "§6Level 2 (Yellow)" : "§cLevel 3 (Red)");
                player.sendActionBar(Component.text("§cYou need a " + colorName + " §cKeycard to open this door!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            }
            return;
        }

        Location exitDoorLoc = plugin.getLocationManager().getBasementDoor();
        if (exitDoorLoc == null) return;

        // Check if clicked block is the exit door (either top or bottom half)
        boolean isExitDoor = false;
        Location blockLoc = block.getLocation();
        if (blockLoc.getBlockX() == exitDoorLoc.getBlockX() && blockLoc.getBlockZ() == exitDoorLoc.getBlockZ()) {
            if (blockLoc.getBlockY() == exitDoorLoc.getBlockY() || blockLoc.getBlockY() == exitDoorLoc.getBlockY() + 1) {
                isExitDoor = true;
            }
        }

        if (!isExitDoor) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null) return;

        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
            player.sendMessage("§cThe game is not active!");
            return;
        }

        if (snp.isNeighbor()) {
            player.sendMessage("§cNeighbors cannot unlock the exit door!");
            return;
        }

        if (unlocked) {
            player.sendMessage("§aThe exit door is already fully unlocked! Escape!");
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean isKey = false;
        int keyCmd = -1;

        if (heldItem.getType() == Material.FEATHER && heldItem.hasItemMeta()) {
            ItemMeta meta = heldItem.getItemMeta();
            if (meta.hasCustomModelData()) {
                int cmd = meta.getCustomModelData();
                if (cmd >= 1000 && cmd <= 1005) {
                    isKey = true;
                    keyCmd = cmd;
                }
            }
        }

        if (!isKey) {
            player.sendActionBar(Component.text("§eExit Door Progress: §a" + (6 - padlockEntities.size()) + "/6 locks unlocked."));
            player.sendMessage("§eYou need a matching Key to unlock this door!");
            return;
        }

        int matchingSlot = -1;
        for (Map.Entry<Integer, ItemDisplay> entry : padlockEntities.entrySet()) {
            int slotIdx = entry.getKey();
            if (activeKeys[slotIdx] == keyCmd) {
                matchingSlot = slotIdx;
                break;
            }
        }

        if (matchingSlot == -1) {
            player.sendMessage("§cNo remaining lock on the door matches this key!");
            return;
        }

        // Insert key!
        heldItem.setAmount(heldItem.getAmount() - 1);
        removePadlock(matchingSlot);

        int insertedCount = 6 - padlockEntities.size();
        plugin.getGameManager().broadcast("§e§l🔑 " + player.getName() + " §7inserted the " + getKeyName(keyCmd) + " §7key into the exit door! §e(" + insertedCount + "/6)");

        if (padlockEntities.isEmpty()) {
            unlocked = true;
            
            // Unlock/Open the door by turning both blocks to air
            Location bottomDoor = exitDoorLoc.clone();
            if (exitDoorLoc.getBlock().getBlockData() instanceof Door door) {
                if (door.getHalf() == Bisected.Half.TOP) {
                    bottomDoor.add(0, -1, 0);
                }
            }
            bottomDoor.getBlock().setType(Material.AIR);
            bottomDoor.clone().add(0, 1, 0).getBlock().setType(Material.AIR);

            // Play explosion/unlock sounds and particle effects
            bottomDoor.getWorld().playSound(bottomDoor, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            bottomDoor.getWorld().playSound(bottomDoor, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
            bottomDoor.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, bottomDoor.clone().add(0.5, 1.0, 0.5), 1);

            plugin.getGameManager().broadcast("§a§lTHE EXIT DOOR HAS BEEN UNLOCKED! ESCAPE NOW!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!unlocked) return;
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || snp.isNeighbor() || !snp.isAlive()) return;

        Location exitLoc = plugin.getLocationManager().getBasementDoor();
        if (exitLoc == null) return;

        // Compare against center of the door block
        Location exitCenter = exitLoc.clone().add(0.5, 0, 0.5);
        if (player.getLocation().distanceSquared(exitCenter) < 2.25) {
            plugin.getGameManager().childrenEscape();
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Location exitLoc = plugin.getLocationManager().getBasementDoor();
        if (exitLoc == null) return;
        
        Block block = event.getBlock();
        if (block.getLocation().getBlockX() == exitLoc.getBlockX() && block.getLocation().getBlockZ() == exitLoc.getBlockZ()) {
            if (block.getLocation().getBlockY() == exitLoc.getBlockY() || block.getLocation().getBlockY() == exitLoc.getBlockY() + 1) {
                Bukkit.getLogger().info("[SN-Debug] onBlockPhysics: physics event detected at exit door location block Y=" + block.getY());
                // Wait 1 tick for block data state to update fully in Minecraft, then refresh padlock coordinates
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updatePadlockPositions(exitLoc);
                }, 1L);
            }
        }
    }
}
