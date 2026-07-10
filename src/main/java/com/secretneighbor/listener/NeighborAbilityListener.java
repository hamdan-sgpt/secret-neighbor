package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.ChildClass;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Neighbor Abilities based on the real Secret Neighbor game:
 *
 * HOTBAR ITEMS:
 *   Slot 0: §4 Bear Trap (1300) - Place hunting traps that snare children
 *   Slot 1: §6 Rage (1304) - Activate Rage Mode (after taking enough hits)
 *   Slot 2: §d Disguise Mask (1301) - Disguise as a random child player
 *
 * KEYBINDS:
 *   F (Swap Hand): Toggle Bapake/Kill mode (Hello Neighbor skin)
 *   Right-Click (empty hand, Bapake mode only): Grab children to capture them
 *
 * MECHANICS:
 *   - Bear Trap: Place on ground, snares children for 5s (they can escape by jumping)
 *   - Grab: Right-click (empty hand) in Bapake mode. Held for 3s = captured (eliminated).
 *     * Other children can rescue by hitting the Neighbor with throwables.
 *     * Brave with Counter-Strike charged (3 hits) auto-escapes.
 *     * Bagger slows Neighbor while grabbed (Heavy Bones).
 *   - Rage Mode: After being hit 7 times, Rage charges up. Activate for Speed III + 
 *     temporary invincibility for 5 seconds.
 *   - Disguise/Bapake: Same as before (skin switching).
 */
public class NeighborAbilityListener implements Listener {

    private final SecretNeighborPlugin plugin;
    private final List<ArmorStand> activeBearTraps = new ArrayList<>();
    private final Set<Location> openBookshelves = new HashSet<>();

