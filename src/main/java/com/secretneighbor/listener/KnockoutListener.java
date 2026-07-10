package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.Role;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnockoutListener implements Listener {

    private final SecretNeighborPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeKnockoutTimers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeReviveChannels = new HashMap<>();

    public KnockoutListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMeleePvP(EntityDamageByEntityEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        
        // Cancel direct melee damage between players (allow projectiles/thrown items)
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player attacker) {
            if (!attacker.hasMetadata("sn_thrown_damage")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getEntity() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isAlive()) return;

        // If player is already knocked, we cancel general environmental damage
        if (snp.isKnocked()) {
            event.setCancelled(true);
            return;
        }

        // Check if this damage would kill the player (health <= damage)
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage <= 0) {
            event.setCancelled(true);
            enterKnockoutState(player, snp);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        SNPlayer victimSnp = plugin.getGameManager().getPlayers().get(victim.getUniqueId());
        if (victimSnp == null || !victimSnp.isChild() || !victimSnp.isAlive()) return;

        if (victimSnp.isKnocked()) {
            event.setCancelled(true);

            // Check if damager is the Neighbor
            Player attacker = null;
            if (event.getDamager() instanceof Player) {
                attacker = (Player) event.getDamager();
            } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }

            if (attacker != null) {
                SNPlayer attackerSnp = plugin.getGameManager().getPlayers().get(attacker.getUniqueId());
                if (attackerSnp != null && attackerSnp.isNeighbor()) {
                    // Eliminate downed child immediately!
                    eliminateKnockedPlayer(victim, victimSnp, "was executed by the Neighbor");
                    attacker.sendMessage("§c§l[Grab] §eYou eliminated §6" + victim.getName() + "§e!");
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
                }
            }
        }
    }

    private void enterKnockoutState(Player player, SNPlayer snp) {
        snp.setKnocked(true);
        player.sendBlockChange(player.getLocation().add(0, 1, 0), Bukkit.createBlockData(Material.BARRIER));
        if (plugin.getKnockoutPacketHandler() != null) {
            plugin.getKnockoutPacketHandler().addKnockedPlayer(player);
        }
        player.setHealth(20.0); // Full bar so they don't die instantly, we manage their timer
        
        // Visual/Audio cues
        player.sendTitle("§c§lKNOCKED DOWN!", "§eTeammates must sneak + Right-Click to revive!", 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.8f);

        // Apply severe slowness, blindness, weakness
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 5, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 99999, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 5, false, false));

        plugin.getGameManager().broadcast("§c§l☠ " + player.getName() + " §7has been knocked down!");

        // Start countdown timer (30 seconds)
        BukkitRunnable timer = new BukkitRunnable() {
            int timeRemaining = 30;

            @Override
            public void run() {
                if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME || !snp.isAlive() || !snp.isKnocked()) {
                    cancel();
                    activeKnockoutTimers.remove(player.getUniqueId());
                    return;
                }

                player.sendActionBar(Component.text("§c⌛ Time remaining to bleed out: §f" + timeRemaining + " seconds"));
                player.sendBlockChange(player.getLocation().add(0, 1, 0), Bukkit.createBlockData(Material.BARRIER));

                if (timeRemaining <= 0) {
                    eliminateKnockedPlayer(player, snp, "bled out");
                    cancel();
                    activeKnockoutTimers.remove(player.getUniqueId());
                }

                timeRemaining--;
            }
        };

        timer.runTaskTimer(plugin, 0L, 20L);
        activeKnockoutTimers.put(player.getUniqueId(), timer);
    }

    private void eliminateKnockedPlayer(Player player, SNPlayer snp, String reason) {
        snp.setKnocked(false);
        Location headLoc = player.getLocation().add(0, 1, 0);
        player.sendBlockChange(headLoc, headLoc.getBlock().getBlockData());
        if (plugin.getKnockoutPacketHandler() != null) {
            plugin.getKnockoutPacketHandler().removeKnockedPlayer(player);
        }
        snp.setAlive(false);

        // Clear active timer if any
        BukkitRunnable timer = activeKnockoutTimers.remove(player.getUniqueId());
        if (timer != null) timer.cancel();

        // Clear active revive channel if any
        cancelReviveForTarget(player.getUniqueId());

        // Set to spectator
        player.sendTitle("§c§l☠ ELIMINATED!", "§7You " + reason + "!", 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
        player.setGameMode(GameMode.SPECTATOR);

        // Drop non-ability, non-locked items on the ground
        Location dropLoc = player.getLocation();
        NamespacedKey abilityKey = new NamespacedKey(plugin, "sn_ability");
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                if (InventoryListener.isLockedPane(item)) continue;
                
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(abilityKey, PersistentDataType.STRING)) {
                    continue;
                }
                
                dropLoc.getWorld().dropItemNaturally(dropLoc, item);
            }
        }

        // Clear inventory completely
        if (snp.getChildClass() == com.secretneighbor.player.ChildClass.BAGGER) {
            if (plugin.getChildAbilitiesListener() != null) {
                plugin.getChildAbilitiesListener().dropBackpackItems(player);
            }
        }
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        plugin.getGameManager().broadcast("§c§l☠ " + player.getName() + " " + reason + "!");
        plugin.getGameManager().checkWinConditions();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player clicker = event.getPlayer();
        SNPlayer clickerSnp = plugin.getGameManager().getPlayers().get(clicker.getUniqueId());
        SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(target.getUniqueId());

        if (clickerSnp == null || targetSnp == null) return;

        // Clicker must be alive child, target must be knocked child
        if (!clickerSnp.isChild() || !clickerSnp.isAlive() || clickerSnp.isKnocked()) return;
        if (!targetSnp.isChild() || !targetSnp.isAlive() || !targetSnp.isKnocked()) return;

        // Must sneak to revive
        if (!clicker.isSneaking()) {
            clicker.sendActionBar(Component.text("§eHold SNEAK (Shift) and Right-Click to revive your teammate!"));
            return;
        }

        // Start reviving channel
        startReviveChannel(clicker, target, targetSnp);
    }

    private void startReviveChannel(Player clicker, Player target, SNPlayer targetSnp) {
        UUID clickerId = clicker.getUniqueId();
        if (activeReviveChannels.containsKey(clickerId)) return; // Already reviving someone

        clicker.sendMessage("§a§lReviving §e" + target.getName() + "... Stand still and keep crouching!");
        target.sendMessage("§a" + clicker.getName() + " §eisi reviving you... §7(5 seconds)");

        BukkitRunnable channel = new BukkitRunnable() {
            int ticks = 0;
            final int MAX_TICKS = 100; // 5 seconds (runs every 2 ticks)

            @Override
            public void run() {
                // Pre-checks: both players must be online and close
                if (!clicker.isOnline() || !target.isOnline()) {
                    cancelRevive(clickerId, "Teammate disconnected");
                    return;
                }

                SNPlayer cSnp = plugin.getGameManager().getPlayers().get(clickerId);
                if (cSnp == null || !cSnp.isAlive() || cSnp.isKnocked() || !targetSnp.isKnocked() || !targetSnp.isAlive()) {
                    cancelRevive(clickerId, "Revive cancelled");
                    return;
                }

                // Check distance (must be within 3 blocks)
                if (clicker.getLocation().distanceSquared(target.getLocation()) > 9.0) {
                    cancelRevive(clickerId, "Too far away!");
                    return;
                }

                // Must remain sneaking
                if (!clicker.isSneaking()) {
                    cancelRevive(clickerId, "You stopped sneaking!");
                    return;
                }

                // Visual effects
                target.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, target.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.02);

                ticks += 2;
                int percent = (ticks * 100) / MAX_TICKS;
                
                // Show progress bar
                StringBuilder progress = new StringBuilder("§a§lReviving: §f[");
                int filled = percent / 10;
                for (int i = 0; i < 10; i++) {
                    if (i < filled) progress.append("■");
                    else progress.append("□");
                }
                progress.append("] §a").append(percent).append("%");

                clicker.sendActionBar(Component.text(progress.toString()));
                target.sendActionBar(Component.text(progress.toString()));

                if (ticks >= MAX_TICKS) {
                    // Revive successful!
                    targetSnp.setKnocked(false);
                    Location headLoc = target.getLocation().add(0, 1, 0);
                    target.sendBlockChange(headLoc, headLoc.getBlock().getBlockData());
                    if (plugin.getKnockoutPacketHandler() != null) {
                        plugin.getKnockoutPacketHandler().removeKnockedPlayer(target);
                    }
                    target.setHealth(10.0); // 5 hearts

                    // Clear effects
                    target.removePotionEffect(PotionEffectType.SLOWNESS);
                    target.removePotionEffect(PotionEffectType.BLINDNESS);
                    target.removePotionEffect(PotionEffectType.WEAKNESS);

                    clicker.sendMessage("§a§l✔ §eSuccessfully revived §6" + target.getName() + "§e!");
                    target.sendTitle("§a§lREVIVED!", "§eYou were revived by §a" + clicker.getName() + "§e!", 10, 60, 10);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    clicker.playSound(clicker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

                    plugin.getGameManager().broadcast("§a§l✔ " + target.getName() + " §ewas revived by §a" + clicker.getName() + "§e!");

                    cancel();
                    activeReviveChannels.remove(clickerId);
                }
            }
        };

        channel.runTaskTimer(plugin, 0L, 2L);
        activeReviveChannels.put(clickerId, channel);
    }

    private void cancelRevive(UUID clickerId, String reason) {
        BukkitRunnable channel = activeReviveChannels.remove(clickerId);
        if (channel != null) {
            channel.cancel();
            Player p = Bukkit.getPlayer(clickerId);
            if (p != null) {
                p.sendMessage("§c§l✖ §cRevival cancelled: " + reason);
                p.sendActionBar(Component.text("§c✖ Revival Cancelled"));
            }
        }
    }

    private void cancelReviveForTarget(UUID targetId) {
        // Find clickers that are reviving this target and cancel them
        UUID toCancel = null;
        for (Map.Entry<UUID, BukkitRunnable> entry : activeReviveChannels.entrySet()) {
            // Check if this task is for this target
            // Simplest way is to cancel it and let it clean up
            toCancel = entry.getKey();
            break; 
        }
        if (toCancel != null) {
            cancelRevive(toCancel, "Target eliminated");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());

        if (snp != null && snp.isChild() && snp.isKnocked()) {
            // Prevent moving/jumping
            Location from = event.getFrom();
            Location to = event.getTo();
            
            // Allow looking around (pitch/yaw changes) but block X/Z coordinate changes
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
                Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());

        if (snp != null && snp.isChild() && snp.isKnocked()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());

        if (snp != null && snp.isChild() && snp.isKnocked()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp != null && snp.isChild() && snp.isKnocked()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Cancel active knockout bleed out timer
        BukkitRunnable timer = activeKnockoutTimers.remove(uuid);
        if (timer != null) {
            timer.cancel();
        }
        
        // Cancel if they were reviving someone
        cancelRevive(uuid, "Player disconnected");
        
        // Cancel if they were being revived
        cancelReviveForTarget(uuid);
    }
}
