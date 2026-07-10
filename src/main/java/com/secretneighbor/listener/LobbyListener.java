package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.SNPlayer;
import com.secretneighbor.player.ChildClass;
import com.secretneighbor.game.GameManager;
import com.secretneighbor.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LobbyListener implements Listener {
    private final SecretNeighborPlugin plugin;

    public LobbyListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Send the custom resource pack containing our 3D models and 128x128 textures
        plugin.sendResourcePack(player);

        GameManager gm = plugin.getGameManager();
        if (gm.getState() == GameState.IN_GAME) {
            SNPlayer snp = gm.getPlayers().get(player.getUniqueId());
            if (snp != null) {
                player.sendMessage("§a§l[Rejoin] §eYou rejoined the active game!");
                
                // Teleport back to last offline location if exists
                Location loc = gm.getOfflineLocation(player.getUniqueId());
                if (loc != null) {
                    player.teleport(loc);
                    gm.removeOfflineLocation(player.getUniqueId());
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                }

                // Restore attributes
                if (snp.isNeighbor()) {
                    if (snp.getDisguiseValue() != null && snp.getDisguiseSignature() != null) {
                        gm.setPlayerSkin(player, snp.getDisguiseValue(), snp.getDisguiseSignature());
                    } else {
                        // Bapake mode skin
                        gm.setPlayerSkin(player, GameManager.NEIGHBOR_SKIN_VALUE, GameManager.NEIGHBOR_SKIN_SIGNATURE);
                    }
                    
                    if (snp.isBapakeMode()) {
                        org.bukkit.attribute.AttributeInstance scaleInstance = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                        if (scaleInstance != null) {
                            scaleInstance.setBaseValue(1.3);
                        }
                    }
                    player.setHealth(20.0);
                    gm.updateNeighborInventoryVisuals(player, snp);
                } else {
                    // Child role
                    // Ensure normal scale (1.0)
                    org.bukkit.attribute.AttributeInstance scaleInstance = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
                    if (scaleInstance != null) {
                        scaleInstance.setBaseValue(1.0);
                    }
                    
                    if (snp.isKnocked()) {
                        player.setHealth(20.0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 5, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 99999, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 5, false, false));
                    } else {
                        player.setHealth(20.0);
                    }
                    
                    // Lock inventory and items
                    InventoryListener.lockPlayerInventorySlots(player, snp);
                }
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage("§7An active game is in progress. You are spectating.");
            }
        } else if (gm.getState() == GameState.LOBBY) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            gm.giveLobbyItems(player);
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGameManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null) {
            if (item.getType() == Material.COMPASS) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 8888) {
                    event.setCancelled(true);
                    openClassSelector(player);
                }
            } else if (item.getType() == Material.CLOCK) {
                event.setCancelled(true);
                java.util.UUID owner = plugin.getGameManager().getLobbyOwner();
                if (owner != null && owner.equals(player.getUniqueId())) {
                    openTimeSelector(player);
                } else {
                    player.sendMessage("§cOnly the lobby owner can change the time.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("Pilih Kelas Anak")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 9) return;

            if (slot == 8) {
                player.closeInventory();
                return;
            }

            ChildClass[] classes = ChildClass.values();
            if (slot < classes.length) {
                ChildClass selected = classes[slot];
                SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
                if (snp != null) {
                    snp.setChildClass(selected);
                    player.sendMessage("§a§l[Kelas] §eAnda memilih kelas: §6" + selected.getDisplayName());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                    player.closeInventory();
                }
            }
        } else if (title.equals("Pengaturan Waktu Game")) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 9) return;

            if (slot == 8) {
                player.closeInventory();
                return;
            }
            
            int minutes = 0;
            if (slot == 1) minutes = 5;
            else if (slot == 3) minutes = 10;
            else if (slot == 5) minutes = 15;
            else if (slot == 7) minutes = 20;
            
            if (minutes > 0) {
                plugin.getConfig().set("game-duration-seconds", minutes * 60);
                plugin.saveConfig();
                plugin.getGameManager().setGameTimeSeconds(minutes * 60);
                
                plugin.getGameManager().broadcast("§6[§eSN§6] §eDurasi game telah diatur menjadi §b" + minutes + " menit §eoleh " + player.getName() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.closeInventory();
            }
        }
    }

    public void openClassSelector(Player player) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(player, 9, Component.text("Pilih Kelas Anak"));
        
        ChildClass[] classes = ChildClass.values();
        Material[] icons = {
            Material.IRON_SWORD,         // Brave
            Material.COMPASS,            // Detective
            Material.LEATHER_CHESTPLATE,    // Bagger
            Material.GOLDEN_HELMET,       // Leader
            Material.REPEATER,            // Inventor
            Material.BOW,                 // Scout
            Material.REDSTONE_TORCH,      // Engineer
            Material.GOLDEN_CARROT        // Gamer
        };

        for (int i = 0; i < classes.length; i++) {
            ItemStack item = new ItemStack(icons[i]);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§e§l" + classes[i].getDisplayName()));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text(classes[i].getDescription()));
                lore.add(Component.text(""));
                for (String detail : classes[i].getDetails()) {
                    lore.add(Component.text(detail));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("§a» Klik untuk memilih!"));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i, item);
        }

        // Close item in slot 8
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("§cTutup Menu"));
            close.setItemMeta(closeMeta);
        }
        gui.setItem(8, close);

        player.openInventory(gui);
    }

    public void openTimeSelector(Player player) {
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(player, 9, Component.text("Pengaturan Waktu Game"));
        
        int[] slots = {1, 3, 5, 7};
        int[] minutes = {5, 10, 15, 20};
        
        for (int i = 0; i < slots.length; i++) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("§a§l" + minutes[i] + " Menit"));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7Klik untuk mengatur durasi"));
                lore.add(Component.text("§7game menjadi " + minutes[i] + " menit."));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(slots[i], item);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("§cTutup Menu"));
            close.setItemMeta(closeMeta);
        }
        gui.setItem(8, close);

        player.openInventory(gui);
    }
}