    public NeighborAbilityListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
        startBearTrapCheckTask();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isNeighbor() || !snp.isAlive()) return;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        Action action = event.getAction();
        boolean isEmptyHand = (heldItem == null || heldItem.getType() == Material.AIR);

        // 1. Check if holding Rage item
        if (heldItem != null && heldItem.getType() == Material.FEATHER && heldItem.hasItemMeta()) {
            ItemMeta meta = heldItem.getItemMeta();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "sn_ability");
            boolean isRage = false;
            if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                if ("rage".equals(meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING))) {
                    isRage = true;
                }
            } else if (meta.hasCustomModelData() && meta.getCustomModelData() == 1304) {
                isRage = true; // Fallback
            }

            if (isRage && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);
                handleRage(player, snp);
                return;
            }
        }

        // 2. Check for empty hand keybinds (Sneaking + Left/Right click)
        if (player.isSneaking() && isEmptyHand) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                // Shift + Right Click = Bear Trap
                event.setCancelled(true);
                handleBearTrap(player, snp, event.getClickedBlock(), event.getBlockFace());
                return;
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                // Shift + Left Click = Disguise
                event.setCancelled(true);
                handleDisguiseMask(player, snp);
                return;
            }
        }

        // 3. Grab action (Right-click empty hand, NOT sneaking, in Bapake Mode)
        if (snp.isBapakeMode() && !player.isSneaking() && isEmptyHand) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                handleGrab(player, snp);
                return;
            }
        }
    }

    // =============================================
    // 1. BEAR TRAP - Place snare traps for children
    // =============================================
    private void handleBearTrap(Player player, SNPlayer snp, Block clickedBlock, BlockFace face) {
        if (snp.isOnCooldown("bear_trap")) {
            int remaining = snp.getRemainingCooldown("bear_trap");
            player.sendActionBar(Component.text("§cBear Trap on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Maximum 3 active traps
        if (activeBearTraps.size() >= 3) {
            ArmorStand oldest = activeBearTraps.remove(0);
            if (oldest != null && !oldest.isDead()) oldest.remove();
            player.sendMessage("§7Oldest bear trap removed (max 3).");
        }

        // Place trap at target location
        Location trapLoc;
        if (clickedBlock != null && face != null) {
            trapLoc = clickedBlock.getRelative(face).getLocation().add(0.5, 0, 0.5);
        } else {
            // Place at player's feet
            trapLoc = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        }

        // Spawn invisible armor stand as trap marker
        ArmorStand trap = trapLoc.getWorld().spawn(trapLoc.clone().subtract(0, 0.8, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setSmall(true);
            as.setCustomNameVisible(false);
            as.addScoreboardTag("sn_bear_trap");
            // Set iron trapdoor on head to visually represent the trap
            as.getEquipment().setHelmet(new ItemStack(Material.IRON_TRAPDOOR));
        });
        activeBearTraps.add(trap);

        player.sendMessage("§4§l[Bear Trap] §eTrap placed! §7(" + activeBearTraps.size() + "/3 active)");
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 0.6f);
        snp.setCooldown("bear_trap", 8);
    }

    private void startBearTrapCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
                    // Clean up traps when game ends
                    for (ArmorStand trap : activeBearTraps) {
                        if (trap != null && !trap.isDead()) trap.remove();
                    }
                    activeBearTraps.clear();
                    return;
                }

                Iterator<ArmorStand> it = activeBearTraps.iterator();
                while (it.hasNext()) {
                    ArmorStand trap = it.next();
                    if (trap == null || trap.isDead()) {
                        it.remove();
                        continue;
                    }

                    Location tLoc = trap.getLocation();
                    Location checkLoc = tLoc.clone().add(0, 0.8, 0);

                    // Ambient particles so trap is slightly visible
                    checkLoc.getWorld().spawnParticle(Particle.DUST, checkLoc.clone().add(0, 0.1, 0),
                            2, 0.15, 0.05, 0.15, new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 50, 10), 0.5f));

                    // Check if a child walks on it
                    for (Player p : checkLoc.getWorld().getPlayers()) {
                        SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
                        if (targetSnp == null || !targetSnp.isChild() || !targetSnp.isAlive()) continue;
                        if (targetSnp.isTrapped()) continue; // Already trapped

                        if (p.getLocation().distanceSquared(checkLoc) < 1.5) {
                            // TRIGGER TRAP!
                            triggerBearTrap(trap, p, targetSnp);
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L);
    }

    private void triggerBearTrap(ArmorStand trap, Player victim, SNPlayer victimSnp) {
        trap.remove();
        Location loc = victim.getLocation();

        // Trap the child
        victimSnp.setTrapped(true);
        victimSnp.setTrapTicks(100); // 5 seconds (100 ticks / 20 per second, but game loop ticks every 20L so 5 iterations)

        // Effects
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4)); // 5s immobilize
        victim.damage(4.0); // 2 hearts damage

        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.5f, 0.3f);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.5, 0), 25, 0.2, 0.3, 0.2, 0.1);

        victim.sendMessage("§4§l[Bear Trap] §cYou stepped on a Bear Trap! §7Jump repeatedly to escape faster!");

        // Alert Neighbor
        for (SNPlayer snp : plugin.getGameManager().getPlayers().values()) {
            if (snp.isNeighbor() && snp.isAlive()) {
                Player neighbor = Bukkit.getPlayer(snp.getUuid());
                if (neighbor != null) {
                    neighbor.sendMessage("§4§l[Bear Trap] §eA child has been trapped at §6" +
                            loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "§e!");
                    neighbor.playSound(neighbor.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
                }
            }
        }
    }

    // =============================================
    // 2. GRAB HAND - Grab children to capture them
    // =============================================
    private void handleGrab(Player player, SNPlayer snp) {
        // Grab only callable in Bapake mode (enforced by caller via right-click check)
        if (snp.isOnCooldown("grab")) {
            int remaining = snp.getRemainingCooldown("grab");
            player.sendActionBar(Component.text("§cGrab on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Find nearest child within 3.5 blocks and 55 degree cone
        Player targetChild = null;
        double nearestDist = Double.MAX_VALUE;
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        for (Player p : player.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) continue;

            SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
            if (targetSnp == null || !targetSnp.isChild() || !targetSnp.isAlive()) continue;
            if (targetSnp.isGrabbed()) continue;

            double dist = p.getLocation().distanceSquared(player.getLocation());
            if (dist < 12.25 && dist < nearestDist) { // 3.5 blocks
                Vector toTarget = p.getEyeLocation().toVector().subtract(eyeLoc.toVector()).normalize();
                double dot = direction.dot(toTarget);
                if (dot > 0.55) { // ~56 degree cone
                    nearestDist = dist;
                    targetChild = p;
                }
            }
        }

        if (targetChild == null) {
            // Lunge animation even on miss (grab air)
            player.setVelocity(direction.clone().multiply(0.4).setY(0.05));
            player.sendActionBar(Component.text("§7No child in reach to grab..."));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(direction.multiply(1.5)).add(0, 1, 0), 1);
            snp.setCooldown("grab", 1);
            return;
        }

        SNPlayer childSnp = plugin.getGameManager().getPlayers().get(targetChild.getUniqueId());

        // Check Counter-Strike (Brave escape)
        if (childSnp.getChildClass() == ChildClass.BRAVE && childSnp.getHitsTaken() >= 3) {
            childSnp.resetHitsTaken();
            // Lunge but miss — Brave breaks free
            player.setVelocity(direction.clone().multiply(0.5).setY(0.05));
            player.sendMessage("§c§l[Counter-Strike] §eBrave escaped your grab!");
            targetChild.sendMessage("§6§l⚡ COUNTER-STRIKE! §aYou broke free from the Neighbor's grab!");
            targetChild.playSound(targetChild.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            targetChild.playSound(targetChild.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            // Push Brave away
            Vector pushAway = targetChild.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.9).setY(0.35);
            targetChild.setVelocity(pushAway);
            targetChild.getWorld().spawnParticle(Particle.EXPLOSION, targetChild.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.02);
            snp.setCooldown("grab", 5);
            return;
        }

        // ===== GRAB SUCCESS! =====
        // Lunge toward the child
        Vector lungeDir = targetChild.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        player.setVelocity(lungeDir.clone().multiply(0.6).setY(0.1));

        // Grab sound & particle burst
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.7f);
        player.getWorld().playSound(targetChild.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1.2f, 1.5f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(lungeDir.multiply(1.2)).add(0, 1, 0), 3, 0.3, 0.3, 0.3);

        // Set grabbed state
        childSnp.setGrabbed(true);
        childSnp.setGrabProgress(0);
        childSnp.setGrabbedByUuid(player.getUniqueId());
        childSnp.setStruggleCount(0);
        childSnp.setGrabStartTime(System.currentTimeMillis());

        // Heavy Bones: Extra slowness if Bagger
        boolean isBagger = childSnp.getChildClass() == ChildClass.BAGGER;
        if (isBagger) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3)); // 4s Slowness IV
            player.sendMessage("§d§l[Heavy Bones] §eThis child is HEAVY! You can barely move!");
        }

        // Apply slowness to Neighbor while carrying
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1)); // Slowness II for carrying

        // Mount the child onto the Neighbor after a short delay (2 ticks for lunge to land)
        final Player fTarget = targetChild;
        final SNPlayer fChildSnp = childSnp;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!fChildSnp.isGrabbed() || !snp.isAlive()) return;

            Player neighbor = Bukkit.getPlayer(snp.getUuid());
            Player child = Bukkit.getPlayer(fChildSnp.getUuid());
            if (neighbor == null || child == null) {
                fChildSnp.setGrabbed(false);
                return;
            }

            // Apply effects to child: freeze them while grabbed
            child.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 99999, 4, false, false));
            child.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 4, false, false));

            // Mount child as passenger of Neighbor
            neighbor.addPassenger(child);

            // Messages
            neighbor.sendMessage("§c§l[GRAB!] §eGrabbed §6" + child.getName() + "§e! Hold for 3 seconds to capture!");
            child.sendMessage("§c§l[GRABBED!] §cThe Neighbor is holding you! Your teammates must throw items to save you!");
            child.sendTitle("§c§l✋ GRABBED!", "§7Spam JUMP to struggle! Friends must help!", 5, 60, 10);

            // Alert nearby players with directional hint
            for (SNPlayer otherSnp : plugin.getGameManager().getPlayers().values()) {
                if (otherSnp.isChild() && otherSnp.isAlive() && !otherSnp.getUuid().equals(fChildSnp.getUuid())) {
                    Player other = Bukkit.getPlayer(otherSnp.getUuid());
                    if (other != null) {
                        other.sendMessage("§c§l⚠ " + child.getName() + " §ewas grabbed by the Neighbor! Throw items to rescue them!");
                        other.playSound(other.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
                    }
                }
            }

            snp.setCooldown("grab", 6);

            // Start the capture countdown task
            startGrabCountdown(neighbor, child, snp, fChildSnp, isBagger);

        }, 3L); // 3 tick delay for lunge
    }

    /**
     * Runs the grab countdown timer with passenger-based holding.
     * The child rides on the Neighbor. Dramatic visual/audio effects play during the hold.
     * If timer completes, child is captured (eliminated).
     */
    private void startGrabCountdown(Player neighbor, Player child, SNPlayer neighborSnp, SNPlayer childSnp, boolean isBagger) {
        new BukkitRunnable() {
            int ticks = 0;
            final int CAPTURE_TICKS = 60; // 3 seconds at 2-tick intervals = 60 ticks
            int lastStruggleCount = 0;

            @Override
            public void run() {
                // === Pre-checks ===
                if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
                    releaseGrab(neighborSnp, childSnp, "game_end");
                    cancel();
                    return;
                }

                if (!childSnp.isGrabbed() || !childSnp.isAlive()) {
                    // Released by external event (rescue, dismount, etc)
                    cancel();
                    return;
                }

                if (!neighborSnp.isAlive() || !neighborSnp.isNeighbor()) {
                    releaseGrab(neighborSnp, childSnp, "neighbor_dead");
                    cancel();
                    return;
                }

                Player nb = Bukkit.getPlayer(neighborSnp.getUuid());
                Player ch = Bukkit.getPlayer(childSnp.getUuid());
                if (nb == null || ch == null) {
                    releaseGrab(neighborSnp, childSnp, "disconnect");
                    cancel();
                    return;
                }

                // === Struggle detection: if child jumped/spammed ===
                int currentStruggle = childSnp.getStruggleCount();
                boolean isStruggling = currentStruggle > lastStruggleCount;
                lastStruggleCount = currentStruggle;

                // === Visual effects — struggle/holding ===
                Location chestLoc = nb.getLocation().add(0, 1.8, 0);

                if (isStruggling) {
                    // Intense struggle particles
                    nb.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, chestLoc, 3, 0.4, 0.3, 0.4, 0.02);
                    nb.getWorld().spawnParticle(Particle.CRIT, chestLoc, 5, 0.3, 0.4, 0.3, 0.1);
                    nb.getWorld().playSound(chestLoc, Sound.ENTITY_PLAYER_HURT, 0.4f, 1.5f);
                } else {
                    // Passive held particles (smoke around child)
                    nb.getWorld().spawnParticle(Particle.SMOKE, chestLoc, 2, 0.2, 0.3, 0.2, 0.01);
                }

                // Periodic struggle sounds from child
                if (ticks % 10 == 0) {
                    nb.getWorld().playSound(chestLoc, Sound.ENTITY_VILLAGER_HURT, 0.6f, 1.8f);
                }

                // Red dust particles spiral around (capture closing in)
                double angle = (ticks * 12) * Math.PI / 180.0;
                double radius = 0.6;
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;
                nb.getWorld().spawnParticle(Particle.DUST, chestLoc.clone().add(px, -0.5, pz),
                        1, 0, 0, 0, new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 20, 20), 1.0f));

                // === Progress tracking ===
                ticks += 2;
                childSnp.incrementGrabProgress();

                // Progressive vision constriction using custom font vignettes (no potion effects)
                String vignetteChar;
                if (ticks < 12) {
                    vignetteChar = "\uE001";
                } else if (ticks < 24) {
                    vignetteChar = "\uE002";
                } else if (ticks < 36) {
                    vignetteChar = "\uE003";
                } else if (ticks < 48) {
                    vignetteChar = "\uE004";
                } else {
                    vignetteChar = "\uE005";
                }
                ch.sendTitle(vignetteChar, "", 0, 5, 0);

                int percent = (ticks * 100) / CAPTURE_TICKS;
                percent = Math.min(percent, 100);

                // Progress bar
                StringBuilder progressBar = new StringBuilder();
                int filled = percent / 10;
                for (int i = 0; i < 10; i++) {
                    if (i < filled) progressBar.append("§c■");
                    else progressBar.append("§8□");
                }

                ch.sendActionBar(Component.text("§c☠ BEING CAPTURED: " + progressBar + " §f" + percent + "% §7— Spam JUMP to struggle!"));
                nb.sendActionBar(Component.text("§e✋ HOLDING: " + progressBar + " §f" + percent + "% §7— Keep holding!"));

                // === Capture complete ===
                if (ticks >= CAPTURE_TICKS) {
                    // Dismount child before eliminating
                    nb.removePassenger(ch);

                    // Clear grab effects
                    ch.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                    ch.removePotionEffect(PotionEffectType.WEAKNESS);
                    ch.clearTitle();

                    childSnp.setGrabbed(false);
                    childSnp.setGrabbedByUuid(null);
                    childSnp.setAlive(false);

                    // === DRAMATIC CAPTURE ANIMATION ===
                    Location captureLoc = ch.getLocation();

                    // Dark ink spiral
                    for (int i = 0; i < 3; i++) {
                        double a = i * 120 * Math.PI / 180.0;
                        double r = 0.8;
                        captureLoc.getWorld().spawnParticle(Particle.SQUID_INK,
                                captureLoc.clone().add(Math.cos(a) * r, 1.0, Math.sin(a) * r),
                                15, 0.1, 0.3, 0.1, 0.05);
                    }
                    captureLoc.getWorld().spawnParticle(Particle.EXPLOSION, captureLoc.clone().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
                    captureLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, captureLoc.clone().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.03);

                    // Sounds
                    captureLoc.getWorld().playSound(captureLoc, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                    captureLoc.getWorld().playSound(captureLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
                    captureLoc.getWorld().playSound(captureLoc, Sound.ENTITY_EVOKER_CELEBRATE, 0.8f, 0.6f);

                    ch.sendTitle("§c§l☠ CAPTURED!", "§7You have been caught by the Neighbor!", 10, 80, 20);
                    ch.playSound(ch.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
                    ch.setGameMode(org.bukkit.GameMode.SPECTATOR);

                    nb.sendMessage("§c§l[Captured] §a" + ch.getName() + " has been captured!");
                    nb.playSound(nb.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);

                    // Remove neighbor slowness from carrying
                    nb.removePotionEffect(PotionEffectType.SLOWNESS);

                    // Broadcast
                    plugin.getGameManager().broadcast("§c§l☠ " + ch.getName() + " §7was captured by the Neighbor!");
                    plugin.getGameManager().checkWinConditions();

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Release a grabbed child — dismount from passenger, push away, clear effects.
     * Called when grab is interrupted (rescue, neighbor death, game end, etc).
     */
    public void releaseGrab(SNPlayer neighborSnp, SNPlayer childSnp, String reason) {
        if (!childSnp.isGrabbed()) return;

        childSnp.setGrabbed(false);
        childSnp.setGrabbedByUuid(null);

        Player neighbor = Bukkit.getPlayer(neighborSnp.getUuid());
        Player child = Bukkit.getPlayer(childSnp.getUuid());

        if (child != null) {
            // Dismount
            if (neighbor != null) {
                neighbor.removePassenger(child);
            }

            // Clear grab effects
            child.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            child.removePotionEffect(PotionEffectType.WEAKNESS);
            child.clearTitle();

            if (!"game_end".equals(reason) && !"disconnect".equals(reason)) {
                // Release animation — throw child away
                Vector pushAway;
                if (neighbor != null) {
                    pushAway = child.getLocation().toVector().subtract(neighbor.getLocation().toVector()).normalize().multiply(1.0).setY(0.4);
                } else {
                    pushAway = new Vector(0, 0.4, 0);
                }
                child.setVelocity(pushAway);

                // Release effects
                Location releaseLoc = child.getLocation();
                releaseLoc.getWorld().spawnParticle(Particle.CLOUD, releaseLoc.add(0, 1, 0), 15, 0.4, 0.3, 0.4, 0.08);
                releaseLoc.getWorld().spawnParticle(Particle.EXPLOSION, releaseLoc, 2, 0.2, 0.2, 0.2, 0.01);
                releaseLoc.getWorld().playSound(releaseLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 1.0f);

                child.sendMessage("§a§l[ESCAPED!] §eYou broke free from the Neighbor!");
                child.sendTitle("§a§lFREE!", "§eYou escaped!", 5, 30, 10);
                child.playSound(child.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            }
        }

        if (neighbor != null) {
            neighbor.removePotionEffect(PotionEffectType.SLOWNESS);
            if (!"game_end".equals(reason) && !"disconnect".equals(reason)) {
                neighbor.sendMessage("§c§l[Grab] §eThe child escaped!");
                neighbor.playSound(neighbor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            }
        }
    }

    /**
     * Handle EntityDismountEvent — prevent child from dismounting by themselves during grab.
     * If force-dismount happens, cancel the grab properly.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player child)) return;
        if (!(event.getDismounted() instanceof Player neighbor)) return;
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        SNPlayer childSnp = plugin.getGameManager().getPlayers().get(child.getUniqueId());
        if (childSnp == null || !childSnp.isGrabbed()) return;

        // Prevent voluntary dismount — the child can't escape just by pressing shift
        event.setCancelled(true);
    }

    /**
     * Handle struggle: when grabbed child tries to interact (any click), increment struggle counter.
     * Struggle doesn't free them directly but adds visual feedback and extends time slightly.
     */
    @EventHandler
    public void onGrabbedChildInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isGrabbed()) return;

        // Child is grabbed — any click counts as struggle
        event.setCancelled(true);
        snp.incrementStruggleCount();

        // Visual feedback for struggling
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.5, 0), 3, 0.3, 0.2, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.3f, 2.0f);
    }

    // Also handle throwable hits rescuing grabbed children
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (!(event.getDamager() instanceof org.bukkit.entity.Projectile projectile)) return;
        if (!(projectile.getShooter() instanceof Player shooter)) return;

        SNPlayer damagedSnp = plugin.getGameManager().getPlayers().get(damaged.getUniqueId());
        SNPlayer shooterSnp = plugin.getGameManager().getPlayers().get(shooter.getUniqueId());
        if (damagedSnp == null || shooterSnp == null) return;

        // If a child hits the Neighbor with a projectile, increment rage counter
        if (damagedSnp.isNeighbor() && shooterSnp.isChild()) {
            damagedSnp.incrementHitsTaken();
            int hits = damagedSnp.getHitsTaken();
            plugin.getGameManager().updateNeighborRageItem(damaged, hits);

            if (hits >= 7) {
                damaged.sendMessage("§6§l[Rage] §c⚡ RAGE MODE READY! §eUse Rage item to activate!");
                damaged.playSound(damaged.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
            }

            // Check if Neighbor is holding a grabbed child - force release (rescue!)
            for (SNPlayer childSnp : plugin.getGameManager().getPlayers().values()) {
                if (childSnp.isChild() && childSnp.isGrabbed() && childSnp.isAlive()) {
                    // Use releaseGrab to properly dismount passenger + clear effects
                    releaseGrab(damagedSnp, childSnp, "rescued");

                    Player child = Bukkit.getPlayer(childSnp.getUuid());
                    if (child != null) {
                        child.sendMessage("§a§l[RESCUED!] §eA teammate saved you from the Neighbor!");
                        child.playSound(child.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    }
                    damaged.sendMessage("§c§l[Grab] §eThe child was rescued! You dropped them!");
                    shooter.sendMessage("§a§l✔ §eYou rescued §6" + childSnp.getName() + "§e from the Neighbor!");
                    break;
                }
            }
        }
    }

    // =============================================
    // 3. RAGE MODE - Temporary invincibility + speed
    // =============================================
    private void handleRage(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("rage")) {
            int remaining = snp.getRemainingCooldown("rage");
            player.sendActionBar(Component.text("§cRage on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        int hits = snp.getHitsTaken();
        if (hits < 7) {
            player.sendMessage("§6§l[Rage] §cNot enough charge! §7(" + hits + "/7 hits taken)");
            player.sendActionBar(Component.text("§cRage charge: " + hits + "/7"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Activate Rage Mode!
        snp.resetHitsTaken();
        plugin.getGameManager().updateNeighborRageItem(player, 0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2)); // 5s Speed III
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4)); // 5s invincibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1)); // 5s Strength II
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2)); // 5s Regen III

        // Visual effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);

        player.sendMessage("§6§l[RAGE MODE] §c§lACTIVATED! §e5 seconds of unstoppable power!");
        player.sendTitle("§6§l⚡ RAGE MODE! ⚡", "§cUnstoppable for 5 seconds!", 5, 60, 10);

        // Alert children
        for (SNPlayer childSnp : plugin.getGameManager().getPlayers().values()) {
            if (childSnp.isChild() && childSnp.isAlive()) {
                Player child = Bukkit.getPlayer(childSnp.getUuid());
                if (child != null) {
                    child.sendMessage("§c§l⚠ WARNING: §cThe Neighbor entered Rage Mode! §7RUN!");
                    child.playSound(child.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.8f);
                }
            }
        }

        // Rage particle trail for 5 seconds
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 100 || !snp.isAlive()) {
                    cancel();
                    return;
                }
                Player p = Bukkit.getPlayer(snp.getUuid());
                if (p != null) {
                    p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
                    p.getWorld().spawnParticle(Particle.LAVA, p.getLocation().add(0, 0.5, 0), 1, 0.2, 0.2, 0.2, 0);
                }
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);

        snp.setCooldown("rage", 30); // 30s cooldown after rage ends
    }

    // =============================================
    // 4. DISGUISE MASK - Disguise as random child
    // =============================================
    private void handleDisguiseMask(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("mask_toggle")) {
            int remaining = snp.getRemainingCooldown("mask_toggle");
            player.sendActionBar(Component.text("§cPlease wait " + remaining + "s before toggling!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }
        snp.setCooldown("mask_toggle", 3);

        if (snp.isBapakeMode()) {
            player.sendMessage("§cYou cannot toggle disguise while in Bapake Mode! Exit Bapake Mode first.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        if (snp.isDisguised()) {
            // Return to self
            snp.setDisguised(false);
            snp.setDisguiseValue(null);
            snp.setDisguiseSignature(null);
            snp.setDisguiseName(null);

            if (snp.getOriginalSkinValue() != null && snp.getOriginalSkinSignature() != null) {
                plugin.getGameManager().setPlayerSkin(player, snp.getOriginalSkinValue(), snp.getOriginalSkinSignature());
            }
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));

            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1.2f);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.02);

            plugin.getGameManager().updateNeighborInventoryVisuals(player, snp);
            player.sendMessage("§a§l[Disguise] §eYou returned to your original self skin!");
        } else {
            // Find random child to copy
            List<Player> children = new ArrayList<>();
            for (SNPlayer other : plugin.getGameManager().getPlayers().values()) {
                if (other.isChild() && other.isAlive()) {
                    Player cp = Bukkit.getPlayer(other.getUuid());
                    if (cp != null) children.add(cp);
                }
            }

            if (children.isEmpty()) {
                player.sendMessage("§cNo active child players found to disguise as!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            Player targetChild = children.get(new Random().nextInt(children.size()));
            String val = null;
            String sig = null;
            for (com.destroystokyo.paper.profile.ProfileProperty prop : targetChild.getPlayerProfile().getProperties()) {
                if (prop.getName().equals("textures")) {
                    val = prop.getValue();
                    sig = prop.getSignature();
                    break;
                }
            }

            if (val != null && sig != null) {
                snp.setDisguiseValue(val);
                snp.setDisguiseSignature(sig);
                snp.setDisguiseName(targetChild.getName());
                snp.setDisguised(true);

                plugin.getGameManager().setPlayerSkin(player, val, sig);
                player.displayName(Component.text("§a" + targetChild.getName()));
                player.playerListName(Component.text("§a" + targetChild.getName()));

                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.02);

                plugin.getGameManager().updateNeighborInventoryVisuals(player, snp);
                player.sendMessage("§a§l[Disguise] §eYou disguised as §6" + targetChild.getName() + "§e!");
            } else {
                player.sendMessage("§cFailed to extract skin from " + targetChild.getName() + "!");
            }
        }
    }

    public void toggleBapakeMode(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("mask_toggle")) {
            int remaining = snp.getRemainingCooldown("mask_toggle");
            player.sendActionBar(Component.text("§cSilakan tunggu " + remaining + "s sebelum mengganti mode!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }
        snp.setCooldown("mask_toggle", 3);

        if (snp.isBapakeMode()) {
            // Exit Bapake Mode
            snp.setBapakeMode(false);
            player.getInventory().setItem(0, null);

            if (snp.isDisguised() && snp.getDisguiseValue() != null && snp.getDisguiseSignature() != null) {
                plugin.getGameManager().setPlayerSkin(player, snp.getDisguiseValue(), snp.getDisguiseSignature());
                player.displayName(Component.text("§a" + snp.getDisguiseName()));
                player.playerListName(Component.text("§a" + snp.getDisguiseName()));
                player.sendMessage("§a§l[Bapake Mode] §eExited. Returned to disguise: §6" + snp.getDisguiseName());
            } else {
                if (snp.getOriginalSkinValue() != null && snp.getOriginalSkinSignature() != null) {
                    plugin.getGameManager().setPlayerSkin(player, snp.getOriginalSkinValue(), snp.getOriginalSkinSignature());
                }
                player.displayName(Component.text(player.getName()));
                player.playerListName(Component.text(player.getName()));
                player.sendMessage("§a§l[Bapake Mode] §eExited. Returned to original self!");
            }

            // Restore scale
            org.bukkit.attribute.AttributeInstance scaleInstance = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
            if (scaleInstance != null) {
                scaleInstance.setBaseValue(1.0);
            }

            // Lock inventory slots again (disguised as kid)
            com.secretneighbor.listener.InventoryListener.lockPlayerInventorySlots(player, snp);

            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.02);

        } else {
            // Enter Bapake Mode (Reveal!)
            snp.setBapakeMode(true);
            player.getInventory().setItem(0, plugin.getGameManager().createNeighborAbilityItem("§6⚡ Rage (0/7)", 1304, "rage"));

            plugin.getGameManager().setPlayerSkin(player,
                     com.secretneighbor.game.GameManager.NEIGHBOR_SKIN_VALUE,
                     com.secretneighbor.game.GameManager.NEIGHBOR_SKIN_SIGNATURE);

            player.displayName(Component.text("§c§lNEIGHBOR"));
            player.playerListName(Component.text("§c§lNEIGHBOR"));

            // Set scale larger
            org.bukkit.attribute.AttributeInstance scaleInstance = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
            if (scaleInstance != null) {
                scaleInstance.setBaseValue(1.3);
            }

            // Clear locked glass panes
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (com.secretneighbor.listener.InventoryListener.isLockedPane(item)) {
                    player.getInventory().setItem(i, null);
                }
            }

            player.sendMessage("§c§l[Bapake Mode] §eKill Mode aktif! Right-click untuk grab anak!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.6f);
            player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        }

        plugin.getGameManager().updateNeighborInventoryVisuals(player, snp);
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getGameManager().getState() == com.secretneighbor.game.GameState.IN_GAME) {
            SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
            if (snp != null && snp.isNeighbor()) {
                event.getDrops().clear(); // Don't drop Neighbor custom items
                event.setDroppedExp(0);
                event.deathMessage(null); // Hide default death message

                Location deathLoc = player.getLocation();
                // Dissolve shadow particles
                deathLoc.getWorld().spawnParticle(Particle.SQUID_INK, deathLoc.clone().add(0, 1, 0), 80, 0.4, 0.6, 0.4, 0.05);
                deathLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, deathLoc.clone().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.02);
                
                // Creepy dissolve sounds
                deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_EVOKER_DEATH, 1.2f, 0.6f);
                deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);

                // Broadcast
                plugin.getGameManager().broadcast("§c§l☠ Neighbor §7was knocked out! They will respawn in their Security Room.");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() == com.secretneighbor.game.GameState.IN_GAME) {
            SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
            if (snp != null && snp.isNeighbor()) {
                String worldName = plugin.getConfig().getString("world-name", "secret_neighbor_map_2");
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location respawnLoc = new Location(world, -37.530, -51.93750, 12.440, 0.9f, 0.1f);
                    event.setRespawnLocation(respawnLoc);

                    // Re-equip and start reassembling cooldown (5 seconds)
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline() || plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

                        plugin.getGameManager().setupNeighbor(player, snp);

                        // Cooldown countdown task
                        new org.bukkit.scheduler.BukkitRunnable() {
                            int secondsLeft = 5;

                            @Override
                            public void run() {
                                if (!player.isOnline() || plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
                                    cancel();
                                    return;
                                }

                                if (secondsLeft <= 0) {
                                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                                    player.sendTitle("§a§l✔ READY!", "§eGo catch the children!", 5, 20, 5);
                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                                    cancel();
                                    return;
                                }

                                // Re-apply slowness/blindness each second
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 25, 9, false, false));
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 25, 0, false, false));

                                player.sendTitle("§c§l☠ DOWNED!", "§7Reassembling in §e" + secondsLeft + "s", 0, 22, 0);
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 0.5f);
                                secondsLeft--;
                            }
                        }.runTaskTimer(plugin, 0L, 20L);

                    }, 2L);
                }
            }
        }
    }

    @EventHandler
    public void onNeighborInteractBookshelf(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.BOOKSHELF) return;

        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isNeighbor() || !snp.isAlive()) return;

        // Only the Neighbor can interact with and open secret bookshelf doors!
        event.setCancelled(true);

        Location loc1 = clicked.getLocation();
        if (openBookshelves.contains(loc1)) return; // Already open

        // Find all connected bookshelf blocks (BFS within a 3.16 blocks radius, max 12 blocks)
        List<Block> blocksToOpen = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        queue.add(clicked);
        visited.add(clicked);

        while (!queue.isEmpty() && blocksToOpen.size() < 12) {
            Block current = queue.poll();
            blocksToOpen.add(current);

            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block neighborBlock = current.getRelative(face);
                if (neighborBlock.getType() == Material.BOOKSHELF && !visited.contains(neighborBlock)) {
                    if (neighborBlock.getLocation().distanceSquared(loc1) <= 10) {
                        visited.add(neighborBlock);
                        queue.add(neighborBlock);
                    }
                }
            }
        }

        // Mark all as open
        for (Block b : blocksToOpen) {
            openBookshelves.add(b.getLocation());
        }

        // Play secret door mechanism sounds
        loc1.getWorld().playSound(loc1, Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1.2f, 0.6f);
        loc1.getWorld().playSound(loc1, Sound.BLOCK_BARREL_OPEN, 1.0f, 0.8f);
        loc1.getWorld().spawnParticle(Particle.BLOCK, loc1.clone().add(0.5, 1.0, 0.5), 40, 0.5, 0.8, 0.5, 0.05, Material.BOOKSHELF.createBlockData());

        // Temporarily set to AIR
        for (Block b : blocksToOpen) {
            b.setType(Material.AIR);
        }

        player.sendMessage("§a§l[Shortcut] §eOpened secret bookshelf door!");

        // Schedule closing after 3 seconds (60 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Restore blocks
            for (Block b : blocksToOpen) {
                Location bLoc = b.getLocation();
                openBookshelves.remove(bLoc);

                // Safely push any entities standing inside the bookshelf blocks to prevent getting stuck
                bLoc.getWorld().getNearbyEntities(bLoc.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5).forEach(entity -> {
                    if (entity instanceof Player p) {
                        Vector vel = p.getLocation().getDirection().normalize().multiply(0.6).setY(0.1);
                        p.setVelocity(vel);
                    }
                });

                b.setType(Material.BOOKSHELF);
            }
            // Play close sound
            loc1.getWorld().playSound(loc1, Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1.2f, 0.6f);
            loc1.getWorld().playSound(loc1, Sound.BLOCK_BARREL_CLOSE, 1.0f, 0.8f);
        }, 60L);
    }

    @EventHandler
    public void onPlayerItemHeld(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp != null && snp.isNeighbor()) {
            plugin.getGameManager().updateNeighborInventoryVisuals(player, snp);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp != null && snp.isNeighbor()) {
            plugin.getGameManager().updateNeighborInventoryVisuals(player, snp);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        event.setCancelled(true); // Prevent item swap

        if (snp.isChild()) {
            if (plugin.getChildAbilitiesListener() != null) {
                plugin.getChildAbilitiesListener().triggerActiveAbility(player, snp);
            }
        } else if (snp.isNeighbor()) {
            if (snp.isBapakeMode()) {
                toggleBapakeMode(player, snp);
            } else {
                if (player.isSneaking()) {
                    toggleBapakeMode(player, snp);
                } else {
                    if (plugin.getChildAbilitiesListener() != null) {
                        plugin.getChildAbilitiesListener().triggerActiveAbility(player, snp);
                    }
                }
            }
        }
    }
}
