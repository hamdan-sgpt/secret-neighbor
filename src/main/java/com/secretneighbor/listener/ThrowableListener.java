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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.FluidCollisionMode;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.UUID;

/**
 * Handles realistic physical throwable items in Secret Neighbor.
 * - Allows throwing ALL custom items (Keys, Cards, Tools, Furniture, Abilities).
 * - Uses ItemDisplay entities for smooth, center-aligned tumbling rotations.
 * - Realistic vector reflections (bouncing) off walls, ceilings, and floors.
 * - Precise bounding offsets to prevent items clipping through walls.
 * - Custom scales, bounciness, gravity, speeds, and stuns based on item size/weight.
 */
public class ThrowableListener implements Listener {

    private static final String THROWN_TAG = "sn_thrown_projectile";
    private static final double AIR_DRAG = 0.985; // Velocity multiplier per tick
    private static final int MAX_FLIGHT_TICKS = 120; // 6 seconds max flight

    private final SecretNeighborPlugin plugin;
    private final Random rand = new Random();

    public ThrowableListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a CustomModelData value belongs to a valid Secret Neighbor custom item.
     */
    public static boolean isSecretNeighborItem(int cmd) {
        if (cmd >= 1000 && cmd <= 1005) return true; // Keys
        if (cmd >= 1010 && cmd <= 1016) return true; // Tools & Cards
        if (cmd >= 1100 && cmd <= 1110) return true; // Throwables
        if (cmd >= 1200 && cmd <= 1207) return true; // Child class items
        if (cmd >= 1300 && cmd <= 1304) return true; // Neighbor items (Bear Trap, Masks, Rage)
        return false;
    }

    // --- PROPERTY GETTERS ---

    /**
     * Returns the display scale for each item, calibrated from actual model bounding boxes.
     * Each model has its own "fixed" display scale baked in. This transformation scale is
     * applied ON TOP of that, so the final visible size = model_raw × fixed_scale × this_scale.
     * Values are tuned so items match real-world proportions (1 block = 1 meter).
     */
    public static float getItemScale(int cmd) {
        return switch (cmd) {
            case 1100 -> 3.00f; // Box
            case 1101 -> 3.00f; // Chair (reduced)
            case 1102 -> 3.11f; // TV
            case 1103 -> 3.00f; // Sofa (reduced)
            case 1104 -> 3.44f; // Painting
            case 1105 -> 2.46f; // Book
            case 1106 -> 1.04f; // Tomato
            case 1107 -> 3.00f; // Broom (reduced)
            case 1108 -> 2.69f; // Basketball
            case 1109 -> 3.98f; // Pillow
            case 1110 -> 2.44f; // Hat
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 0.34f; // Keys
            case 1014, 1015, 1016 -> 0.42f; // Keycards
            case 1010 -> 1.34f; // Crowbar
            case 1011 -> 0.81f; // Flashlight
            case 1012 -> 0.81f; // Radar
            case 1013 -> 1.26f; // Hammer
            case 1200 -> 2.38f; // Baseball Bat
            case 1201 -> 1.12f; // Camera
            case 1202 -> 1.62f; // Backpack
            case 1203 -> 1.23f; // Megaphone
            case 1204 -> 0.95f; // Wrench
            case 1205 -> 1.06f; // Slingshot
            case 1206 -> 1.04f; // Sensor
            case 1207 -> 1.40f; // Decoy Console
            case 1300 -> 1.34f; // Bear Trap
            case 1301, 1302 -> 1.26f; // Masks
            default -> 1.00f;
        };
    }

