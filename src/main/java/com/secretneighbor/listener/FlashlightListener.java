package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class FlashlightListener implements Listener {

    private static final int FLASHLIGHT_CMD = 1011;
    
    private final SecretNeighborPlugin plugin;
    private final Set<UUID> activeFlashlights = new HashSet<>();
    private final Map<UUID, Location> activeLights = new HashMap<>();
    private final Map<UUID, Location> activeAmbientLights = new HashMap<>();

    private final BlockData lightData15;
    private final BlockData lightData8;

    public FlashlightListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;

        // Initialize block data once for performance
        BlockData data15 = org.bukkit.Bukkit.createBlockData(Material.LIGHT);
        if (data15 instanceof Light light) {
            light.setLevel(15);
        }
        this.lightData15 = data15;

        BlockData data8 = org.bukkit.Bukkit.createBlockData(Material.LIGHT);
        if (data8 instanceof Light light) {
            light.setLevel(8);
        }
        this.lightData8 = data8;

        startUpdateTask();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFlashlight(item)) {
            item = player.getInventory().getItemInOffHand();
        }

        if (isFlashlight(item)) {
            event.setCancelled(true);
            toggleFlashlight(player);
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (activeFlashlights.contains(player.getUniqueId())) {
            // Check main hand after the switch (run next tick)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        ItemStack held = player.getInventory().getItemInMainHand();
                        if (!isFlashlight(held)) {
                            turnOff(player);
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (activeFlashlights.contains(player.getUniqueId())) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            ItemStack held = player.getInventory().getItemInMainHand();
                            ItemStack off = player.getInventory().getItemInOffHand();
                            if (!isFlashlight(held) && !isFlashlight(off)) {
                                turnOff(player);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanup(event.getEntity());
    }

    public void cleanUpAll() {
        for (UUID uuid : new ArrayList<>(activeFlashlights)) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                cleanup(p);
            }
        }
        for (Location loc : activeLights.values()) {
            restoreFakeBlock(loc.getWorld(), loc);
        }
        for (Location loc : activeAmbientLights.values()) {
            restoreFakeBlock(loc.getWorld(), loc);
        }
        activeLights.clear();
        activeAmbientLights.clear();
        activeFlashlights.clear();
    }

    private void toggleFlashlight(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeFlashlights.contains(uuid)) {
            turnOff(player);
        } else {
            turnOn(player);
        }
    }

    private void turnOn(Player player) {
        UUID uuid = player.getUniqueId();
        activeFlashlights.add(uuid);
        player.sendActionBar(Component.text("§fFlashlight: §a§lON"));
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 2.0f);
    }

    private void turnOff(Player player) {
        UUID uuid = player.getUniqueId();
        activeFlashlights.remove(uuid);
        player.sendActionBar(Component.text("§fFlashlight: §c§lOFF"));
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
        
        // Clean up focus light block
        Location prevLight = activeLights.remove(uuid);
        if (prevLight != null) {
            restoreFakeBlock(player.getWorld(), prevLight);
        }

        // Clean up ambient light block
        Location prevAmbient = activeAmbientLights.remove(uuid);
        if (prevAmbient != null) {
            restoreFakeBlock(player.getWorld(), prevAmbient);
        }
    }

    private void cleanup(Player player) {
        turnOff(player);
    }

    private boolean isFlashlight(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;
        if (!item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        return meta.hasCustomModelData() && meta.getCustomModelData() == FLASHLIGHT_CMD;
    }

    private void sendFakeBlock(org.bukkit.World world, Location loc, BlockData data) {
        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(loc) < 4096) { // within 64 blocks
                viewer.sendBlockChange(loc, data);
            }
        }
    }

    private void restoreFakeBlock(org.bukkit.World world, Location loc) {
        BlockData originalData = loc.getBlock().getBlockData();
        for (Player viewer : world.getPlayers()) {
            if (viewer.getLocation().distanceSquared(loc) < 4096) { // within 64 blocks
                viewer.sendBlockChange(loc, originalData);
            }
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(activeFlashlights)) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        activeFlashlights.remove(uuid);
                        continue;
                    }

                    // Verify they are still holding it
                    ItemStack main = player.getInventory().getItemInMainHand();
                    ItemStack off = player.getInventory().getItemInOffHand();
                    if (!isFlashlight(main) && !isFlashlight(off)) {
                        turnOff(player);
                        continue;
                    }

                    Location prevLight = activeLights.get(uuid);
                    Location prevAmbient = activeAmbientLights.get(uuid);

                    Location currentLoc = player.getEyeLocation();
                    Vector dir = currentLoc.getDirection().normalize();

                    Block prevAirBlock = null;
                    Location traceLoc = currentLoc.clone();

                    // 1. Raytrace up to 15 blocks to find the wall/ground or end of beam (Focus Light)
                    for (double d = 0; d < 15; d += 0.5) {
                        traceLoc.add(dir.clone().multiply(0.5));
                        Block block = traceLoc.getBlock();
                        if (block.getType().isSolid()) {
                            break;
                        }
                        prevAirBlock = block;
                    }

                    // 2. Place/Update Focus Light
                    if (prevAirBlock != null) {
                        Location newLightLoc = prevAirBlock.getLocation();

                        // Only update if location changed
                        if (prevLight == null || !prevLight.equals(newLightLoc)) {
                            // Restore previous light block
                            if (prevLight != null) {
                                restoreFakeBlock(player.getWorld(), prevLight);
                            }

                            // Send fake block change packet to nearby players (level 15)
                            Material blockType = prevAirBlock.getType();
                            if (blockType == Material.AIR || blockType == Material.CAVE_AIR) {
                                sendFakeBlock(player.getWorld(), newLightLoc, lightData15);
                                activeLights.put(uuid, newLightLoc);
                            } else {
                                activeLights.remove(uuid);
                            }
                        }

                        // Spawn subtle glowing dust at the impact point to simulate light beam hitting the wall
                        if (player.getWorld().getPlayers().size() > 0) {
                            player.getWorld().spawnParticle(Particle.GLOW, newLightLoc.clone().add(0.5, 0.5, 0.5), 1, 0.1, 0.1, 0.1, 0.0);
                        }
                    } else {
                        // Restore previous if we hit nothing
                        if (prevLight != null) {
                            restoreFakeBlock(player.getWorld(), prevLight);
                            activeLights.remove(uuid);
                        }
                    }

                    // 3. Place/Update Ambient Light (Spill Light around player, level 8)
                    Location newAmbientLoc = player.getEyeLocation().add(dir.clone().multiply(1.2)).getBlock().getLocation();
                    
                    // Do not place ambient light at the same location as focus light to prevent conflict
                    if (prevAirBlock != null && newAmbientLoc.equals(prevAirBlock.getLocation())) {
                        newAmbientLoc = null;
                    }

                    if (newAmbientLoc != null) {
                        if (prevAmbient == null || !prevAmbient.equals(newAmbientLoc)) {
                            // Restore previous ambient block
                            if (prevAmbient != null) {
                                restoreFakeBlock(player.getWorld(), prevAmbient);
                            }

                            Block ambientBlock = newAmbientLoc.getBlock();
                            Material ambientType = ambientBlock.getType();
                            if (ambientType == Material.AIR || ambientType == Material.CAVE_AIR) {
                                sendFakeBlock(player.getWorld(), newAmbientLoc, lightData8);
                                activeAmbientLights.put(uuid, newAmbientLoc);
                            } else {
                                activeAmbientLights.remove(uuid);
                            }
                        }
                    } else {
                        if (prevAmbient != null) {
                            restoreFakeBlock(player.getWorld(), prevAmbient);
                            activeAmbientLights.remove(uuid);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Run every single tick for smooth dynamic lighting!
    }
}
