package com.secretneighbor.furniture;

import com.secretneighbor.SecretNeighborPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class WardrobeManager {

    private static final int CMD_CLOSED = 1700;
    private static final int CMD_HALF = 1701;
    private static final int CMD_OPEN = 1702;
    private static final String WARDROBE_TAG = "sn_wardrobe";

    private final SecretNeighborPlugin plugin;

    // Maps player UUID -> wardrobe armor stand UUID
    private final Map<UUID, UUID> hidingPlayers = new HashMap<>();

    // Maps wardrobe armor stand UUID -> player UUID hiding inside
    private final Map<UUID, UUID> wardrobeHiders = new HashMap<>();

    public WardrobeManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new wardrobe armor stand and double barrier block structure.
     */
    public ArmorStand spawnWardrobe(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        
        Location spawnLoc = blockLoc.clone().add(0.5, 0.0, 0.5);
        float yaw = Math.round(location.getYaw() / 90.0f) * 90.0f;
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(0.0f);

        // Place two physical barrier blocks for collision (double height)
        blockLoc.getBlock().setType(Material.BARRIER);
        blockLoc.clone().add(0, 1, 0).getBlock().setType(Material.BARRIER);

        // Spawn the armor stand 1.020833 blocks lower
        Location standLoc = spawnLoc.clone().add(0, -1.020833, 0);

        return standLoc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(false);
            as.setArms(false);
            as.setBasePlate(false);
            as.setCanPickupItems(false);
            as.customName(Component.text("§7Wardrobe"));
            as.setCustomNameVisible(false);
            as.addScoreboardTag(WARDROBE_TAG);

            // Set closed model
            ItemStack helmet = createModelItem(CMD_CLOSED);
            as.getEquipment().setHelmet(helmet);
        });
    }

    /**
     * Handles player interaction with a wardrobe.
     */
    public void interactWardrobe(Player player, ArmorStand stand) {
        UUID standId = stand.getUniqueId();
        
        // Check if player is already hiding
        if (hidingPlayers.containsKey(player.getUniqueId())) {
            exitWardrobe(player);
            return;
        }

        // Check if anyone else is inside
        if (wardrobeHiders.containsKey(standId)) {
            // Check if player is Neighbor (can eject the hider!)
            var snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
            if (snp != null && snp.isNeighbor()) {
                ejectAndCaught(stand, player);
            } else {
                player.sendActionBar(Component.text("§cThis wardrobe is occupied!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            }
            return;
        }

        // If player is Neighbor and wardrobe is empty:
        var snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp != null && snp.isNeighbor()) {
            // Just animate opening it and closing
            openWardrobeDoorTemporarily(stand);
            player.sendMessage("§7This wardrobe is empty.");
            return;
        }

        // Otherwise, player is a Child -> Enter & Hide!
        enterWardrobe(player, stand);
    }

    /**
     * Child enters the wardrobe to hide.
     */
    private void enterWardrobe(Player player, ArmorStand stand) {
        UUID standId = stand.getUniqueId();

        // 1. Run door opening animation
        stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.8f);
        runAnimation(stand, CMD_CLOSED, CMD_HALF, CMD_OPEN, () -> {
            // 2. Hide player once open
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1, false, false));
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.hidePlayer(plugin, player);
            }

            // Teleport to wardrobe center (adjusting Y to match grid)
            Location center = stand.getLocation().clone().add(0, 1.020833, 0);
            player.teleport(center);

            hidingPlayers.put(player.getUniqueId(), standId);
            wardrobeHiders.put(standId, player.getUniqueId());

            player.sendTitle("§a§lHIDING", "§7Right-click or Sneak to exit", 10, 40, 10);
            player.sendMessage("§a✔ You are now hiding in the wardrobe!");

            // 3. Close the door behind them
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (stand.isValid() && wardrobeHiders.containsKey(standId)) {
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 0.8f);
                    runAnimation(stand, CMD_OPEN, CMD_HALF, CMD_CLOSED, null);
                }
            }, 10L);
        });
    }

    /**
     * Child exits the wardrobe safely.
     */
    public void exitWardrobe(Player player) {
        UUID standId = hidingPlayers.remove(player.getUniqueId());
        if (standId == null) return;
        wardrobeHiders.remove(standId);

        // Find wardrobe armor stand
        player.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                .filter(as -> as.getUniqueId().equals(standId))
                .findFirst()
                .ifPresent(stand -> {
                    // Open door
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.8f);
                    runAnimation(stand, CMD_CLOSED, CMD_HALF, CMD_OPEN, () -> {
                        // Restore visibility
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.showPlayer(plugin, player);
                        }

                        // Teleport in front of the wardrobe
                        float yaw = stand.getLocation().getYaw();
                        double rad = Math.toRadians(yaw);
                        double dx = -Math.sin(rad);
                        double dz = Math.cos(rad);
                        Location exitLoc = stand.getLocation().clone().add(dx * 1.0, 1.020833, dz * 1.0);
                        player.teleport(exitLoc);

                        player.sendMessage("§7Emerging from wardrobe.");

                        // Close door
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (stand.isValid() && !wardrobeHiders.containsKey(standId)) {
                                stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 0.8f);
                                runAnimation(stand, CMD_OPEN, CMD_HALF, CMD_CLOSED, null);
                            }
                        }, 10L);
                    });
                });
    }

    /**
     * Neighbor ejects and catches a Child inside.
     */
    private void ejectAndCaught(ArmorStand stand, Player neighbor) {
        UUID standId = stand.getUniqueId();
        UUID childId = wardrobeHiders.remove(standId);
        if (childId == null) return;
        hidingPlayers.remove(childId);

        Player child = Bukkit.getPlayer(childId);
        if (child == null) return;

        // Open door
        stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.8f);
        runAnimation(stand, CMD_CLOSED, CMD_HALF, CMD_OPEN, () -> {
            // Eject child
            child.removePotionEffect(PotionEffectType.INVISIBILITY);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, child);
            }

            // Teleport in front of the wardrobe (towards neighbor)
            float yaw = stand.getLocation().getYaw();
            double rad = Math.toRadians(yaw);
            double dx = -Math.sin(rad);
            double dz = Math.cos(rad);
            Location exitLoc = stand.getLocation().clone().add(dx * 1.2, 1.020833, dz * 1.2);
            child.teleport(exitLoc);

            // Jumpscare effects!
            child.playSound(child.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f);
            child.playSound(child.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.2f, 0.8f);
            neighbor.playSound(neighbor.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.8f, 1.2f);

            child.sendTitle("§c§lFOUND BY NEIGHBOR!", "§eYou were caught hiding!", 5, 50, 10);
            neighbor.sendTitle("§a§lCAUGHT THE CHILD!", "§7You found " + child.getName() + "!", 5, 50, 10);

            // Close door
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (stand.isValid()) {
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 0.8f);
                    runAnimation(stand, CMD_OPEN, CMD_HALF, CMD_CLOSED, null);
                }
            }, 30L);
        });
    }

    /**
     * Helper to animate empty wardrobe opening and closing.
     */
    private void openWardrobeDoorTemporarily(ArmorStand stand) {
        stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 0.8f);
        runAnimation(stand, CMD_CLOSED, CMD_HALF, CMD_OPEN, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (stand.isValid() && !wardrobeHiders.containsKey(stand.getUniqueId())) {
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_WOODEN_DOOR_CLOSE, 1f, 0.8f);
                    runAnimation(stand, CMD_OPEN, CMD_HALF, CMD_CLOSED, null);
                }
            }, 30L);
        });
    }

    private void runAnimation(ArmorStand stand, int from, int mid, int to, Runnable onComplete) {
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
                    stand.getEquipment().setHelmet(createModelItem(mid));
                } else if (step == 2) {
                    stand.getEquipment().setHelmet(createModelItem(to));
                    if (onComplete != null) onComplete.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public boolean isWardrobe(ArmorStand stand) {
        return stand.getScoreboardTags().contains(WARDROBE_TAG);
    }

    public boolean isPlayerHiding(UUID playerUuid) {
        return hidingPlayers.containsKey(playerUuid);
    }

    public void removeAll() {
        for (UUID pid : hidingPlayers.keySet()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.showPlayer(plugin, p);
                }
            }
        }
        hidingPlayers.clear();
        wardrobeHiders.clear();
    }

    private ItemStack createModelItem(int customModelData) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.displayName(Component.text("§7Wardrobe"));
            item.setItemMeta(meta);
        }
        return item;
    }
}