    /**
     * Returns the actual half-height of the item after scaling, in blocks.
     * For FIXED ItemDisplay, the model is centered at the entity location.
     * This value is added to the floor Y to place the bottom edge of the item
     * exactly on the surface. Calculated from model bounding boxes.
     */
    public static float getItemHalfHeight(int cmd) {
        return switch (cmd) {
            case 1100 -> 0.490f; // Box
            case 1101 -> 0.796f; // Chair (0.531 * 3.0 / 2)
            case 1102 -> 0.630f; // TV
            case 1103 -> 0.609f; // Sofa (0.406 * 3.0 / 2)
            case 1104 -> 0.700f; // Painting
            case 1105 -> 0.115f; // Book
            case 1106 -> 0.090f; // Tomato
            case 1107 -> 1.032f; // Broom (0.688 * 3.0 / 2)
            case 1108 -> 0.336f; // Basketball
            case 1109 -> 0.218f; // Pillow
            case 1110 -> 0.246f; // Hat
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 0.028f; // Keys
            case 1014, 1015, 1016 -> 0.028f; // Keycards
            case 1010 -> 0.840f; // Crowbar
            case 1011 -> 0.308f; // Flashlight
            case 1012 -> 0.308f; // Radar
            case 1013 -> 0.493f; // Hammer
            case 1200 -> 1.190f; // Baseball Bat
            case 1201 -> 0.193f; // Camera
            case 1202 -> 0.630f; // Backpack
            case 1203 -> 0.420f; // Megaphone
            case 1204 -> 0.420f; // Wrench
            case 1205 -> 0.350f; // Slingshot
            case 1206 -> 0.308f; // Sensor
            case 1207 -> 0.308f; // Decoy Console
            case 1300 -> 0.210f; // Bear Trap
            case 1301, 1302 -> 0.280f; // Masks
            default -> 0.30f;
        };
    }

    /**
     * Returns the collision radius for block raytrace offset, based on actual final item dimensions.
     */
    public static double getCollisionRadius(int cmd) {
        return switch (cmd) {
            case 1103 -> 0.35; // Sofa (~0.70m wide / 2)
            case 1101 -> 0.28; // Chair (~0.55m / 2)
            case 1100 -> 0.20; // Box (~0.40m / 2)
            case 1104 -> 0.25; // Painting (~0.50m / 2)
            case 1107 -> 0.15; // Broom (thin stick)
            case 1109 -> 0.20; // Pillow (~0.40m / 2)
            case 1102 -> 0.17; // TV (~0.35m / 2)
            case 1110 -> 0.15; // Hat (~0.30m / 2)
            case 1108 -> 0.12; // Basketball (~0.24m / 2)
            case 1202 -> 0.15; // Backpack
            case 1200, 1010 -> 0.10; // Bat, Crowbar (thin)
            case 1013 -> 0.12; // Hammer
            case 1105 -> 0.11; // Book
            case 1106 -> 0.04; // Tomato (tiny)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 0.03; // Keys (very small)
            case 1014, 1015, 1016 -> 0.04; // Cards
            default -> 0.12; // Default for small tools
        };
    }

    public static double getItemThrowSpeed(int cmd) {
        return switch (cmd) {
            case 1103 -> 0.75; // Sofa (heavy, slow throw)
            case 1100 -> 0.95; // Box
            case 1101, 1102 -> 0.95; // Chair, TV
            case 1104, 1107 -> 1.05; // Painting, Broom
            case 1105 -> 1.35; // Book (light, fast throw)
            case 1106 -> 1.45; // Tomato (very fast)
            case 1108 -> 1.4; // Basketball
            case 1109, 1110 -> 1.15; // Pillow, Hat
            // Keys & Cards (easy to flick)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 1.35;
            case 1014, 1015, 1016 -> 1.35;
            default -> 1.15;
        };
    }

    public static double getBounciness(int cmd) {
        return switch (cmd) {
            case 1108 -> 0.75; // Basketball
            case 1106 -> 0.0; // Tomato (splats)
            case 1103 -> 0.15; // Sofa (absorbs impact)
            case 1109 -> 0.15; // Pillow (cushiony)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 0.12; // Keys (metallic thud, tiny bounce)
            case 1014, 1015, 1016 -> 0.12; // Cards
            default -> 0.25; // Default wood/metal bounce
        };
    }

    public static double getItemGravity(int cmd) {
        return switch (cmd) {
            case 1103 -> 0.055; // Sofa (heavy)
            case 1100, 1101, 1102 -> 0.045; // Box, Chair, TV
            case 1109, 1110 -> 0.032; // Pillow, Hat (floaty)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 0.038; // Keys
            case 1014, 1015, 1016 -> 0.038;
            default -> 0.04;
        };
    }

