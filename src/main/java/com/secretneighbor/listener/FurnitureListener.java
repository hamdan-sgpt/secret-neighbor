package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.furniture.DrawerManager;
import com.secretneighbor.furniture.FridgeManager;
import com.secretneighbor.furniture.WardrobeManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

public class FurnitureListener implements Listener {

    private final SecretNeighborPlugin plugin;

    public FurnitureListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // Handle ItemDisplay pickup (thrown items on the floor)
        if (event.getRightClicked() instanceof ItemDisplay itemDisplay) {
            if (itemDisplay.getScoreboardTags().contains("sn_3d_item")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                ItemStack displayedItem = itemDisplay.getItemStack();
                if (displayedItem != null && displayedItem.getType() != Material.AIR) {
                    // Check if player has inventory space
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendActionBar(Component.text("§cInventory full!"));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                        return;
                    }
                    // Give item to player
                    ItemStack pickupItem = displayedItem.clone();
                    pickupItem.setAmount(1);
                    player.getInventory().addItem(pickupItem);
                    // Remove the display entity
                    itemDisplay.remove();
                    // Effects
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                    player.getWorld().spawnParticle(Particle.ITEM, itemDisplay.getLocation().add(0, 0.3, 0), 5, 0.1, 0.1, 0.1, 0.05, displayedItem);
                    String itemName = displayedItem.hasItemMeta() && displayedItem.getItemMeta().hasDisplayName()
                            ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayedItem.getItemMeta().displayName())
                            : displayedItem.getType().name();
                    player.sendActionBar(Component.text("§a✔ Picked up " + itemName));
                }
                return;
            }
        }

        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        DrawerManager dm = plugin.getDrawerManager();
        if (dm != null && dm.isDrawer(stand)) {
            event.setCancelled(true);
            dm.openDrawer(event.getPlayer(), stand);
            return;
        }

        FridgeManager fm = plugin.getFridgeManager();
        if (fm != null && fm.isFridge(stand)) {
            event.setCancelled(true);
            fm.openFridge(event.getPlayer(), stand);
            return;
        }

        WardrobeManager wm = plugin.getWardrobeManager();
        if (wm != null && wm.isWardrobe(stand)) {
            event.setCancelled(true);
            wm.interactWardrobe(event.getPlayer(), stand);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.block.Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;

        DrawerManager dm = plugin.getDrawerManager();
        FridgeManager fm = plugin.getFridgeManager();
        WardrobeManager wm = plugin.getWardrobeManager();

        Location blockLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        // Target only the single closest ArmorStand to the clicked block
        blockLoc.getWorld().getNearbyEntities(blockLoc, 1.8, 1.8, 1.8).stream()
                .filter(e -> e instanceof ArmorStand)
                .map(e -> (ArmorStand) e)
                .min(java.util.Comparator.comparingDouble(stand -> stand.getLocation().distanceSquared(blockLoc)))
                .ifPresent(stand -> {
                    if (dm != null && dm.isDrawer(stand)) {
                        event.setCancelled(true);
                        dm.openDrawer(event.getPlayer(), stand);
                    } else if (fm != null && fm.isFridge(stand)) {
                        event.setCancelled(true);
                        fm.openFridge(event.getPlayer(), stand);
                    } else if (wm != null && wm.isWardrobe(stand)) {
                        event.setCancelled(true);
                        wm.interactWardrobe(event.getPlayer(), stand);
                    }
                });
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        if (block.getType() != Material.BARRIER) return;

        DrawerManager dm = plugin.getDrawerManager();
        FridgeManager fm = plugin.getFridgeManager();
        WardrobeManager wm = plugin.getWardrobeManager();

        Location blockLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        blockLoc.getWorld().getNearbyEntities(blockLoc, 1.8, 1.8, 1.8).stream()
                .filter(e -> e instanceof ArmorStand)
                .map(e -> (ArmorStand) e)
                .min(java.util.Comparator.comparingDouble(stand -> stand.getLocation().distanceSquared(blockLoc)))
                .ifPresent(stand -> {
                    if (dm != null && dm.isDrawer(stand)) {
                        boolean isLong = stand.getScoreboardTags().contains("sn_drawer_long");
                        Location centerBlockLoc = stand.getLocation().clone().add(0, 1.020833, 0).getBlock().getLocation();
                        float yaw = stand.getLocation().getYaw();

                        // Clear decorations (trapdoors, banners, etc.) around the drawer first
                        dm.clearDecorationsAround(centerBlockLoc, isLong, yaw);

                        stand.remove();
                        centerBlockLoc.getBlock().setType(Material.AIR);
                        if (isLong) {
                            int absYaw = Math.abs((int) yaw) % 360;
                            if (absYaw == 90 || absYaw == 270) {
                                centerBlockLoc.clone().add(0, 0, 1).getBlock().setType(Material.AIR);
                                centerBlockLoc.clone().add(0, 0, -1).getBlock().setType(Material.AIR);
                            } else {
                                centerBlockLoc.clone().add(1, 0, 0).getBlock().setType(Material.AIR);
                                centerBlockLoc.clone().add(-1, 0, 0).getBlock().setType(Material.AIR);
                            }
                        }
                        event.getPlayer().sendMessage("§c✔ Drawer removed.");
                    } else if (fm != null && fm.isFridge(stand)) {
                        Location centerBlockLoc = stand.getLocation().clone().add(0, 1.020833, 0).getBlock().getLocation();
                        stand.remove();
                        centerBlockLoc.getBlock().setType(Material.AIR);
                        centerBlockLoc.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
                        event.getPlayer().sendMessage("§c✔ Refrigerator removed.");
                    } else if (wm != null && wm.isWardrobe(stand)) {
                        Location centerBlockLoc = stand.getLocation().clone().add(0, 1.020833, 0).getBlock().getLocation();
                        stand.remove();
                        centerBlockLoc.getBlock().setType(Material.AIR);
                        centerBlockLoc.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
                        event.getPlayer().sendMessage("§c✔ Wardrobe removed.");
                    }
                });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        DrawerManager dm = plugin.getDrawerManager();
        if (dm != null && dm.hasOpenDrawer(player.getUniqueId())) {
            dm.closeDrawer(player);
        }

        FridgeManager fm = plugin.getFridgeManager();
        if (fm != null && fm.hasOpenFridge(player.getUniqueId())) {
            fm.closeFridge(player);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        WardrobeManager wm = plugin.getWardrobeManager();
        if (wm != null && wm.isPlayerHiding(player.getUniqueId())) {
            event.setCancelled(true);
            wm.exitWardrobe(player);
        }
    }
}
