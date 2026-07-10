package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.SNPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * AtmosphereManager — Central hub for all immersive atmosphere effects.
 *
 * Handles four subsystems simultaneously:
 *   A. Footstep Sound System — movement-based directional footstep audio
 *   B. Heartbeat / Fear System — proximity-based heartbeat + darkness when Neighbor is close
 *   C. Ambient Sound System — random creepy background noises
 *   D. Neighbor Proximity Warning — visual particle + screen effects
 *
 * Designed after the real Secret Neighbor game's atmosphere mechanics.
 */
public class AtmosphereManager {

    private final SecretNeighborPlugin plugin;

    // Track previous positions for movement detection
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    // Ambient sound timer
    private int ambientCooldown = 0;
    private final Random random = new Random();

    private BukkitTask mainTask;
    private BukkitTask heartbeatTask;

    // Footstep material mapping — maps block material to sound category
    private enum FloorType {
        WOOD, STONE, GRASS, METAL, WOOL, SAND, GRAVEL, GLASS
    }

    public AtmosphereManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start all atmosphere loops. Called when game enters IN_GAME state.
     */
    public void start() {
        stop(); // Clean up any previous tasks

        // Main tick (every 5 ticks = 250ms) — Footsteps + Ambient + Proximity Warning
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != GameState.IN_GAME) {
                    return;
                }
                tickFootsteps();
                tickAmbientSounds();
                tickProximityWarning();
            }
        }.runTaskTimer(plugin, 5L, 5L);

        // Heartbeat tick (every 20 ticks = 1s) — Heartbeat + Fear
        heartbeatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != GameState.IN_GAME) {
                    return;
                }
                tickHeartbeat();
                tickCooldownHUD();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Stop all atmosphere loops. Called when game ends.
     */
    public void stop() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        previousLocations.clear();
        ambientCooldown = 0;
    }

    // =========================================================================
    // COOLDOWN HUD SYSTEM
    // =========================================================================
    private void tickCooldownHUD() {
        for (SNPlayer snp : plugin.getGameManager().getSNPlayers()) {
            if (!snp.isAlive() || snp.isKnocked() || snp.isGrabbed()) continue;
            
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p == null) continue;

            Map<String, Integer> activeCDs = snp.getActiveCooldowns();
            if (activeCDs.isEmpty()) continue;

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : activeCDs.entrySet()) {
                if (sb.length() > 0) sb.append(" §7| ");
                String name = formatAbilityName(entry.getKey());
                sb.append("§c").append(name).append(": §f").append(entry.getValue()).append("s");
            }
            p.sendActionBar(net.kyori.adventure.text.Component.text(sb.toString()));
        }
    }

    private String formatAbilityName(String key) {
        return switch(key) {
            case "bear_trap" -> "Bear Trap";
            case "grab" -> "Grab";
            case "mask_toggle" -> "Disguise";
            case "rage" -> "Rage";
            case "detective" -> "Intel";
            case "leader" -> "Inspiration";
            case "inventor" -> "Crafting";
            case "slingshot" -> "Slingshot";
            case "engineer" -> "Radar";
            case "gamer" -> "Decoy";
            case "bat" -> "Bat";
            default -> key;
        };
    }

    // =========================================================================
    // A. FOOTSTEP SOUND SYSTEM
    //
    // In real Secret Neighbor:
    // - All player movement produces audible footstep sounds
    // - Sprinting = loud + fast footsteps (hearable from far away)
    // - Walking = normal footsteps
    // - Sneaking = very quiet footsteps (almost silent)
    // - Standing still = no footsteps
    // - The Neighbor can track children by listening to their footsteps
    // =========================================================================

    private void tickFootsteps() {
        for (SNPlayer snp : plugin.getGameManager().getSNPlayers()) {
            if (!snp.isAlive()) continue;
            Player player = Bukkit.getPlayer(snp.getUuid());
            if (player == null) continue;

            Location current = player.getLocation();
            Location previous = previousLocations.get(snp.getUuid());
            previousLocations.put(snp.getUuid(), current.clone());

            if (previous == null) continue;

            // Calculate horizontal movement distance
            double dx = current.getX() - previous.getX();
            double dz = current.getZ() - previous.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // Skip if not moving meaningfully
            if (horizontalDist < 0.08) continue;

            // Determine movement mode
            boolean isSneaking = player.isSneaking();
            boolean isSprinting = player.isSprinting();

            // Get floor material and determine sound
            Block floorBlock = current.clone().add(0, -0.5, 0).getBlock();
            FloorType floorType = classifyFloor(floorBlock.getType());
            Sound stepSound = getStepSound(floorType);

            // Volume and pitch based on movement mode
            float volume;
            float pitch;
            double hearingRadius;

            if (isSneaking) {
                volume = 0.08f;
                pitch = 0.7f + random.nextFloat() * 0.1f;
                hearingRadius = 4.0;
            } else if (isSprinting) {
                volume = 0.5f;
                pitch = 1.1f + random.nextFloat() * 0.2f;
                hearingRadius = 20.0;
            } else {
                volume = 0.2f;
                pitch = 0.9f + random.nextFloat() * 0.15f;
                hearingRadius = 12.0;
            }

            // Play footstep sound to all players within hearing radius
            double radiusSq = hearingRadius * hearingRadius;
            for (Player listener : Bukkit.getOnlinePlayers()) {
                if (listener.getUniqueId().equals(player.getUniqueId())) continue;

                SNPlayer listenerSnp = plugin.getGameManager().getPlayers().get(listener.getUniqueId());
                if (listenerSnp == null || !listenerSnp.isAlive()) continue;

                double distSq = listener.getLocation().distanceSquared(current);
                if (distSq > radiusSq) continue;

                // Volume attenuation based on distance
                double distFactor = 1.0 - (Math.sqrt(distSq) / hearingRadius);
                float attenuatedVolume = (float) (volume * distFactor);

                listener.playSound(current, stepSound, attenuatedVolume, pitch);
            }

            // Also play own footsteps to the player (very quiet self-awareness)
            player.playSound(current, stepSound, volume * 0.3f, pitch);
        }
    }

    private FloorType classifyFloor(Material mat) {
        String name = mat.name();
        if (name.contains("WOOL") || name.contains("CARPET")) return FloorType.WOOL;
        if (name.contains("SAND")) return FloorType.SAND;
        if (name.contains("GRAVEL") || name.contains("DIRT") || name.contains("PATH")) return FloorType.GRAVEL;
        if (name.contains("GLASS")) return FloorType.GLASS;
        if (name.contains("IRON") || name.contains("ANVIL") || name.contains("CHAIN") ||
            name.contains("COPPER") || name.contains("GOLD") || name.contains("DIAMOND")) return FloorType.METAL;
        if (name.contains("GRASS") || name.contains("LEAVES") || name.contains("MOSS") ||
            name.contains("VINE") || name.contains("FERN")) return FloorType.GRASS;
        if (name.contains("PLANKS") || name.contains("LOG") || name.contains("WOOD") ||
            name.contains("STRIPPED") || name.contains("FENCE") || name.contains("SLAB") && name.contains("OAK") ||
            name.contains("BAMBOO") || name.contains("MANGROVE") || name.contains("CHERRY")) return FloorType.WOOD;
        return FloorType.STONE; // Default for stone, concrete, bricks, etc.
    }

    private Sound getStepSound(FloorType type) {
        return switch (type) {
            case WOOD -> Sound.BLOCK_WOOD_STEP;
            case STONE -> Sound.BLOCK_STONE_STEP;
            case GRASS -> Sound.BLOCK_GRASS_STEP;
            case METAL -> Sound.BLOCK_METAL_STEP;
            case WOOL -> Sound.BLOCK_WOOL_STEP;
            case SAND -> Sound.BLOCK_SAND_STEP;
            case GRAVEL -> Sound.BLOCK_GRAVEL_STEP;
            case GLASS -> Sound.BLOCK_GLASS_STEP;
        };
    }

    // =========================================================================
    // B. HEARTBEAT / FEAR SYSTEM
    //
    // In real Secret Neighbor:
    // - When the Neighbor is nearby, children hear a heartbeat sound
    // - The closer the Neighbor, the louder and faster the heartbeat
    // - Screen darkens with proximity
    // - This is THE key mechanic that creates tension
    // =========================================================================

    private void tickHeartbeat() {
        // Find the Neighbor player
        Player neighbor = null;
        SNPlayer neighborSnp = null;
        for (SNPlayer snp : plugin.getGameManager().getSNPlayers()) {
            if (snp.isNeighbor() && snp.isAlive()) {
                neighborSnp = snp;
                neighbor = Bukkit.getPlayer(snp.getUuid());
                break;
            }
        }
        if (neighbor == null) return;

        boolean isBapake = neighborSnp.isBapakeMode();
        // If not in Bapake mode (i.e., disguised), do not run heartbeat effects!
        if (!isBapake) {
            return;
        }

        boolean isDisguised = neighborSnp.isDisguised();

        for (SNPlayer childSnp : plugin.getGameManager().getSNPlayers()) {
            if (!childSnp.isChild() || !childSnp.isAlive()) continue;
            Player child = Bukkit.getPlayer(childSnp.getUuid());
            if (child == null) continue;

            double dist = child.getLocation().distance(neighbor.getLocation());

            // Tier 1: Far warning (< 20 blocks)
            if (dist < 20.0) {
                float volume = isDisguised ? 0.15f : 0.3f;
                float pitch = 0.6f;
                child.playSound(child.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
            }

            // Tier 2: Medium proximity (< 12 blocks)
            if (dist < 12.0) {
                float volume = isDisguised ? 0.35f : 0.6f;
                float pitch = 1.0f;
                child.playSound(child.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);

                // Brief darkness flash
                child.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 25, 0, false, false));
            }

            // Tier 3: Close danger (< 6 blocks)
            if (dist < 6.0) {
                float volume = isBapake ? 1.2f : 0.9f;
                float pitch = 1.5f;
                child.playSound(child.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                // Double heartbeat for urgency
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (child.isOnline()) {
                        child.playSound(child.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume * 0.8f, 1.8f);
                    }
                }, 5L);

                // Longer darkness
                child.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 35, 0, false, false));
            }

            // Tier 4: Extreme danger — Bapake mode chase (< 10 blocks while revealed)
            if (isBapake && dist < 10.0) {
                // Intense chase audio
                child.playSound(child.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 0.3f, 1.2f);
            }
        }
    }

    // =========================================================================
    // C. AMBIENT SOUND SYSTEM
    //
    // In real Secret Neighbor:
    // - The house constantly makes creepy ambient noises
    // - Creaking doors, wind, clock ticking, random thuds
    // - Creates persistent unease even when nothing is happening
    // =========================================================================

    private void tickAmbientSounds() {
        ambientCooldown--;
        if (ambientCooldown > 0) return;

        // Random interval between 6-12 seconds (24-48 main ticks at 5 tick interval)
        ambientCooldown = 24 + random.nextInt(24);

        // Pick a random ambient sound
        int choice = random.nextInt(10);
        Sound sound;
        float volume;
        float pitch;

        switch (choice) {
            case 0, 1 -> {
                // Creaking door
                sound = Sound.BLOCK_WOODEN_DOOR_OPEN;
                volume = 0.15f;
                pitch = 0.3f + random.nextFloat() * 0.3f;
            }
            case 2 -> {
                // Wind
                sound = Sound.ITEM_ELYTRA_FLYING;
                volume = 0.1f;
                pitch = 0.2f + random.nextFloat() * 0.2f;
            }
            case 3, 4 -> {
                // Clock tick
                sound = Sound.BLOCK_NOTE_BLOCK_HAT;
                volume = 0.08f;
                pitch = 0.4f + random.nextFloat() * 0.2f;
            }
            case 5 -> {
                // Distant thud
                sound = Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR;
                volume = 0.06f;
                pitch = 0.2f + random.nextFloat() * 0.2f;
            }
            case 6 -> {
                // Creak
                sound = Sound.BLOCK_WOODEN_DOOR_CLOSE;
                volume = 0.1f;
                pitch = 0.5f + random.nextFloat() * 0.3f;
            }
            case 7 -> {
                // Low rumble
                sound = Sound.AMBIENT_CAVE;
                volume = 0.12f;
                pitch = 0.3f;
            }
            case 8 -> {
                // Glass rattle
                sound = Sound.BLOCK_GLASS_STEP;
                volume = 0.07f;
                pitch = 0.8f + random.nextFloat() * 0.4f;
            }
            default -> {
                // Wooden creak
                sound = Sound.BLOCK_FENCE_GATE_OPEN;
                volume = 0.09f;
                pitch = 0.4f + random.nextFloat() * 0.3f;
            }
        }

        // Play to all in-game players with random positional offset
        for (SNPlayer snp : plugin.getGameManager().getSNPlayers()) {
            if (!snp.isAlive()) continue;
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p == null) continue;

            // Random offset around the player so it sounds like it's coming from the house
            double ox = (random.nextDouble() - 0.5) * 16;
            double oy = (random.nextDouble() - 0.5) * 4;
            double oz = (random.nextDouble() - 0.5) * 16;
            Location soundLoc = p.getLocation().add(ox, oy, oz);

            p.playSound(soundLoc, sound, volume, pitch);
        }
    }

    // =========================================================================
    // D. NEIGHBOR PROXIMITY WARNING (VISUAL)
    //
    // In real Secret Neighbor:
    // - Red vignette effect appears on screen edges when Neighbor is near
    // - Screen distortion / static effect at very close range
    // - Subtle visual warning before the heartbeat kicks in
    // =========================================================================

    private void tickProximityWarning() {
        Player neighbor = null;
        SNPlayer neighborSnp = null;
        for (SNPlayer snp : plugin.getGameManager().getSNPlayers()) {
            if (snp.isNeighbor() && snp.isAlive()) {
                neighborSnp = snp;
                neighbor = Bukkit.getPlayer(snp.getUuid());
                break;
            }
        }
        if (neighbor == null) return;

        boolean isBapake = neighborSnp.isBapakeMode();
        // If not in Bapake mode (i.e., disguised), do not run proximity warnings!
        if (!isBapake) {
            return;
        }

        for (SNPlayer childSnp : plugin.getGameManager().getSNPlayers()) {
            if (!childSnp.isChild() || !childSnp.isAlive()) continue;
            Player child = Bukkit.getPlayer(childSnp.getUuid());
            if (child == null) continue;

            double dist = child.getLocation().distance(neighbor.getLocation());

            // Tier 1: Distant warning (< 15 blocks) — subtle red dust particles at screen edges
            if (dist < 15.0) {
                // Spawn red dust particles around player's peripheral vision
                Location eyeLoc = child.getEyeLocation();
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 1.5 + random.nextDouble() * 0.5;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location particleLoc = eyeLoc.clone().add(px, -0.5 + random.nextDouble(), pz);

                    float size = (float) (1.0 - (dist / 15.0)) * 1.5f;
                    child.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 20, 20), size));
                }
            }

            // Tier 2: Medium warning (< 8 blocks) — cave ambience + more particles
            if (dist < 8.0) {
                // Only play ambient cave periodically (controlled by main tick timing)
                if (random.nextInt(4) == 0) {
                    child.playSound(child.getLocation(), Sound.AMBIENT_CAVE, 0.25f, 1.2f);
                }
            }

            // Tier 3: Extreme proximity (< 4 blocks) — screen shake effect
            if (dist < 4.0 && isBapake) {
                // Subtle screen shake via tiny velocity impulse
                double shakeY = (random.nextDouble() - 0.5) * 0.02;
                double shakeX = (random.nextDouble() - 0.5) * 0.01;
                child.setVelocity(child.getVelocity().add(new org.bukkit.util.Vector(shakeX, shakeY, shakeX)));

                // Elder guardian curse sound (very quiet, only in bapake mode)
                if (random.nextInt(8) == 0) {
                    child.playSound(child.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.15f, 1.5f);
                }
            }
        }
    }
}