    public static int getStunTicks(int cmd) {
        return switch (cmd) {
            case 1103 -> 50; // Sofa (2.5s)
            case 1100 -> 40; // Box (2.0s)
            case 1101, 1102 -> 40; // Chair, TV (2.0s)
            case 1010, 1013, 1200 -> 35; // Crowbar, Hammer, Bat (1.75s)
            case 1107, 1108 -> 25; // Broom, Basketball (1.25s)
            case 1104, 1109, 1110 -> 20; // Painting, Pillow, Hat (1.0s)
            case 1106 -> 15; // Tomato (0.75s)
            case 1105 -> 15; // Book (0.75s)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 10; // Keys (0.5s)
            case 1014, 1015, 1016 -> 10; // Cards (0.5s)
            default -> 15;
        };
    }

    public static double getItemDamage(int cmd) {
        return switch (cmd) {
            case 1103 -> 6.0; // Sofa
            case 1100, 1101, 1102 -> 5.0; // Box, Chair, TV
            case 1010, 1013, 1200, 1204 -> 3.5; // Tools (Crowbar, Hammer, Baseball Bat, Wrench)
            case 1104, 1107, 1108, 1109, 1110 -> 2.5; // Painting, Broom, Basketball, Pillow, Hat
            case 1105, 1106 -> 1.0; // Book, Tomato
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 1.0; // Keys
            case 1014, 1015, 1016 -> 1.0; // Keycards
            default -> 1.5;
        };
    }

    public static double getSpinYawSpeed(int cmd) {
        return switch (cmd) {
            case 1103 -> 6.0; // Sofa (slow)
            case 1100, 1101, 1102 -> 10.0; // Box, Chair, TV
            case 1108 -> 25.0; // Basketball (fast)
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 20.0; // Keys
            default -> 15.0;
        };
    }

    public static double getSpinPitchSpeed(int cmd) {
        return switch (cmd) {
            case 1103 -> 4.0;
            case 1100, 1101, 1102 -> 6.0;
            case 1108 -> 18.0;
            case 1000, 1001, 1002, 1003, 1004, 1005 -> 15.0;
            default -> 10.0;
        };
    }

    // --- EVENT LISTENERS ---

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        org.bukkit.entity.Item droppedItem = event.getItemDrop();
        ItemStack itemStack = droppedItem.getItemStack();

