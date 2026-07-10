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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Child class abilities based on the real Secret Neighbor game:
 *
 * DETECTIVE (1201) - Field Intel: Scan to find nearest key in a drawer (particle trail + distance).
 *                    Intuition (passive): After 2 keys inserted, remaining keys emit particles.
 *
 * BAGGER (1202)    - Backpack: Open portable 9-slot inventory GUI.
 *                    Heavy Bones (passive): Neighbor gets Slowness when grabbing Bagger.
 *                    Strong Knees (passive): Permanent Resistance I (fall damage reduction).
 *
 * LEADER (1203)    - Inspiration: Speed boost nearby children (10m) + Blind Neighbor if within 4m.
 *                    Stress Resistance (passive): Reduced stun duration.
 *
 * INVENTOR (1204)  - Metal Detector: Scan for nearest key/gear through walls (particle trail).
 *                    Tinkering: Can detect items behind barriers.
 *
 * SCOUT (1205)     - Slingshot: Shoot nut projectile that stuns Neighbor on hit.
 *
 * ENGINEER (1206)  - Tripwire Sensor: Place proximity sensors that alert team when Neighbor walks near.
 *                    Custom class (not in original game).
 *
 * GAMER (1207)     - Decoy Hologram: Spawn a decoy armor stand at target location with sounds.
 *                    Quick Reflexes (passive): Permanent Speed I.
 *                    Custom class (not in original game).
 */
public class ChildAbilitiesListener implements Listener {

    private final SecretNeighborPlugin plugin;
    private final Map<UUID, Inventory> backpacks = new HashMap<>();
    private final List<Location> engineerTraps = new ArrayList<>();
    private final Map<UUID, ArmorStand> gamerDecoys = new HashMap<>();

