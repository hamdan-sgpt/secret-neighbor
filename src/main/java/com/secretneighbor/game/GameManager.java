package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.ChildClass;
import com.secretneighbor.player.Role;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {
    private final SecretNeighborPlugin plugin;
    private GameState state = GameState.IDLE;
    private final Set<UUID> invitedPlayers = new HashSet<>();
    private final Map<UUID, SNPlayer> players = new LinkedHashMap<>();
    private final Map<UUID, Location> offlineLocations = new HashMap<>();

    private BukkitTask gameTask;

    public Location getOfflineLocation(UUID uuid) { return offlineLocations.get(uuid); }
    public void removeOfflineLocation(UUID uuid) { offlineLocations.remove(uuid); }
    public void clearOfflineLocations() { offlineLocations.clear(); }
    private int gameTimeSeconds = 600;
    private int maxGameTimeSeconds = 600;
    private BossBar gameTimerBar;

    private UUID lobbyOwner;

    public UUID getLobbyOwner() { return lobbyOwner; }
    public void setLobbyOwner(UUID uuid) { this.lobbyOwner = uuid; }

    public void setGameTimeSeconds(int seconds) {
        this.gameTimeSeconds = seconds;
        this.maxGameTimeSeconds = seconds;
    }

    public int getGameTimeSeconds() {
        return gameTimeSeconds;
    }

    public GameManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public boolean isInvited(UUID uuid) { return invitedPlayers.contains(uuid); }
    public void addInvite(UUID uuid) { invitedPlayers.add(uuid); }
    public void removeInvite(UUID uuid) { invitedPlayers.remove(uuid); }

    public Set<UUID> getPlayerUUIDs() { return players.keySet(); }
    public Collection<SNPlayer> getSNPlayers() { return players.values(); }
    public Map<UUID, SNPlayer> getPlayers() { return players; }
    public SNPlayer getPlayer(UUID uuid) { return players.get(uuid); }
    public int getPlayerCount() { return players.size(); }

    // --- Lobby Phase ---

    public void createLobby(Player host) {
        resetAll();
        state = GameState.LOBBY;
        lobbyOwner = host.getUniqueId();
        addInvite(host.getUniqueId());
        addPlayer(host);
        broadcast("§6[§eSN§6] §aLobby has been created by " + host.getName() + "!");
    }

    public void addPlayer(Player player) {
        if (state == GameState.IDLE) {
            player.sendMessage("§cNo active lobby! Ask an admin/host to run /sn create first.");
            return;
        }
        if (state == GameState.IN_GAME || state == GameState.ENDING) {
            player.sendMessage("§cGame is already in progress!");
            return;
        }
        if (!isInvited(player.getUniqueId())) {
            player.sendMessage("§cYou have not been invited to this game!");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!players.containsKey(uuid)) {
            players.put(uuid, new SNPlayer(uuid, player.getName()));

            Location lobby = plugin.getLocationManager().getLobby();
            if (lobby != null) {
                player.teleport(lobby);
                player.sendMessage("§aTeleporting to Secret Neighbor lobby...");
            } else {
                player.sendMessage("§cWarning: Lobby spawn point has not been set yet!");
            }

            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getInventory().clear();
            giveLobbyItems(player);

            broadcast("§6[§eSN§6] §a" + player.getName() + " joined the game! (" + players.size() + "/10)");
        }
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (state == GameState.IN_GAME) {
            SNPlayer snp = players.get(uuid);
            if (snp != null) {
                if (snp.isChild() && snp.isAlive()) {
                    // Drop non-ability, non-locked items on the ground
                    Location dropLoc = player.getLocation();
                    org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(plugin, "sn_ability");
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            if (com.secretneighbor.listener.InventoryListener.isLockedPane(item)) continue;
                            
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null && meta.getPersistentDataContainer().has(abilityKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                                continue;
                            }
                            
                            dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                        }
                    }
                    
                    // Save offline location
                    offlineLocations.put(uuid, player.getLocation());

                    // Drop backpack items if Bagger
                    if (snp.getChildClass() == com.secretneighbor.player.ChildClass.BAGGER) {
                        if (plugin.getChildAbilitiesListener() != null) {
                            plugin.getChildAbilitiesListener().dropBackpackItems(player);
                        }
                    }
                }
                player.getInventory().clear();
                broadcast("§6[§eSN§6] §c" + player.getName() + " disconnected (can rejoin).");
                checkWinConditions();
            }
        } else {
            SNPlayer snp = players.remove(uuid);
            if (snp != null) {
                player.getInventory().clear();
                broadcast("§6[§eSN§6] §c" + player.getName() + " left the game.");
            }
        }
    }

    // --- Game Start ---

    public void startGame() {
        startGame(null);
    }

    public void startGame(String forceRole) {
        if (state != GameState.LOBBY) return;
        if (players.size() < 1) return;

        clearOfflineLocations();

        // Enter countdown phase first
        state = GameState.IN_GAME; // Set early so other systems know game is starting
        maxGameTimeSeconds = gameTimeSeconds;

        // Freeze all players during countdown
        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) {
                p.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 220, 127, false, false));
                p.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 220, 128, false, false));
            }
        }

        // 10-second countdown with dramatic effects
        final String savedForceRole = forceRole;
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    // Unfreeze all players
                    for (SNPlayer snp : players.values()) {
                        Player p = Bukkit.getPlayer(snp.getUuid());
                        if (p != null) {
                            p.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                            p.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
                        }
                    }
                    // GO!
                    for (SNPlayer snp : players.values()) {
                        Player p = Bukkit.getPlayer(snp.getUuid());
                        if (p != null) {
                            p.sendTitle("§c§lGO!", "§7Find the keys and escape!", 5, 40, 15);
                            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
                        }
                    }
                    // Now execute the actual game setup
                    executeGameStart(savedForceRole);
                    cancel();
                    return;
                }

                // Countdown effects
                String color;
                Sound sound;
                float pitch;
                float volume;

                if (countdown > 5) {
                    // 10-6: Yellow, calm tick
                    color = "§e§l";
                    sound = Sound.BLOCK_NOTE_BLOCK_HAT;
                    pitch = 1.0f;
                    volume = 0.8f;
                } else if (countdown > 2) {
                    // 5-3: Orange, faster tick
                    color = "§6§l";
                    sound = Sound.BLOCK_NOTE_BLOCK_PLING;
                    pitch = 1.0f + (5 - countdown) * 0.15f;
                    volume = 1.0f;
                } else {
                    // 2-1: Red, intense
                    color = "§c§l";
                    sound = Sound.BLOCK_NOTE_BLOCK_BELL;
                    pitch = 1.5f;
                    volume = 1.2f;
                }

                for (SNPlayer snp : players.values()) {
                    Player p = Bukkit.getPlayer(snp.getUuid());
                    if (p != null) {
                        p.sendTitle(color + countdown, "§7The game is about to begin...", 3, 18, 3);
                        p.playSound(p.getLocation(), sound, volume, pitch);
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Executes the actual game start logic after the countdown completes.
     * Assigns roles, teleports players, sets up items, creates BossBar, starts atmosphere.
     */
    private void executeGameStart(String forceRole) {
        List<SNPlayer> participantList = new ArrayList<>(players.values());

        if (forceRole != null && participantList.size() == 1) {
            SNPlayer snp = participantList.get(0);
            ChildClass[] classes = ChildClass.values();
            if (snp.getChildClass() == null) {
                snp.setChildClass(classes[new Random().nextInt(classes.length)]);
            }
            if (forceRole.equalsIgnoreCase("NEIGHBOR") || forceRole.equalsIgnoreCase("N")) {
                snp.setRole(Role.NEIGHBOR);
            } else {
                snp.setRole(Role.CHILD);
            }
        } else {
            // Assign roles: 1 Neighbor, rest Children
            int neighborIndex = new Random().nextInt(participantList.size());
            ChildClass[] classes = ChildClass.values();
            for (int i = 0; i < participantList.size(); i++) {
                SNPlayer snp = participantList.get(i);
                if (snp.getChildClass() == null) {
                    snp.setChildClass(classes[new Random().nextInt(classes.length)]);
                }
                if (i == neighborIndex) {
                    snp.setRole(Role.NEIGHBOR);
                } else {
                    snp.setRole(Role.CHILD);
                }
            }
        }

        // Save original skins
        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) {
                for (com.destroystokyo.paper.profile.ProfileProperty prop : p.getPlayerProfile().getProperties()) {
                    if (prop.getName().equals("textures")) {
                        snp.setOriginalSkinValue(prop.getValue());
                        snp.setOriginalSkinSignature(prop.getSignature());
                        break;
                    }
                }
            }
        }

        // Teleport and setup each player
        Location entrance = plugin.getLocationManager().getHome();
        if (entrance == null) {
            entrance = plugin.getLocationManager().getHouseEntrance();
        }

        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p == null) continue;

            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20);
            p.setFoodLevel(20);
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            if (entrance != null) {
                p.teleport(entrance);
            }

            if (snp.isNeighbor()) {
                setupNeighbor(p, snp);
            } else {
                setupChild(p, snp);
            }
        }

        // Auto-spawn preset drawers at hardcoded locations
        spawnPresetDrawers();

        Location basementDoor = plugin.getLocationManager().getBasementDoor();
        if (basementDoor != null) {
            org.bukkit.block.Block block = basementDoor.getBlock();
            org.bukkit.block.BlockFace facing = org.bukkit.block.BlockFace.NORTH;
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
                facing = door.getFacing();
            } else if (block.getRelative(0, 1, 0).getBlockData() instanceof org.bukkit.block.data.type.Door door) {
                facing = door.getFacing();
            }

            // Force closed Iron Door facing correct direction
            org.bukkit.block.data.type.Door bottomDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.IRON_DOOR);
            bottomDoorData.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
            bottomDoorData.setFacing(facing);
            bottomDoorData.setOpen(false);

            org.bukkit.block.data.type.Door topDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.IRON_DOOR);
            topDoorData.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            topDoorData.setFacing(facing);
            topDoorData.setOpen(false);

            block.setType(Material.IRON_DOOR, false);
            Location topLoc = basementDoor.clone().add(0, 1, 0);
            topLoc.getBlock().setType(Material.IRON_DOOR, false);

            topLoc.getBlock().setBlockData(topDoorData, false);
            block.setBlockData(bottomDoorData, true);

            plugin.getKeyManager().clear();
            plugin.getKeyManager().spawnPadlocks(basementDoor);
        }

        startGameTicking();

        // Create BossBar timer
        gameTimerBar = Bukkit.createBossBar("⏰ 10:00 — Game in progress", BarColor.GREEN, BarStyle.SOLID);
        gameTimerBar.setProgress(1.0);
        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) gameTimerBar.addPlayer(p);
        }

        // Start atmosphere system (footsteps, heartbeat, ambient sounds, proximity)
        if (plugin.getAtmosphereManager() != null) {
            plugin.getAtmosphereManager().start();
        }
    }

    public void setupNeighbor(Player p, SNPlayer snp) {
        p.sendMessage("§c§lYOU ARE THE NEIGHBOR!");
        p.sendMessage("§7Goal: Prevent the children from escaping and capture them all.");
        p.sendTitle("§c§lNEIGHBOR", "§7Prevent escape at all costs!", 10, 70, 20);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.8f);

        p.sendMessage("§7» §4Bear Trap: §fShift + Right-Click (tangan kosong) untuk pasang jebakan (max 3).");
        p.sendMessage("§7» §dDisguise: §fShift + Left-Click (tangan kosong) untuk menyamar jadi child random.");
        p.sendMessage("§7» §c[Shift + F] Bapake: §fTekan Shift + F untuk reveal jadi Neighbor (Kill Mode). Tekan F kembali untuk menyamar kembali.");
        p.sendMessage("§7» §c✋ Grab: §fRight-Click (tangan kosong) di Bapake mode untuk grab anak.");
        p.sendMessage("§7» §6⚡ Rage: §fItem Rage akan muncul di hotbar saat di Bapake mode.");

        // Start as yourself (not disguised, not bapakeMode)
        snp.setDisguised(false);
        snp.setBapakeMode(false);
        snp.setDisguiseValue(null);
        snp.setDisguiseSignature(null);
        snp.setDisguiseName(null);
        snp.resetHitsTaken(); // Reset rage charge

        // Name is their original name at start
        p.displayName(Component.text(p.getName()));
        p.playerListName(Component.text(p.getName()));

        updateNeighborInventoryVisuals(p, snp);
        com.secretneighbor.listener.InventoryListener.lockPlayerInventorySlots(p, snp);
    }

    private void setupChild(Player p, SNPlayer snp) {
        ChildClass cc = snp.getChildClass();
        p.sendMessage("§a§lYOU ARE A CHILD (" + cc.getDisplayName() + ")!");
        p.sendMessage("§7" + cc.getDescription());
        p.sendMessage("§7Goal: Search for keys, insert them at the basement door, and escape!");
        p.sendTitle("§a§lCHILD", "§7" + cc.getDisplayName() + " — Search & Escape!", 10, 70, 20);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);

        // Give class-specific item + passives
        switch (cc) {
            case BRAVE:
                p.sendMessage("§7» §cBaseball Bat: §fTekan F untuk ayunkan bat dan knock back Neighbor.");
                p.sendMessage("§7» §6Counter-Strike: §fHit Neighbor 3x to auto-escape grab.");
                break;
            case DETECTIVE:
                p.sendMessage("§7» §6Field Intel: §fTekan F untuk locate the nearest key.");
                p.sendMessage("§7» §eIntuition: §fAfter 2 keys inserted, remaining keys glow.");
                break;
            case BAGGER:
                // Strong Knees: Resistance I (reduce fall/overall damage)
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 999999, 0, false, false));
                p.sendMessage("§7» §d3 Slots: §fMembawa hingga 3 item utama (hotbar).");
                p.sendMessage("§7» §aStrong Knees: §fReduced damage (Resistance I).");
                p.sendMessage("§7» §eHeavy Bones: §fNeighbor moves slower carrying you.");
                break;
            case LEADER:
                p.sendMessage("§7» §eInspiration: §fTekan F untuk speed boost nearby allies. Blinds Neighbor if close.");
                p.sendMessage("§7» §aStress Resistance: §fReduced stun durations.");
                break;
            case INVENTOR:
                p.sendMessage("§7» §9Metal Detector: §fTekan F untuk find nearest drawer with items.");
                p.sendMessage("§7» §eTinkering: §fIdentifies key contents through walls.");
                break;
            case SCOUT:
                p.sendMessage("§7» §aSlingshot: §fTekan F untuk shoot nuts to stun the Neighbor.");
                break;
            case ENGINEER:
                p.sendMessage("§7» §eTripwire: §fTekan F sambil melihat block untuk pasang sensor (max 3).");
                p.sendMessage("§7» §cTrap: §fNeighbor gets stunned when triggered.");
                break;
            case GAMER:
                // Quick Reflexes: Permanent Speed I
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 999999, 0, false, false));
                p.sendMessage("§7» §bDecoy: §fTekan F untuk place a hologram decoy.");
                p.sendMessage("§7» §aQuick Reflexes: §fPermanent Speed I.");
                break;
            default:
                break;
        }
        
        // Initialize locked inventory slots immediately
        com.secretneighbor.listener.InventoryListener.lockPlayerInventorySlots(p, snp);
    }

    // --- Game Loop ---

    private void startGameTicking() {
        if (gameTask != null) gameTask.cancel();

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_GAME) { cancel(); return; }

                if (gameTimeSeconds <= 0) {
                    endGame(Role.NEIGHBOR); // Time up = Neighbor wins
                    cancel();
                    return;
                }

                int mins = gameTimeSeconds / 60;
                int secs = gameTimeSeconds % 60;
                String timeStr = String.format("%02d:%02d", mins, secs);

                // Count alive children
                int childrenAlive = 0;
                for (SNPlayer snp : players.values()) {
                    if (snp.isChild() && snp.isAlive()) childrenAlive++;
                }

                // Update BossBar
                if (gameTimerBar != null) {
                    double progress = (double) gameTimeSeconds / maxGameTimeSeconds;
                    gameTimerBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                    // Color changes based on time remaining
                    if (progress > 0.5) {
                        gameTimerBar.setColor(BarColor.GREEN);
                    } else if (progress > 0.25) {
                        gameTimerBar.setColor(BarColor.YELLOW);
                    } else {
                        gameTimerBar.setColor(BarColor.RED);
                    }

                    String keysInfo = "";
                    if (plugin.getKeyManager() != null) {
                        int inserted = plugin.getKeyManager().getKeysInserted();
                        keysInfo = " §7| §f🔑 " + inserted + "/6 Keys";
                    }

                    gameTimerBar.setTitle("§e⏰ " + timeStr + " §7| §f👧 " + childrenAlive + " Children Alive" + keysInfo);
                }

                // Trap decay per player
                for (SNPlayer snp : players.values()) {
                    Player p = Bukkit.getPlayer(snp.getUuid());
                    if (p == null) continue;

                    if (snp.isTrapped()) {
                        snp.decrementTrapTicks();
                        if (snp.getTrapTicks() <= 0) {
                            snp.setTrapped(false);
                            p.sendMessage("§aYou broke free from the trap!");
                        }
                    }

                    // Display Cooldown HUD
                    StringBuilder hud = new StringBuilder();
                    if (snp.isNeighbor()) {
                        // Neighbor Cooldowns: Mask, Bear Trap, Rage
                        if (snp.isOnCooldown("mask")) {
                            hud.append("§eMask: §c").append(snp.getRemainingCooldown("mask")).append("s");
                        } else {
                            hud.append("§eMask: §aReady");
                        }
                        hud.append(" §8| ");
                        if (snp.isOnCooldown("bear_trap")) {
                            hud.append("§eTrap: §c").append(snp.getRemainingCooldown("bear_trap")).append("s");
                        } else {
                            hud.append("§eTrap: §aReady");
                        }
                        hud.append(" §8| ");
                        if (snp.isOnCooldown("rage")) {
                            hud.append("§eRage: §c").append(snp.getRemainingCooldown("rage")).append("s");
                        } else if (snp.getHitsTaken() >= 7) {
                            hud.append("§eRage: §aREADY");
                        } else {
                            hud.append("§eRage: §7").append(snp.getHitsTaken()).append("/7 Hits");
                        }
                    } else if (snp.isChild() && snp.getChildClass() != null) {
                        String abilityName = null;
                        String cdKey = null;
                        switch (snp.getChildClass()) {
                            case LEADER: abilityName = "Inspiration"; cdKey = "leader_inspiration"; break;
                            case INVENTOR: abilityName = "Metal Detector"; cdKey = "inventor_detector"; break;
                            case SCOUT: abilityName = "Slingshot"; cdKey = "scout_slingshot"; break;
                            case ENGINEER: abilityName = "Tripwire"; cdKey = "engineer_tripwire"; break;
                            case GAMER: abilityName = "Decoy"; cdKey = "gamer_decoy"; break;
                            default: break;
                        }
                        if (abilityName != null && cdKey != null) {
                            if (snp.isOnCooldown(cdKey)) {
                                hud.append("§b").append(abilityName).append(": §c").append(snp.getRemainingCooldown(cdKey)).append("s");
                            } else {
                                hud.append("§b").append(abilityName).append(": §aReady (F)");
                            }
                        }
                    }

                    if (hud.length() > 0) {
                        p.sendActionBar(Component.text(hud.toString()));
                    }
                }

                checkWinConditions();
                gameTimeSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // --- Win Conditions ---

    public void checkWinConditions() {
        if (state != GameState.IN_GAME) return;

        int onlineChildrenAlive = 0;
        int onlineNeighborsAlive = 0;

        for (SNPlayer snp : players.values()) {
            if (snp.isAlive()) {
                Player p = Bukkit.getPlayer(snp.getUuid());
                if (p != null && p.isOnline()) {
                    if (snp.isNeighbor()) onlineNeighborsAlive++;
                    else onlineChildrenAlive++;
                }
            }
        }

        if (onlineChildrenAlive == 0) {
            endGame(Role.NEIGHBOR);
        } else if (onlineNeighborsAlive == 0) {
            endGame(Role.CHILD);
        }
    }

    public void childrenEscape() {
        if (state == GameState.IN_GAME) {
            endGame(Role.CHILD);
        }
    }

    public void endGame(Role winner) {
        state = GameState.ENDING;
        clearOfflineLocations();
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }

        // Remove BossBar
        if (gameTimerBar != null) {
            gameTimerBar.removeAll();
            gameTimerBar = null;
        }

        // Stop atmosphere
        if (plugin.getAtmosphereManager() != null) {
            plugin.getAtmosphereManager().stop();
        }

        if (plugin.getKeyManager() != null) {
            plugin.getKeyManager().clearPadlocks();
        }

        String winnerName = winner == Role.CHILD ? "§a§lCHILDREN WIN!" : "§c§lNEIGHBOR WINS!";
        String subtitle = winner == Role.CHILD ? "§7The children escaped!" : "§7All children have been captured!";

        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p == null) continue;

            p.sendTitle(winnerName, subtitle, 10, 100, 30);
            p.sendMessage("§6§l===========================");
            p.sendMessage("§6  " + winnerName);
            p.sendMessage("§6§l===========================");

            if (winner == Role.CHILD) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
            }
        }

        // Reset after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getKeyManager() != null) {
                plugin.getKeyManager().clear();
            }
            java.util.Iterator<Map.Entry<UUID, SNPlayer>> iterator = players.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, SNPlayer> entry = iterator.next();
                SNPlayer snp = entry.getValue();
                Player p = Bukkit.getPlayer(snp.getUuid());
                if (p != null && p.isOnline()) {
                    p.getInventory().clear();
                    giveLobbyItems(p);
                    p.setGameMode(GameMode.ADVENTURE);
                    p.setHealth(20);
                    p.setFoodLevel(20);
                    for (PotionEffect effect : p.getActivePotionEffects()) {
                        p.removePotionEffect(effect.getType());
                    }
                    // Reset display name
                    p.displayName(Component.text(p.getName()));
                    p.playerListName(Component.text(p.getName()));

                    // Restore original skin
                    if (snp.getOriginalSkinValue() != null && snp.getOriginalSkinSignature() != null) {
                        setPlayerSkin(p, snp.getOriginalSkinValue(), snp.getOriginalSkinSignature());
                    }

                    // Reset scale
                    org.bukkit.attribute.AttributeInstance scaleInstance = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                    if (scaleInstance != null) {
                        scaleInstance.setBaseValue(1.0);
                    }

                    // Teleport to default world safety first while we regenerate the map
                    org.bukkit.World defaultWorld = Bukkit.getWorlds().get(0);
                    p.teleport(defaultWorld.getSpawnLocation());
                    snp.reset();
                } else {
                    iterator.remove();
                }
            }
            state = GameState.LOBBY;
            broadcast("§6[§eSN§6] §eGame ended! Lobby is open for a new round.");
            broadcast("§eRegenerating map... Please wait.");
            new com.secretneighbor.map.MapGenerator(plugin).generate(Bukkit.getConsoleSender());
        }, 200L); // 10 seconds
    }

    public void forceStopGame() {
        if (state == GameState.IDLE) return;

        state = GameState.ENDING;
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }

        if (gameTimerBar != null) {
            gameTimerBar.removeAll();
            gameTimerBar = null;
        }

        if (plugin.getAtmosphereManager() != null) {
            plugin.getAtmosphereManager().stop();
        }

        if (plugin.getKeyManager() != null) {
            plugin.getKeyManager().clearPadlocks();
        }

        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
                p.setHealth(20);
                p.setFoodLevel(20);
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
                p.displayName(Component.text(p.getName()));
                p.playerListName(Component.text(p.getName()));

                if (snp.getOriginalSkinValue() != null && snp.getOriginalSkinSignature() != null) {
                    setPlayerSkin(p, snp.getOriginalSkinValue(), snp.getOriginalSkinSignature());
                }

                org.bukkit.attribute.AttributeInstance scaleInstance = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                if (scaleInstance != null) {
                    scaleInstance.setBaseValue(1.0);
                }

                org.bukkit.World defaultWorld = Bukkit.getWorlds().get(0);
                p.teleport(defaultWorld.getSpawnLocation());
                giveLobbyItems(p);
            }
            snp.reset();
        }

        if (plugin.getKeyManager() != null) {
            plugin.getKeyManager().clear();
        }

        state = GameState.LOBBY;
        broadcast("§c§l[SN] Permainan dihentikan paksa oleh Admin!");
        broadcast("§eMeregenerasi map... Mohon tunggu.");
        new com.secretneighbor.map.MapGenerator(plugin).generate(Bukkit.getConsoleSender());
    }

    // --- Preset Drawer Spawning ---

    /**
     * Spawns all preset drawers at hardcoded map coordinates.
     * Called at the start of each game so drawers don't need to be placed manually.
     * Facing yaw: north=180, south=0, west=90, east=-90
     */
    public void spawnPresetDrawers() {
        String worldName = plugin.getConfig().getString("world-name", "secret_neighbor_map_2");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[SN] Cannot spawn preset drawers: world '" + worldName + "' not loaded!");
            return;
        }

        // Clean up old drawers from the world if any exist to prevent duplication
        plugin.getDrawerManager().clearDrawersFromWorld(world);

        // {x, y, z, yaw} — Y sudah dikurangin 1 dari posisi player (block di bawah)
        double[][] longAltDrawerData = {
            {-29.480, -53.0, -14.304, 180},  // 1. north
            {-37.309, -53.0, -17.514,  90},  // 2. west
            {-55.667, -53.0,   0.300,   0},  // 3. south
            {-36.569, -47.0, -10.695,   0},  // 4. south
            {-35.560, -41.0,  13.300,   0},  // 5. south
            {-19.300, -35.0, -18.551,  90},  // 6. west
            {-57.700, -59.0,  -1.559, -90},  // 7. east
            {-41.300, -59.0,  -8.509,  90},  // 8. west
            {-24.654, -59.0, -14.300, 180},  // 9. north
        };

        double[][] regularDrawerData = {
            {-27.510, -59.0, 5.303, 0},       // south
            {-49.700, -29.0, -21.322, -90},   // east
            {-24.463, -41.0, -14.657, 0},     // south
            {-22.514, -41.0, -2.529, 90},     // west
            {-23.486, -35.0, -23.700, 0},     // south
            {-32.700, -53.0, -7.557, -90},    // east
            {-24.292, -59.0, 14.415, 180},    // north
            {-23.441, -59.0, 5.398, 0},       // south
            {-24.442, -59.0, 5.378, 0},       // south
            {-26.533, -59.0, 5.300, 0},       // south
            {-26.359, -59.0, -19.661, 0},     // south
            {-48.488, -51.0, 8.692, 180},     // north
            {-57.595, -53.0, 7.563, -90},     // east
            {-40.695, -53.0, -21.524, -90},   // east
            {-48.406, -53.0, -24.700, 0},     // south
            {-47.276, -53.0, -35.650, 0},     // south
            {-39.571, -59.0, -6.484, -90},    // east
            {-30.390, -59.0, -4.470, 90},     // west
            {-30.301, -58.0, -6.527, 90},     // west
            {-30.300, -58.0, -9.619, 90},     // west
            {-57.700, -53.0, -8.544, -90},    // east
            {-57.635, -44.0, 7.626, -90},     // east
            {-56.615, -47.0, 8.688, 180},     // north
            {-47.393, -40.0, -16.700, 0},     // south
            {-49.491, -39.0, -16.700, 0},     // south
            {-42.687, -42.0, -10.491, -90},   // east
            {-44.528, -59.0, -13.573, 0},     // south
            {-45.521, -59.0, -13.700, 0},     // south
            {-57.700, -59.0, -9.518, -90},    // east
            {-46.697, -59.0, 14.473, -90},    // east
            {-32.300, -44.0, -10.517, 90},    // west
            {-22.300, -47.0, -7.483, 90},     // west
            {-27.651, -45.0, 7.300, 0},       // south
            {-30.428, -41.0, -12.563, -90},   // east
            {-30.510, -41.0, -11.442, -90},   // east
            {-41.455, -58.0, -4.700, 0},      // south
            {-26.300, -38.9375, 8.700, 90},   // west
            {-28.700, -41.0, 18.537, -90},    // east
            {-48.395, -47.0, 17.686, 180},    // north
            {-47.372, -47.0, 17.432, 180},    // north
            {-57.624, -47.0, 10.508, -90},    // east
            {-57.499, -47.0, 11.570, -90},    // east
            {-61.539, -47.0, 9.300, 0},       // south
            {-62.676, -47.0, 9.300, 0},        // south
            {-32.412, -40.0, -23.700, 0},     // south
            {-34.537, -39.0, -23.686, 0},     // south
            {-27.443, -53.0, -23.700, 0},     // south
            // Baru dari screenshot batch 2 (Y sudah dikurangi 1)
            {-19.489, -53.0, -14.301, 180},   // north
            {-13.419, -53.0, -17.541, 90},    // west
            {-19.654, -53.0, -21.379, -90},   // east
            {-17.504, -51.0, -23.670, 0},     // south
            {-21.487, -51.0, -23.700, 0},      // south
            // Baru dari screenshot batch 3 (Y sudah dikurangi 1)
            {-35.699, -53.0, -21.425, -90},   // east
            {-35.700, -53.0, -23.670, -90},   // east
            {-42.300, -53.0, -14.490, 90},    // west
            {-52.700, -53.0, -29.534, -90},   // east
            // Baru dari screenshot batch 4 (Y sudah dikurangi 1)
            {-20.506, -57.0, 14.687, 180},    // north
            {-29.700, -57.0, 8.357, -90},     // east
            {-36.300, -56.0, 10.381, 90},     // west
            {-33.462, -47.0, -10.700, 0},     // south
            {-45.356, -47.0, -16.649, 0},      // south
            {-55.554, -40.0, -11.379, 180},    // north
            // Baru dari koordinat screenshot 2 (-56.552, -39.0, -11.318, facing north)
            {-56.552, -40.0, -11.318, 180},    // north
            // Baru dari koordinat screenshot (-17.561, -58.0, 12.583, facing west)
            {-17.561, -59.0, 12.583, 90},      // west
            // Baru dari koordinat screenshot 3 (-60.688, -39.0, -31.799, facing east)
            {-60.688, -40.0, -31.799, -90},    // east
            // Baru dari koordinat screenshot 4 (-30.467, -34.0, -12.437, facing east)
            {-30.467, -35.0, -12.437, -90},    // east
            // Baru dari koordinat screenshot 5 (-30.518, -34.0, -11.472, facing east)
            {-30.518, -35.0, -11.472, -90},    // east
            // Baru dari koordinat screenshot 6 & 7 (-27.529, -34.0, -23.528 and -28.511, -34.0, -23.700, facing south)
            {-27.529, -35.0, -23.528, 0},      // south
            {-28.511, -35.0, -23.700, 0},      // south
            // Baru dari koordinat screenshot 8 (-40.514, -28.0, -28.596, facing south)
            {-40.514, -29.0, -28.596, 0},      // south
            // Baru dari koordinat screenshot 9 & 10 (-30.426, -39.0, 20.471 and -30.359, -39.0, 19.476, facing west)
            {-30.426, -40.0, 20.471, 90},       // west
            {-30.359, -40.0, 19.476, 90}        // west
        };

        double[][] longDrawerData = {
            {-48.596, -59.0, -29.499, 0},     // south
            {-47.621, -59.0, -17.456, -90},   // east
            {-36.300, -59.0, 7.582, 90},      // west
            {-39.619, -41.0, 10.359, -90},    // east
            // Baru dari screenshot batch 6 (Y sudah dikurangi 1)
            {-32.329, -47.0, -1.700, 0},      // south
            {-50.605, -47.0, -16.555, 0},     // south
            {-37.607, -59.0, -19.700, 0},     // south
            // Baru dari screenshot batch 7 (Y sudah dikurangi 1)
            {-50.588, -53.0, -13.311, 180},   // north
            {-46.500, -53.0, -13.466, 180}    // north
        };

        int longCount = 0;
        for (double[] d : longAltDrawerData) {
            Location loc = new Location(world, d[0], d[1], d[2], (float) d[3], 0f);
            plugin.getDrawerManager().spawnLongAltDrawer(loc);
            longCount++;
        }

        int longRegCount = 0;
        for (double[] d : longDrawerData) {
            Location loc = new Location(world, d[0], d[1], d[2], (float) d[3], 0f);
            plugin.getDrawerManager().spawnLongDrawer(loc);
            longRegCount++;
        }

        int regCount = 0;
        for (double[] d : regularDrawerData) {
            Location loc = new Location(world, d[0], d[1], d[2], (float) d[3], 0f);
            plugin.getDrawerManager().spawnDrawer(loc);
            regCount++;
        }

        plugin.getLogger().info("[SN] Auto-spawned " + longCount + " preset long alt, " + longRegCount + " preset long regular, and " + regCount + " preset regular drawers.");
    }

    // --- Utilities ---

    private ItemStack createItem(String name, int customModelData) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.setCustomModelData(customModelData);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void resetAll() {
        state = GameState.IDLE;
        lobbyOwner = null;
        gameTimeSeconds = plugin.getConfig().getInt("game-duration-seconds", 600);
        maxGameTimeSeconds = gameTimeSeconds;
        invitedPlayers.clear();
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }

        // Clean up BossBar
        if (gameTimerBar != null) {
            gameTimerBar.removeAll();
            gameTimerBar = null;
        }

        // Stop atmosphere
        if (plugin.getAtmosphereManager() != null) {
            plugin.getAtmosphereManager().stop();
        }

        if (plugin.getKeyManager() != null) {
            plugin.getKeyManager().clear();
        }

        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) {
                p.getInventory().clear();
                p.displayName(Component.text(p.getName()));
                p.playerListName(Component.text(p.getName()));
                if (snp.getOriginalSkinValue() != null && snp.getOriginalSkinSignature() != null) {
                    setPlayerSkin(p, snp.getOriginalSkinValue(), snp.getOriginalSkinSignature());
                }
                org.bukkit.attribute.AttributeInstance scaleInstance = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                if (scaleInstance != null) {
                    scaleInstance.setBaseValue(1.0);
                }
                p.sendMessage("§eLobby has been reset. You have been removed from the queue.");
            }
        }
        players.clear();
    }

    public void broadcast(String message) {
        for (SNPlayer snp : players.values()) {
            Player p = Bukkit.getPlayer(snp.getUuid());
            if (p != null) p.sendMessage(message);
        }
        plugin.getLogger().info(message);
    }

    public static final String NEIGHBOR_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc4MjcwMzk1MDI3NiwKICAicHJvZmlsZUlkIiA6ICJlNzVjZTk1MTc1OWI0MmVlYmY4ZmVjZDhhYjM0YjM2MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJfS0tYRF8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWM5NWZiZjY4ZGIyY2UzYTI4MWJiZjhmNDk2OTY1ODY3MWQzODA5YTE2ZGFhOGQxODczMTkyODNkZGI4ZSIKICAgIH0KICB9Cn0=";
    public static final String NEIGHBOR_SKIN_SIGNATURE = "je09BsKyYqYH72/YU8WQEKMVxXqIiRYQ072j5SkgpgRGk3Kmh2Vc/ZQHgBVPfO/aBJTykFlCosHUpU/MLsqd/Fyi6ZxCQPj6vk7VG6oKvw1HG0ucJlUznnKzOgieKyP2WZ9byf0I+BSj64PP2/0keivSesgRjnoX6/vkicAUIooOb3G/dn4uwX+iIWZzI+UwGstZg2RlJKmer2jBSO5dtzborbJrpNEO7UM2TbYmLpntWeTS1POjolBONIK+qEE8pVq2c90zOdKTgOJQD5OpHNui6hhsC9+oQ8rEIJVaVkDm+l1xewmbCMQJ3cd1zc9a9xaqJe7BFy/2fiKglJUslOCosYs77DnMlDTF9NX+OIPlY5m9IVaz3yAnwRWlW+YfPxq61Pard/kM9TBZm1NrdE1JffXwwHMhWheXMm8zY5csdEMSR5rGwqHCRFxgh3Bwvm/B4rg3giPWkO1ISFa0gOAgTr0EZsBpj7IJrISkBITzODN2AjSc8/4H/PKLAG1eH7AvX7qihZmk/VaUkKHVXgB3dxDM4H3yqTwwpxwvvp9i/6XKNvPOjygMEsAxEsZRhAPxvv2RHeTgXHVkF05HXfrxA6iqkQa1L+KbSVj9YBkb1ZdJ7+Hw5KfKYA1jUA1uNyDv7vdoJMmkbb6Bd7tiO+uf1oVhKZPTxmxZyKq/9cY=";

    public void setPlayerSkin(Player player, String value, String signature) {
        com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
        profile.clearProperties();
        profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", value, signature));
        player.setPlayerProfile(profile);

        // Refresh for everyone
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            online.hidePlayer(plugin, player);
            online.showPlayer(plugin, player);
        }
    }

    public void copyPlayerSkin(Player neighbor, Player victim) {
        com.destroystokyo.paper.profile.PlayerProfile victimProfile = victim.getPlayerProfile();
        String value = null;
        String sig = null;
        for (com.destroystokyo.paper.profile.ProfileProperty prop : victimProfile.getProperties()) {
            if (prop.getName().equals("textures")) {
                value = prop.getValue();
                sig = prop.getSignature();
                break;
            }
        }
        if (value != null && sig != null) {
            setPlayerSkin(neighbor, value, sig);
        }
    }

    public ItemStack createNeighborAbilityItem(String name, int customModelData, String abilityTag) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.setCustomModelData(customModelData);
            meta.setUnbreakable(true);

            // Set PersistentDataContainer tag
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "sn_ability");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, abilityTag);

            item.setItemMeta(meta);
        }
        return item;
    }

    public void updateNeighborInventoryVisuals(Player player, SNPlayer snp) {
        if (snp == null || !snp.isNeighbor()) return;

        boolean hideItems = !snp.isBapakeMode();
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "sn_ability");
            if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String ability = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (ability == null) continue;

                int cmd = 0;
                String displayName = "";
                List<Component> lore = new ArrayList<>();

                if (hideItems) {
                    // Under disguise: use fake child models, names and descriptive Indonesian lore
                    lore.add(Component.text("§7[Disguised - Kemampuan Neighbor]"));
                    switch (ability) {
                        case "bear_trap" -> {
                            cmd = 1003; // Yellow Key
                            displayName = "§eYellow Key";
                            lore.add(Component.text("§7Klik kanan untuk menaruh §4Bear Trap§7."));
                        }

                        case "rage" -> {
                            cmd = 1011; // Flashlight
                            displayName = "§fFlashlight";
                            int rageHits = snp.getHitsTaken();
                            if (rageHits >= 7) {
                                lore.add(Component.text("§c⚡ RAGE MODE SIAP! §7Klik kanan untuk pakai."));
                            } else {
                                lore.add(Component.text("§7Gunakan untuk aktifkan §6Rage §7(" + rageHits + "/7)."));
                            }
                        }
                        case "disguise" -> {
                            cmd = 1014; // Keycard Lvl 1
                            displayName = "§bKeycard Lvl 1";
                            lore.add(Component.text("§7Gunakan untuk melepas samaran."));
                        }

                    }
                } else {
                    // Bapake mode: reveal actual neighbor models and names
                    switch (ability) {
                        case "bear_trap" -> {
                            cmd = 1300;
                            displayName = "§4🪤 Bear Trap";
                        }

                        case "rage" -> {
                            int rageHits = snp.getHitsTaken();
                            cmd = 1304;
                            displayName = rageHits >= 7 ? "§c⚡ RAGE MODE READY!" : "§6⚡ Rage (" + rageHits + "/7)";
                        }
                        case "disguise" -> {
                            cmd = 1301;
                            displayName = snp.isDisguised() ? "§dDisguise Mask (Return to Self)" : "§dDisguise Mask (Disguise as Player)";
                        }

                    }
                }

                meta.displayName(Component.text(displayName));
                meta.lore(lore);
                meta.setCustomModelData(cmd);
                item.setItemMeta(meta);
            }
        }
    }

    public void updateNeighborRageItem(Player p, int hits) {
        SNPlayer snp = players.get(p.getUniqueId());
        updateNeighborInventoryVisuals(p, snp);
    }

    public ItemStack getClassSelectorItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§e§lPILIH KELAS (Right-Click)"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Klik kanan untuk memilih kelas Anda!"));
            meta.lore(lore);
            meta.setCustomModelData(8888);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getTimeSelectorItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§b§lDURASI GAME (Admin)"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Klik kanan untuk mengatur"));
            lore.add(Component.text("§7durasi permainan (menit)."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveLobbyItems(Player player) {
        player.getInventory().setItem(4, getClassSelectorItem());
        if (lobbyOwner != null && lobbyOwner.equals(player.getUniqueId())) {
            player.getInventory().setItem(8, getTimeSelectorItem());
        }
    }
}