        if (itemStack.getType() != Material.FEATHER || !itemStack.hasItemMeta()) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasCustomModelData()) return;

        int cmd = meta.getCustomModelData();
        if (!isSecretNeighborItem(cmd)) return;

        // Prevent dropping ability items
        org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(plugin, "sn_ability");
        if (meta.getPersistentDataContainer().has(abilityKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            player.sendMessage("§cYou cannot throw ability items!");
            event.setCancelled(true);
            return;
        }

        // Check if game is in progress
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) {
            player.sendMessage("§cYou can only throw items during the game!");
            event.setCancelled(true);
            return;
        }

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        // Prevent grabbed children from throwing items
        if (snp.isGrabbed()) {
            player.sendMessage("§cYou cannot throw items while you are grabbed!");
            event.setCancelled(true);
            return;
        }

        // Do NOT cancel the event so Minecraft naturally decrements the player's hand stack.
        // Instead, delete the spawned Minecraft Item entity instantly from the world.
        droppedItem.remove();

        // Launch the 3D projectile
        ItemStack thrownStack = itemStack.clone();
        thrownStack.setAmount(1);
        launch3DProjectile(player, thrownStack, cmd);
    }

    /**
     * Launches a visible 3D ItemDisplay projectile that simulates realistic physics.
     */
    private void launch3DProjectile(Player thrower, ItemStack itemStack, int cmd) {
        // Spawn location: slightly in front of the player at eye level
        Location eyeLoc = thrower.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Location spawnLoc = eyeLoc.clone().add(dir.clone().multiply(0.8));

        // Create an ItemDisplay entity
        ItemDisplay projectile = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.addScoreboardTag(THROWN_TAG);
            entity.addScoreboardTag("sn_thrower_" + thrower.getUniqueId().toString());
            entity.addScoreboardTag("sn_cmd_" + cmd);

            // Scale based on size
            float scale = getItemScale(cmd);
            Transformation transform = entity.getTransformation();
            transform.getScale().set(scale, scale, scale);
            entity.setTransformation(transform);
        });

        // Speed depends on item weight/type
        double speed = getItemThrowSpeed(cmd);
        Vector velocity = dir.clone().multiply(speed);
        // Slight upward arc
        velocity.setY(velocity.getY() + 0.12);

        // Play throw sound
        thrower.getWorld().playSound(thrower.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.7f);

        // Start physics simulation
        new ProjectilePhysicsTask(projectile, velocity, thrower.getUniqueId(), cmd, itemStack).runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Custom physics task that runs every tick to simulate projectile flight.
     * Handles gravity, air drag, spin rotation, entity collision, and block collision with reflections (bouncing).
     */
    private class ProjectilePhysicsTask extends BukkitRunnable {

        private final ItemDisplay projectile;
        private Vector velocity;
        private final UUID throwerUuid;
        private final int cmd;
        private final ItemStack itemStack;
        private int ticks = 0;
        private double spinYaw = 0;
        private double spinPitch = 0;
        private boolean hasLanded = false;

        ProjectilePhysicsTask(ItemDisplay projectile, Vector velocity, UUID throwerUuid, int cmd, ItemStack itemStack) {
            this.projectile = projectile;
            this.velocity = velocity;
            this.throwerUuid = throwerUuid;
            this.cmd = cmd;
            this.itemStack = itemStack;
            
            // Random initial spin angles
            Random r = new Random();
            this.spinYaw = r.nextDouble() * 360;
            this.spinPitch = r.nextDouble() * 360;
        }

        @Override
        public void run() {
            if (!projectile.isValid() || hasLanded) {
                return;
            }

            ticks++;
            if (ticks > MAX_FLIGHT_TICKS) {
                land(projectile.getLocation());
                return;
            }

            double gravity = getItemGravity(cmd);
            double collisionRadius = getCollisionRadius(cmd);
            double bounciness = getBounciness(cmd);

            // Apply gravity
            velocity.setY(velocity.getY() - gravity);

            // Apply air drag
            velocity.multiply(AIR_DRAG);

            Location currentLoc = projectile.getLocation();
            Vector dir = velocity.clone().normalize();
            double speed = velocity.length();

            if (speed < 0.01) {
                land(currentLoc);
                return;
            }

            // Check for entity collision first (within a radius around the projectile)
            Player hitPlayer = checkPlayerCollision(currentLoc);
            if (hitPlayer != null) {
                onHitEntity(hitPlayer, currentLoc);
                return;
            }

            // Raytrace block collision slightly further than speed to prevent clipping
            World world = currentLoc.getWorld();
            RayTraceResult result = world.rayTraceBlocks(
                    currentLoc,
                    dir,
                    speed + collisionRadius,
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result != null && result.getHitBlock() != null) {
                org.bukkit.block.Block hitBlock = result.getHitBlock();

                // Break glass blocks and continue flying
                if (hitBlock.getType().name().contains("GLASS")) {
                    hitBlock.setType(Material.AIR);
                    hitBlock.getWorld().playSound(hitBlock.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
                    hitBlock.getWorld().spawnParticle(
                            Particle.BLOCK,
                            hitBlock.getLocation().clone().add(0.5, 0.5, 0.5),
                            15, 0.3, 0.3, 0.3, 0.05,
                            hitBlock.getType().createBlockData()
                    );

                    // Slow down slightly and continue flying
                    velocity.multiply(0.85);
                    currentLoc.add(velocity);
                    projectile.teleport(currentLoc);
                    return;
                }

                Vector hitPos = result.getHitPosition();
                BlockFace hitFace = result.getHitBlockFace();
                
                // If hit face is null (shouldn't happen for blocks), default to UP normal
                Vector normal = hitFace != null ? hitFace.getDirection() : new Vector(0, 1, 0);

                // Precise location: touch the wall/floor without clipping (hitPos + normal * radius)
                Location hitLoc = new Location(world, hitPos.getX(), hitPos.getY(), hitPos.getZ())
                        .add(normal.clone().multiply(collisionRadius));

                // Play hit sound at impact point
                playHitSound(hitLoc, cmd);

                // Tomato splat special effect (does not bounce or spawn item)
                if (cmd == 1106) {
                    spawnTomatoSplat(hitLoc);
                    projectile.remove();
                    hasLanded = true;
                    cancel();
                    return;
                }

                // Reflect velocity vector
                double dot = velocity.dot(normal);
                Vector reflected = velocity.clone().subtract(normal.clone().multiply(2 * dot));
                reflected.multiply(bounciness);

                // Check landing condition: low speed OR hitting floor with low vertical velocity
                if (reflected.lengthSquared() < 0.02 || (normal.getY() > 0.5 && Math.abs(reflected.getY()) < 0.05)) {
                    land(hitLoc);
                    return;
                }

                // Bounce off: update velocity and position
                velocity = reflected;
                currentLoc = hitLoc;
            } else {
                // No collision: move forward
                currentLoc.add(velocity);
            }

            // Update spin rotation
            spinYaw = (spinYaw + getSpinYawSpeed(cmd)) % 360;
            spinPitch = (spinPitch + getSpinPitchSpeed(cmd)) % 360;

            // Teleport and rotate
            Location teleportLoc = currentLoc.clone();
            teleportLoc.setYaw((float) spinYaw);
            teleportLoc.setPitch((float) spinPitch);
            projectile.teleport(teleportLoc);

            // Trail particles
            if (ticks % 2 == 0) {
                world.spawnParticle(Particle.CRIT, currentLoc, 1, 0, 0, 0, 0);
            }
        }

        private Player checkPlayerCollision(Location loc) {
            double r = getCollisionRadius(cmd) + 0.40; // Item size + player hitbox
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, r, 1.0, r)) {
                if (entity instanceof Player p && !p.getUniqueId().equals(throwerUuid)) {
                    SNPlayer snp = plugin.getGameManager().getPlayers().get(p.getUniqueId());
                    if (snp != null && snp.isAlive()) {
                        // Check actual Y range: player feet to head (1.8m)
                        double py = p.getLocation().getY();
                        double iy = loc.getY();
                        if (iy >= py - 0.2 && iy <= py + 1.9) {
                            return p;
                        }
                    }
                }
            }
            return null;
        }

        private void onHitEntity(Player hitPlayer, Location hitLoc) {
            hasLanded = true;
            cancel();

            Player thrower = Bukkit.getPlayer(throwerUuid);

            // Play impact sound based on item type
            playHitSound(hitLoc, cmd);

            // Apply knockback to hit player
            if (thrower != null) {
                Vector kb = hitPlayer.getLocation().toVector()
                        .subtract(thrower.getLocation().toVector())
                        .normalize().multiply(0.45).setY(0.2);
                hitPlayer.setVelocity(kb);
            }

            // Check game interactions (child hitting neighbor, etc.)
            SNPlayer targetSnp = plugin.getGameManager().getPlayers().get(hitPlayer.getUniqueId());
            SNPlayer shooterSnp = thrower != null ? plugin.getGameManager().getPlayers().get(throwerUuid) : null;

            if (targetSnp != null && shooterSnp != null && thrower != null) {
                // Apply damage, knockback, and slowness stun to ANY hit player (Friendly Fire Enabled)
                double damage = getItemDamage(cmd);
                
                thrower.setMetadata("sn_thrown_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                hitPlayer.damage(damage, thrower);
                thrower.removeMetadata("sn_thrown_damage", plugin);

                Vector push = velocity.clone().normalize().multiply(0.5).setY(0.2);
                hitPlayer.setVelocity(push);

                int stunTicks = getStunTicks(cmd);
                if (stunTicks > 0) {
                    hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunTicks, 1));
                    if (targetSnp.isNeighbor()) {
                        hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, stunTicks, 0));
                    }
                }

                if (targetSnp.isNeighbor() && shooterSnp.isChild()) {
                    // Struck the Neighbor -> trigger rage, grab cancel, Brave Counter-strike
                    handleNeighborHit(hitPlayer, thrower, cmd);
                }
            }

            // Impact particles
            hitLoc.getWorld().spawnParticle(Particle.CRIT, hitLoc, 12, 0.2, 0.2, 0.2, 0.1);

            // Tomato splat effect (doesn't drop on floor)
            if (cmd == 1106) {
                spawnTomatoSplat(hitLoc);
                projectile.remove();
                return;
            }

            // Calculate where the item should land on the floor near the hit player
            // Use the hit player's feet location and raytrace straight down to find floor
            Location playerFeet = hitPlayer.getLocation();
            Location floorLoc = findFloor(playerFeet);
            projectile.remove();
            spawn3DItem(floorLoc, itemStack);
        }

        private void spawnTomatoSplat(Location hitLoc) {
            hitLoc.getWorld().spawnParticle(
                    Particle.BLOCK,
                    hitLoc,
                    45,
                    0.25, 0.25, 0.25,
                    Material.RED_CONCRETE.createBlockData()
            );
            hitLoc.getWorld().playSound(hitLoc, Sound.ENTITY_LLAMA_SPIT, 0.8f, 0.8f);
        }

        private void land(Location loc) {
            hasLanded = true;
            cancel();
            
            Location landLoc = findFloor(loc);
            
            projectile.remove();
            spawn3DItem(landLoc, itemStack);
        }
    }

    private void handleNeighborHit(Player neighbor, Player shooter, int cmd) {
        SNPlayer neighborSnp = plugin.getGameManager().getPlayers().get(neighbor.getUniqueId());
        if (neighborSnp == null || !neighborSnp.isNeighbor()) return;

        // Deal damage to Neighbor based on item size/weight
        double damage = getItemDamage(cmd);
        neighbor.damage(damage, shooter);

        // Apply knockback
        Vector kb = neighbor.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize().multiply(0.55).setY(0.18);
        neighbor.setVelocity(kb);

        // Increment Rage charge
        neighborSnp.incrementHitsTaken();
        int rageHits = neighborSnp.getHitsTaken();
        plugin.getGameManager().updateNeighborRageItem(neighbor, rageHits);
        if (rageHits >= 7) {
            neighbor.sendMessage("§6§l[Rage] §c⚡ RAGE MODE READY! §eUse Rage item to activate!");
            neighbor.playSound(neighbor.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 0.5f);
        }

        // Cancel Grab if Neighbor is currently grabbing a child
        boolean rescued = false;
        for (SNPlayer childSnp : plugin.getGameManager().getPlayers().values()) {
            if (childSnp.isChild() && childSnp.isGrabbed() && childSnp.isAlive()) {
                childSnp.setGrabbed(false);
                Player child = Bukkit.getPlayer(childSnp.getUuid());
                if (child != null) {
                    child.sendMessage("§a§l[RESCUED!] §eA teammate saved you from the Neighbor!");
                    child.playSound(child.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    // Push child away from neighbor
                    Vector pushAway = child.getLocation().toVector().subtract(neighbor.getLocation().toVector()).normalize().multiply(0.8).setY(0.3);
                    child.setVelocity(pushAway);
                }
                neighbor.sendMessage("§c§l[Grab] §eYou were hit! The child was rescued!");
                shooter.sendMessage("§a§l✔ §eYou rescued §6" + childSnp.getName() + "§e from the Neighbor!");
                rescued = true;
                break;
            }
        }

        if (rescued) {
            plugin.getGameManager().broadcast("§a§l✔ " + shooter.getName() + " §7rescued a teammate by hitting the Neighbor!");
        }

        // Brave increment Counter-Strike charge
        SNPlayer shooterSnp = plugin.getGameManager().getPlayers().get(shooter.getUniqueId());
        if (shooterSnp != null && shooterSnp.getChildClass() == ChildClass.BRAVE) {
            shooterSnp.incrementHitsTaken();
            int braveHits = shooterSnp.getHitsTaken();
            shooter.sendMessage("§a§l✔ Brave struck the Neighbor! §7[Counter-Strike: " + braveHits + "/3]");
            if (braveHits >= 3) {
                shooter.sendMessage("§6§l⚡ COUNTER-STRIKE READY! §eYou will auto-escape the next grab!");
            }
        }
    }

    private void playHitSound(Location loc, int cmd) {
        Sound hitSound;
        float pitch = 1.0f;

        switch (cmd) {
            case 1100 -> { hitSound = Sound.BLOCK_WOOD_BREAK; pitch = 0.8f; }     // Box -> wood crunch
            case 1101 -> { hitSound = Sound.BLOCK_WOOD_BREAK; pitch = 0.6f; }     // Chair -> heavy wood
            case 1102 -> { hitSound = Sound.BLOCK_GLASS_BREAK; pitch = 1.2f; }    // TV -> glass shatter
            case 1103 -> { hitSound = Sound.BLOCK_WOOL_BREAK; pitch = 0.7f; }     // Sofa -> fabric thud
            case 1104 -> { hitSound = Sound.BLOCK_GLASS_BREAK; pitch = 0.9f; }    // Painting -> frame crack
            case 1105 -> { hitSound = Sound.ITEM_BOOK_PAGE_TURN; pitch = 0.5f; }  // Book -> pages flutter
            case 1106 -> { hitSound = Sound.ENTITY_SLIME_SQUISH; pitch = 1.0f; }  // Tomato -> splat
            case 1107 -> { hitSound = Sound.BLOCK_WOOD_BREAK; pitch = 1.1f; }     // Broom -> stick snap
            case 1108 -> { hitSound = Sound.ENTITY_SLIME_SQUISH; pitch = 1.5f; }  // Basketball -> rubber bounce
            case 1109 -> { hitSound = Sound.BLOCK_WOOL_BREAK; pitch = 1.2f; }     // Pillow -> poof
            case 1110 -> { hitSound = Sound.BLOCK_WOOL_BREAK; pitch = 1.0f; }     // Hat -> fabric flop
            // Keys
            case 1000, 1001, 1002, 1003, 1004, 1005 -> { hitSound = Sound.BLOCK_METAL_HIT; pitch = 1.6f; }
            // Keycards
            case 1014, 1015, 1016 -> { hitSound = Sound.BLOCK_BAMBOO_WOOD_HIT; pitch = 1.4f; }
            // Tools (Crowbar, Hammer, Wrench, Baseball Bat)
            case 1010, 1013, 1200, 1204 -> { hitSound = Sound.BLOCK_ANVIL_PLACE; pitch = 1.8f; }
            default -> { hitSound = Sound.BLOCK_WOOD_BREAK; pitch = 1.0f; }
        };

        loc.getWorld().playSound(loc, hitSound, 1.2f, pitch);

        // Secondary impact sound for weight
        if (cmd == 1101 || cmd == 1103 || cmd == 1100) {
            // Heavy items also play a thud
            loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.4f, 0.4f);
        }
    }

    /**
     * Finds the exact Y position of the floor surface below a given location.
     * Handles full blocks, slabs (top and bottom), carpets, stairs, and other non-full blocks
     * by using the block's actual collision bounding box height.
     */
    private Location findFloor(Location loc) {
        World world = loc.getWorld();
        double x = loc.getX();
        double z = loc.getZ();
        double startY = loc.getY();

        // Use raytrace straight down from the location to find the first solid surface
        RayTraceResult result = world.rayTraceBlocks(
                new Location(world, x, startY + 0.5, z),
                new Vector(0, -1, 0),
                12.0,
                FluidCollisionMode.NEVER,
                true
        );

        if (result != null && result.getHitBlock() != null) {
            Block hitBlock = result.getHitBlock();
            double surfaceY = getSurfaceY(hitBlock);
            return new Location(world, x, surfaceY, z);
        }

        // Fallback: scan block by block downward
        for (int dy = 1; dy > -12; dy--) {
            Block block = new Location(world, x, startY + dy, z).getBlock();
            if (block.getType().isSolid() || isNonFullSolid(block)) {
                double surfaceY = getSurfaceY(block);
                return new Location(world, x, surfaceY, z);
            }
        }
        return new Location(world, x, Math.floor(startY), z); // absolute fallback
    }

    /**
     * Gets the Y coordinate of the top surface of a block, accounting for
     * slabs, carpets, stairs, and other non-full blocks.
     */
    private double getSurfaceY(Block block) {
        String name = block.getType().name();
        double baseY = block.getY();

        // Bottom slabs
        if (name.contains("SLAB")) {
            org.bukkit.block.data.type.Slab slabData = (org.bukkit.block.data.type.Slab) block.getBlockData();
            if (slabData.getType() == org.bukkit.block.data.type.Slab.Type.TOP) {
                return baseY + 1.0;
            } else if (slabData.getType() == org.bukkit.block.data.type.Slab.Type.DOUBLE) {
                return baseY + 1.0;
            } else {
                return baseY + 0.5;
            }
        }

        // Carpet (1/16 of a block = 0.0625)
        if (name.contains("CARPET")) {
            return baseY + 0.0625;
        }

        // Daylight detector / pressure plates
        if (name.contains("DAYLIGHT") || name.contains("PRESSURE_PLATE")) {
            return baseY + 0.0625;
        }

        // Snow layers
        if (block.getType() == Material.SNOW) {
            org.bukkit.block.data.type.Snow snow = (org.bukkit.block.data.type.Snow) block.getBlockData();
            return baseY + (snow.getLayers() * 0.125);
        }

        // Trapdoors (when closed = flat on floor)
        if (name.contains("TRAPDOOR")) {
            org.bukkit.block.data.type.TrapDoor trap = (org.bukkit.block.data.type.TrapDoor) block.getBlockData();
            if (!trap.isOpen()) {
                if (trap.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM) {
                    return baseY + 0.1875;
                } else {
                    return baseY + 1.0;
                }
            }
        }

        // Beds
        if (name.contains("BED")) {
            return baseY + 0.5625;
        }

        // Default full block
        return baseY + 1.0;
    }

    /**
     * Check if a block is a non-full solid surface that items can rest on.
     */
    private boolean isNonFullSolid(Block block) {
        String name = block.getType().name();
        return name.contains("SLAB") || name.contains("CARPET") || name.contains("STAIR")
                || name.contains("TRAPDOOR") || name.contains("BED")
                || name.contains("PRESSURE_PLATE") || name.contains("DAYLIGHT")
                || block.getType() == Material.SNOW;
    }

    private void spawn3DItem(Location loc, ItemStack item) {
        World world = loc.getWorld();
        int cmd = 0;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            cmd = item.getItemMeta().getCustomModelData();
        }
        final int finalCmd = cmd;
        float scale = getItemScale(finalCmd);
        float halfHeight = getItemHalfHeight(finalCmd);

        // Place the entity so the bottom of the model sits on the floor surface.
        // FIXED ItemDisplay is centered at its location, so we raise by halfHeight.
        Location adjustedLoc = loc.clone().add(0, halfHeight, 0);
        
        world.spawn(adjustedLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.addScoreboardTag("sn_3d_item");
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);

            // Scale to realistic proportions
            org.bukkit.util.Transformation transform = entity.getTransformation();
            transform.getScale().set(scale, scale, scale);

            // Random yaw rotation for scattered look
            float yaw = rand.nextFloat() * 360f;
            // Pitch -90 = item lies flat on the ground like a fallen object
            // Add a small random tilt (±5°) for natural imperfection
            float pitch = -90f + (rand.nextFloat() * 10f - 5f);
            entity.setRotation(yaw, pitch);

            entity.setTransformation(transform);
        });
    }
}