    public ChildAbilitiesListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
        startEngineerTrapTask();
        startDetectiveIntuitionTask();
    }

    public void dropBackpackItems(Player player) {
        // No-op: Bagger no longer has a backpack
    }

    // =============================================
    // 1. DETECTIVE - Field Intel (find nearest key)
    // =============================================
    private void handleDetective(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("detective")) {
            int remaining = snp.getRemainingCooldown("detective");
            player.sendActionBar(Component.text("§cField Intel on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        // Find nearest drawer containing a key
        Location playerLoc = player.getLocation();
        ArmorStand nearestKeyDrawer = null;
        double nearestDist = Double.MAX_VALUE;

        if (plugin.getKeyManager() != null) {
            for (ArmorStand stand : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
                if (stand.getScoreboardTags().contains("sn_drawer")) {
                    // Check if this drawer has a key inside
                    Inventory drawerInv = plugin.getKeyManager().getDrawerInventory(stand);
                    if (drawerInv != null && containsKey(drawerInv)) {
                        double dist = stand.getLocation().distance(playerLoc);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestKeyDrawer = stand;
                        }
                    }
                }
            }
        }

        if (nearestKeyDrawer != null && nearestDist <= 40.0) {
            // Draw particle trail toward the key drawer
            Location from = player.getEyeLocation();
            Location to = nearestKeyDrawer.getLocation().add(0, 1, 0);
            Vector dir = to.toVector().subtract(from.toVector());
            int steps = Math.min(20, (int)(nearestDist * 2));
            for (int i = 0; i < steps; i++) {
                Location pLoc = from.clone().add(dir.clone().multiply((double) i / steps));
                 player.spawnParticle(Particle.HAPPY_VILLAGER, pLoc, 3, 0.05, 0.05, 0.05, 0.01);
            }

            // Compass direction
            String compassDir = getCompassDirection(playerLoc, nearestKeyDrawer.getLocation());
            player.sendMessage("§6§l[Field Intel] §e📸 Key detected §a" + Math.round(nearestDist) + "m " + compassDir + "§e! Follow the green trail.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.8f);

            snp.setCooldown("detective", 15);
        } else {
            player.sendMessage("§6§l[Field Intel] §7No keys detected nearby...");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            snp.setCooldown("detective", 5);
        }
    }

    private boolean containsKey(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.FEATHER && item.hasItemMeta()) {
                ItemMeta m = item.getItemMeta();
                if (m.hasCustomModelData()) {
                    int cmd = m.getCustomModelData();
                    if (cmd >= 1001 && cmd <= 1010) return true; // Key model data range
                }
            }
        }
        return false;
    }

    private String getCompassDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;

        if (angle >= 337.5 || angle < 22.5) return "§f(South)";
        if (angle < 67.5) return "§f(South-West)";
        if (angle < 112.5) return "§f(West)";
        if (angle < 157.5) return "§f(North-West)";
        if (angle < 202.5) return "§f(North)";
        if (angle < 247.5) return "§f(North-East)";
        if (angle < 292.5) return "§f(East)";
        return "§f(South-East)";
    }

    // Detective Intuition passive: after 2 keys inserted, remaining keys glow
    private void startDetectiveIntuitionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
                if (plugin.getKeyManager() == null) return;

                // Check if at least 2 keys have been inserted
                int keysInserted = plugin.getKeyManager().getKeysInserted();
                if (keysInserted < 2) return;

                // Find Detective players
                for (SNPlayer snp : plugin.getGameManager().getPlayers().values()) {
                    if (snp.isBapakeMode() || !snp.isAlive() || snp.getChildClass() != ChildClass.DETECTIVE) continue;
                    Player detective = Bukkit.getPlayer(snp.getUuid());
                    if (detective == null) continue;

                    // Make remaining key drawers glow for the Detective
                    for (ArmorStand stand : detective.getWorld().getEntitiesByClass(ArmorStand.class)) {
                        if (!stand.getScoreboardTags().contains("sn_drawer")) continue;
                        Inventory drawerInv = plugin.getKeyManager().getDrawerInventory(stand);
                        if (drawerInv != null && containsKey(drawerInv)) {
                            Location glow = stand.getLocation().add(0, 1.5, 0);
                            if (glow.distanceSquared(detective.getLocation()) < 900) { // 30 blocks
                                detective.spawnParticle(Particle.END_ROD, glow, 3, 0.2, 0.3, 0.2, 0.01);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 40L); // Every 2 seconds
    }

    // =============================================
    // 2. BAGGER - Backpack (portable inventory)
    // =============================================
    private void handleBagger(Player player, SNPlayer snp) {
        player.sendMessage("§e§l[Bagger] §fKelebihan Anda adalah pasif membawa 3 item di hotbar, tidak ada skill aktif!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.0f);
    }

    // =============================================
    // 3. LEADER - Inspiration (speed + blind)
    // =============================================
    private void handleLeader(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("leader")) {
            int remaining = snp.getRemainingCooldown("leader");
            player.sendActionBar(Component.text("§cInspiration on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 1.2f, 1.4f);

        // Speed boost to all children within 10 blocks
        int boosted = 0;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(player.getLocation()) > 100.0) continue; // 10m radius

            SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
            if (targetSnp == null) continue;

            if (targetSnp.isChild() && targetSnp.isAlive()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1)); // 5s Speed II
                p.spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
                p.sendMessage("§e§l[Inspiration] §aSpeed boost from the Leader!");
                boosted++;
            }

            // Blind the Neighbor if within 4 blocks (Team Work ability)
            if (targetSnp.isNeighbor() && targetSnp.isAlive() && !p.getUniqueId().equals(player.getUniqueId())) {
                if (p.getLocation().distanceSquared(player.getLocation()) < 16.0) { // 4 blocks
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3s
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1)); // 2s Slowness II
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
                }
            }
        }

        player.sendMessage("§e§l[Inspiration] §aBoosted " + boosted + " teammates with Speed II!");
        snp.setCooldown("leader", 20);
        player.sendActionBar(Component.text("§aInspiration active! 20s cooldown."));
    }

    // =============================================
    // 4. INVENTOR - Metal Detector (find keys/gears)
    // =============================================
    private void handleInventor(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("inventor")) {
            int remaining = snp.getRemainingCooldown("inventor");
            player.sendActionBar(Component.text("§cMetal Detector on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        player.swingMainHand();

        // Scan for nearest drawer (any drawer, not just key drawers)
        Location playerLoc = player.getLocation();
        ArmorStand nearestDrawer = null;
        double nearestDist = Double.MAX_VALUE;

        for (ArmorStand stand : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (stand.getScoreboardTags().contains("sn_drawer")) {
                double dist = stand.getLocation().distance(playerLoc);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestDrawer = stand;
                }
            }
        }

        if (nearestDrawer != null && nearestDist <= 20.0) {
            // Check if it has items
            Inventory drawerInv = plugin.getKeyManager() != null
                    ? plugin.getKeyManager().getDrawerInventory(nearestDrawer) : null;
            boolean hasKey = drawerInv != null && containsKey(drawerInv);
            boolean hasItems = drawerInv != null && !isInventoryEmpty(drawerInv);

            // Beep intensity based on distance
            float pitch = (float)(2.0 - (nearestDist / 20.0)); // Closer = higher pitch
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);

            // Draw particle trail
            Location from = player.getEyeLocation();
            Location to = nearestDrawer.getLocation().add(0, 1, 0);
            Vector dir = to.toVector().subtract(from.toVector());
            int steps = Math.min(16, (int)(nearestDist * 2));
            Particle trailParticle = hasKey ? Particle.ENCHANTED_HIT : Particle.CRIT;
            for (int i = 0; i < steps; i++) {
                Location pLoc = from.clone().add(dir.clone().multiply((double) i / steps));
                player.spawnParticle(trailParticle, pLoc, 1, 0, 0, 0, 0);
            }

            String content = hasKey ? "§a§lKEY INSIDE!" : (hasItems ? "§eItems detected" : "§7Empty");
            player.sendMessage("§9§l[Metal Detector] §eDrawer §a" + Math.round(nearestDist) + "m §eaway — " + content);

            snp.setCooldown("inventor", 8);
            player.sendActionBar(Component.text("§aScan complete! 8s cooldown."));
        } else {
            player.sendMessage("§9§l[Metal Detector] §7No drawers detected within 20m.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            snp.setCooldown("inventor", 3);
        }
    }

    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }

    // =============================================
    // 5. SCOUT - Slingshot (projectile stun)
    // =============================================
    private void handleScout(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("slingshot")) {
            int remaining = snp.getRemainingCooldown("slingshot");
            player.sendActionBar(Component.text("§cSlingshot on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        player.swingMainHand();
        Snowball nut = player.launchProjectile(Snowball.class);
        nut.setMetadata("scout_nut", new FixedMetadataValue(plugin, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.3f);

        snp.setCooldown("slingshot", 3);
        player.sendActionBar(Component.text("§aShot fired! 3s cooldown."));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball nut)) return;
        if (!nut.hasMetadata("scout_nut")) return;

        Location loc = nut.getLocation();
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 0.1, 0.1, 0.1, 0.1, Material.OAK_PLANKS.createBlockData());
        loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);

        if (event.getHitEntity() instanceof Player hitPlayer) {
            SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(hitPlayer.getUniqueId());
            if (targetSnp != null && targetSnp.isNeighbor() && targetSnp.isAlive()) {
                hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3)); // 2s Slowness IV
                hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0)); // 1s Blind
                hitPlayer.playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 0.8f);

                // Increment Neighbor's Rage when hit by Scout slingshot
                targetSnp.incrementHitsTaken();
                int rageHits = targetSnp.getHitsTaken();
                plugin.getGameManager().updateNeighborRageItem(hitPlayer, rageHits);

                if (nut.getShooter() instanceof Player shooter) {
                    shooter.setMetadata("sn_thrown_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    hitPlayer.damage(2.0, shooter); // Deal damage (2.0 = 1.0 heart)
                    shooter.removeMetadata("sn_thrown_damage", plugin);
                    // Increment Counter-Strike charge for Brave
                    SNPlayer shooterSnp = plugin.getGameManager().getPlayers().get(shooter.getUniqueId());
                    if (shooterSnp != null && shooterSnp.getChildClass() == ChildClass.BRAVE) {
                        shooterSnp.incrementHitsTaken();
                    }
                }
            }
        }
    }

    // =============================================
    // 6. ENGINEER - Tripwire Sensor (custom class)
    //    Place proximity sensors that alert the team
    //    when the Neighbor walks near them.
    // =============================================
    private void handleEngineer(Player player, SNPlayer snp, Block clickedBlock, org.bukkit.block.BlockFace face) {
        if (snp.isOnCooldown("engineer")) {
            int remaining = snp.getRemainingCooldown("engineer");
            player.sendActionBar(Component.text("§cSensor on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        Block placeBlock;
        if (clickedBlock != null && face != null) {
            placeBlock = clickedBlock.getRelative(face);
        } else {
            placeBlock = player.getLocation().getBlock();
        }

        if (placeBlock.getType() != Material.AIR) {
            player.sendMessage("§cCannot place sensor here!");
            return;
        }

        // Maximum 3 active sensors
        if (engineerTraps.size() >= 3) {
            Location oldest = engineerTraps.remove(0);
            oldest.getBlock().setType(Material.AIR);
            player.sendMessage("§7Oldest sensor removed (max 3).");
        }

        placeBlock.setType(Material.TRIPWIRE_HOOK);
        engineerTraps.add(placeBlock.getLocation());

        player.sendMessage("§e§l[Sensor] §aTripwire sensor placed! §7(" + engineerTraps.size() + "/3 active)");
        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_ATTACH, 1f, 1f);
        snp.setCooldown("engineer", 5);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        engineerTraps.remove(event.getBlock().getLocation());
    }

    private void startEngineerTrapTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
                    engineerTraps.clear();
                    // Clean up decoys
                    for (ArmorStand as : gamerDecoys.values()) {
                        if (as != null && !as.isDead()) as.remove();
                    }
                    gamerDecoys.clear();
                    return;
                }

                Iterator<Location> it = engineerTraps.iterator();
                while (it.hasNext()) {
                    Location loc = it.next();
                    if (loc.getBlock().getType() != Material.TRIPWIRE_HOOK) {
                        it.remove();
                        continue;
                    }

                    for (Player p : loc.getWorld().getPlayers()) {
                        SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
                        if (targetSnp != null && targetSnp.isNeighbor() && targetSnp.isAlive()) {
                            if (p.getLocation().distanceSquared(loc.clone().add(0.5, 0, 0.5)) < 2.25) {
                                triggerSensor(loc, p);
                                it.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L);
    }

    private void triggerSensor(Location loc, Player neighbor) {
        loc.getBlock().setType(Material.AIR);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.05);

        neighbor.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3s
        neighbor.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2)); // 3s Slowness III

        // Alert ALL children
        for (SNPlayer snp : plugin.getGameManager().getPlayers().values()) {
            if (snp.isChild() && snp.isAlive()) {
                Player child = Bukkit.getPlayer(snp.getUuid());
                if (child != null) {
                    child.sendMessage("§e§l[Sensor] §c⚠ Pergerakan terdeteksi di §6" +
                            loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "§c!");
                    child.playSound(child.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                }
            }
        }
    }

    // =============================================
    // 7. GAMER - Decoy Hologram (custom class)
    //    Spawn a decoy armor stand at target location
    //    that plays distraction sounds to lure Neighbor.
    // =============================================
    private void handleGamer(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("gamer")) {
            int remaining = snp.getRemainingCooldown("gamer");
            player.sendActionBar(Component.text("§cDecoy on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Remove old decoy if exists
        ArmorStand oldDecoy = gamerDecoys.remove(player.getUniqueId());
        if (oldDecoy != null && !oldDecoy.isDead()) {
            oldDecoy.remove();
        }

        // Place decoy at target block
        Block target = player.getTargetBlockExact(10);
        Location decoyLoc;
        if (target != null) {
            decoyLoc = target.getLocation().add(0.5, 1, 0.5);
        } else {
            decoyLoc = player.getLocation().add(player.getLocation().getDirection().multiply(8));
        }

        // Spawn invisible armor stand as decoy marker
        ArmorStand decoy = decoyLoc.getWorld().spawn(decoyLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.customName(Component.text("§a§l? ? ?"));
            as.addScoreboardTag("sn_decoy");
        });
        gamerDecoys.put(player.getUniqueId(), decoy);

        // Play distraction sounds at decoy location for 8 seconds
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        new BukkitRunnable() {
            int ticks = 0;
            final Sound[] sounds = {
                    Sound.BLOCK_IRON_DOOR_OPEN,
                    Sound.BLOCK_CHEST_OPEN,
                    Sound.ENTITY_ITEM_PICKUP,
                    Sound.BLOCK_GLASS_BREAK,
                    Sound.BLOCK_IRON_DOOR_CLOSE
            };
            final Random rng = new Random();

            @Override
            public void run() {
                if (decoy.isDead() || ticks >= 160) { // 8 seconds
                    if (!decoy.isDead()) decoy.remove();
                    gamerDecoys.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (ticks % 40 == 0) { // Every 2 seconds
                    Sound s = sounds[rng.nextInt(sounds.length)];
                    decoyLoc.getWorld().playSound(decoyLoc, s, 1.5f, 1.0f);
                }
                // Particle effect to show decoy location
                decoyLoc.getWorld().spawnParticle(Particle.SMOKE, decoyLoc, 2, 0.2, 0.3, 0.2, 0.01);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        player.sendMessage("§b§l[Decoy] §aHologram deployed! Distracting sounds will play for 8 seconds.");
        snp.setCooldown("gamer", 15);
        player.sendActionBar(Component.text("§aDecoy active! 15s cooldown."));
    }

    public void triggerActiveAbility(Player player, SNPlayer snp) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
            player.sendMessage("§cGame tidak sedang berjalan!");
            return;
        }
        if (snp == null || !snp.isAlive()) return;
        if (!snp.isChild() && snp.isBapakeMode()) return;
        
        if (snp.isGrabbed()) {
            player.sendMessage("§cYou cannot use abilities while you are grabbed!");
            return;
        }

        ChildClass cc = snp.getChildClass();
        if (cc == null) return;

        switch (cc) {
            case DETECTIVE -> handleDetective(player, snp);
            case BAGGER -> handleBagger(player, snp);
            case LEADER -> handleLeader(player, snp);
            case INVENTOR -> handleInventor(player, snp);
            case SCOUT -> handleScout(player, snp);
            case ENGINEER -> {
                org.bukkit.block.Block targetBlock = player.getTargetBlockExact(4);
                if (targetBlock != null) {
                    handleEngineer(player, snp, targetBlock, org.bukkit.block.BlockFace.UP);
                } else {
                    player.sendMessage("§cArahkan ke blok untuk menaruh sensor!");
                }
            }
            case GAMER -> handleGamer(player, snp);
            case BRAVE -> handleBrave(player, snp);
            default -> {}
        }
    }

    private void handleBrave(Player player, SNPlayer snp) {
        if (snp.isOnCooldown("bat")) {
            int remaining = snp.getRemainingCooldown("bat");
            player.sendActionBar(Component.text("§cBat on cooldown! (" + remaining + "s)"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Visual Baseball Bat Swing Effect
        ItemStack original = player.getInventory().getItemInMainHand();
        ItemStack bat = new ItemStack(Material.FEATHER);
        ItemMeta meta = bat.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1200);
            bat.setItemMeta(meta);
        }
        
        player.getInventory().setItemInMainHand(bat);
        player.swingMainHand();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

        // Put original item back after 4 ticks (200ms)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = player.getInventory().getItemInMainHand();
            if (current != null && current.getType() == Material.FEATHER && current.hasItemMeta() && current.getItemMeta().getCustomModelData() == 1200) {
                player.getInventory().setItemInMainHand(original);
            }
        }, 4L);

        // Raycast for Neighbor within 3 blocks, 45-degree cone
        Player target = null;
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        for (Player p : player.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) continue;

            SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
            if (targetSnp == null || !targetSnp.isAlive()) continue;

            Location pLoc = p.getLocation().add(0, 1, 0);
            if (pLoc.distanceSquared(eyeLoc) < 9.0) { // within 3 blocks
                Vector toTarget = pLoc.toVector().subtract(eyeLoc.toVector()).normalize();
                double dot = direction.dot(toTarget);
                if (dot > 0.7) { // 45 degrees
                    target = p;
                    break;
                }
            }
        }

        if (target != null) {
            // Knockback
            Vector kb = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.4);
            target.setVelocity(kb);

            // Stun
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0)); // 2.5s
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2)); // 2.5s Slowness III

            // Sound effects
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.2f, 0.7f);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 1f);

            // Increment Counter-Strike charge
            snp.incrementHitsTaken();
            int hits = snp.getHitsTaken();

            if (hits >= 3) {
                player.sendMessage("§6§l⚡ COUNTER-STRIKE READY! §eYou will auto-escape the next grab!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            }

            snp.setCooldown("bat", 12);
            player.sendActionBar(Component.text("§aHit! Cooldown: 12s | Counter-Strike: " + hits + "/3"));
        } else {
            snp.setCooldown("bat", 2);
            player.sendActionBar(Component.text("§7Swing missed. Cooldown: 2s"));
        }
    }
}
