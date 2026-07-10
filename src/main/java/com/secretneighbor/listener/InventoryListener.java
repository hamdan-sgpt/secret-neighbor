package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.ChildClass;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryListener implements Listener {

    private final SecretNeighborPlugin plugin;

    // GUI slot constants for 27-slot (3-row) layout
    // Row 1 (0-8):   all border except slot 4 = class info
    // Row 2 (9-17):  border + item slots + key count
    //   Slot 10,11 = usable slots for all children
    //   Slot 12    = usable slot 3 (Bagger only) or locked
    //   Slot 17    = key progress indicator
    // Row 3 (18-26): all border
    private static final int GUI_SIZE = 27;
    private static final int GUI_CLASS_INFO_SLOT = 4;
    private static final int GUI_SLOT_1 = 10;
    private static final int GUI_SLOT_2 = 11;
    private static final int GUI_SLOT_3 = 12; // Bagger only
    private static final int GUI_KEY_COUNT_SLOT = 17;

    // Mapping: index -> GUI slot position
    private static final int[] GUI_ITEM_SLOTS = {GUI_SLOT_1, GUI_SLOT_2, GUI_SLOT_3};

    public InventoryListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;

        // Periodically verify and enforce inventory locks (every 1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getGameManager().getState() == com.secretneighbor.game.GameState.IN_GAME) {
                for (SNPlayer snp : plugin.getGameManager().getPlayers().values()) {
                    Player p = Bukkit.getPlayer(snp.getUuid());
                    if (p != null && snp.isChild() && snp.isAlive() && !snp.isKnocked()) {
                        lockPlayerInventorySlots(p, snp);
                    }
                }
            }
        }, 20L, 20L);
    }

    // --- Locked Pane Items ---

    /**
     * Creates the locked pane for hotbar/inventory slots (subtle gray pane).
     */
    public static ItemStack getLockedPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§8§l✦"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Slot terkunci."));
            meta.lore(lore);
            meta.setCustomModelData(9999);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the locked pane with context-aware lore showing max slot info.
     */
    private static ItemStack getLockedPaneWithInfo(int maxSlots) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§8§l✦"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Slot terkunci."));
            lore.add(Component.text("§7Kamu bisa membawa §f" + maxSlots + " item§7."));
            meta.lore(lore);
            meta.setCustomModelData(9999);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the dark border pane for GUI decoration.
     */
    private static ItemStack getBorderPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§r"));
            meta.setCustomModelData(9998);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the Bagger-only locked slot pane (red, with explanation).
     */
    private static ItemStack getBaggerLockedPane() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§c§l🔒"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Slot ini hanya untuk"));
            lore.add(Component.text("§dBagger §7(3 slot)."));
            meta.lore(lore);
            meta.setCustomModelData(9997);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the class info display item for the GUI header.
     */
    private static ItemStack getClassInfoItem(ChildClass childClass) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§e§l" + childClass.getDisplayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(childClass.getDescription()));
            lore.add(Component.text(""));
            int maxSlots = (childClass == ChildClass.BAGGER) ? 3 : 2;
            lore.add(Component.text("§7Kapasitas: §f" + maxSlots + " slot"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the key progress indicator item.
     */
    private ItemStack getKeyProgressItem() {
        int inserted = 0;
        int total = 6;
        if (plugin.getKeyManager() != null) {
            inserted = plugin.getKeyManager().getKeysInserted();
        }

        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6§l🔑 Kunci"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Terpasang: §f" + inserted + "§7/§f" + total));

            // Visual progress bar
            StringBuilder bar = new StringBuilder("§8[");
            for (int i = 0; i < total; i++) {
                bar.append(i < inserted ? "§a■" : "§7□");
            }
            bar.append("§8]");
            lore.add(Component.text(bar.toString()));

            if (inserted >= total) {
                lore.add(Component.text(""));
                lore.add(Component.text("§a§l✔ Pintu terbuka!"));
            } else {
                lore.add(Component.text(""));
                lore.add(Component.text("§7Cari kunci di laci-laci."));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isLockedPane(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.GRAY_STAINED_GLASS_PANE
                && item.getType() != Material.RED_STAINED_GLASS_PANE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        int cmd = meta.getCustomModelData();
        return cmd == 9999 || cmd == 9997;
    }

    private static boolean isBorderPane(ItemStack item) {
        if (item == null || item.getType() != Material.BLACK_STAINED_GLASS_PANE) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 9998;
    }

    private static boolean isGuiDecoration(ItemStack item) {
        return isLockedPane(item) || isBorderPane(item);
    }

    // --- Hotbar Lock System ---

    public static void lockPlayerInventorySlots(Player player, SNPlayer snp) {
        if (snp == null || !snp.isAlive()) return;
        if (snp.isNeighbor() && snp.isBapakeMode()) return; // Neighbor in Bapake mode has no locked slots

        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        int maxSlots;
        if (snp.isNeighbor()) {
            maxSlots = 3; // Keep slots 0, 1, 2 open for Neighbor ability items
        } else {
            maxSlots = (snp.getChildClass() == ChildClass.BAGGER) ? 3 : 2;
        }

        // Lock hotbar slots from maxSlots to 8
        for (int i = maxSlots; i < 9; i++) {
            ItemStack current = inv.getItem(i);
            if (!isLockedPane(current)) {
                if (current != null && current.getType() != Material.AIR) {
                    org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("SecretNeighbor"), "sn_ability");
                    ItemMeta meta = current.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(abilityKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        continue;
                    }
                    // Try to push it to open slots, or drop it
                    if (!pushToOpenSlots(player, current, maxSlots)) {
                        player.getWorld().dropItemNaturally(player.getLocation(), current);
                    }
                }
                inv.setItem(i, getLockedPaneWithInfo(maxSlots));
            }
        }

        // Lock all main inventory slots (9 to 35)
        for (int i = 9; i < 36; i++) {
            ItemStack current = inv.getItem(i);
            if (!isLockedPane(current)) {
                if (current != null && current.getType() != Material.AIR) {
                    org.bukkit.NamespacedKey abilityKey = new org.bukkit.NamespacedKey(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("SecretNeighbor"), "sn_ability");
                    ItemMeta meta = current.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(abilityKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        continue;
                    }
                    if (!pushToOpenSlots(player, current, maxSlots)) {
                        player.getWorld().dropItemNaturally(player.getLocation(), current);
                    }
                }
                inv.setItem(i, getLockedPaneWithInfo(maxSlots));
            }
        }

        // Prevent items in offhand slot (clear glass pane too)
        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            if (!isLockedPane(offhand)) {
                if (!pushToOpenSlots(player, offhand, maxSlots)) {
                    player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                }
            }
            inv.setItemInOffHand(null);
        }
    }

    private static boolean pushToOpenSlots(Player player, ItemStack item, int maxSlots) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < maxSlots; i++) {
            ItemStack current = inv.getItem(i);
            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(i, item);
                return true;
            }
        }
        return false;
    }

    // --- Premium 27-Slot Custom GUI ---

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isAlive()) return;

        // If trying to open survival inventory (crafting inventory)
        if (event.getInventory().getType() == InventoryType.CRAFTING) {
            event.setCancelled(true);

            boolean isBagger = snp.getChildClass() == ChildClass.BAGGER;
            int maxSlots = isBagger ? 3 : 2;
            String title = isBagger ? "§8» §d§lTas Bagger" : "§8» §a§lInventori Anak";
            Inventory gui = Bukkit.createInventory(player, GUI_SIZE, Component.text(title));

            // Fill entire GUI with border panes
            ItemStack border = getBorderPane();
            for (int i = 0; i < GUI_SIZE; i++) {
                gui.setItem(i, border);
            }

            // Row 1: Class info at slot 4
            if (snp.getChildClass() != null) {
                gui.setItem(GUI_CLASS_INFO_SLOT, getClassInfoItem(snp.getChildClass()));
            }

            // Row 2: Usable item slots
            for (int i = 0; i < maxSlots; i++) {
                int guiSlot = GUI_ITEM_SLOTS[i]; // 10, 11, (12)
                ItemStack hotbarItem = player.getInventory().getItem(i);
                if (hotbarItem != null && !isLockedPane(hotbarItem) && hotbarItem.getType() != Material.AIR) {
                    gui.setItem(guiSlot, hotbarItem);
                } else {
                    gui.setItem(guiSlot, null); // Empty usable slot
                }
            }

            // Slot 12: locked for non-Bagger
            if (!isBagger) {
                gui.setItem(GUI_SLOT_3, getBaggerLockedPane());
            }

            // Row 2: Key progress at slot 17
            gui.setItem(GUI_KEY_COUNT_SLOT, getKeyProgressItem());

            // Open on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(gui);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
            });
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        // Prevent ANY player from putting items in the offhand slot
        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND || event.getSlot() == 40) {
            event.setCancelled(true);
            return;
        }

        // Prevent picking up/clicking locked panes for ALL players (Children & Neighbors)
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isLockedPane(current) || isLockedPane(cursor)) {
            event.setCancelled(true);
            return;
        }

        // Prevent swapping items to locked hotbar slots using keyboard hotkeys (1-9) for ALL players
        if (event.getHotbarButton() >= 0) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isLockedPane(hotbarItem)) {
                event.setCancelled(true);
                return;
            }
        }

        if (!snp.isChild()) return;

        String title = event.getView().getTitle();
        boolean isCustomGui = title.contains("Inventori Anak") || title.contains("Tas Bagger");

        if (isCustomGui) {
            // Cancel clicks in bottom player inventory
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                event.setCancelled(true);
                return;
            }

            int rawSlot = event.getRawSlot();
            boolean isBagger = snp.getChildClass() == ChildClass.BAGGER;

            // Only allow interaction with usable item slots
            boolean isUsableSlot = (rawSlot == GUI_SLOT_1 || rawSlot == GUI_SLOT_2
                    || (isBagger && rawSlot == GUI_SLOT_3));

            if (!isUsableSlot) {
                event.setCancelled(true);
                return;
            }

            // Cancel shift-clicking to prevent item loss/glitches
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }

            // Prevent hotkey swaps into locked slots
            if (event.getHotbarButton() >= 0) {
                int maxSlots = isBagger ? 3 : 2;
                if (event.getHotbarButton() >= maxSlots) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isAlive()) return;

        String title = event.getView().getTitle();
        boolean isCustomGui = title.contains("Inventori Anak") || title.contains("Tas Bagger");

        if (isCustomGui) {
            Inventory gui = event.getInventory();
            int maxSlots = (snp.getChildClass() == ChildClass.BAGGER) ? 3 : 2;

            // Sync GUI items back to hotbar using slot mapping
            for (int i = 0; i < maxSlots; i++) {
                int guiSlot = GUI_ITEM_SLOTS[i];
                ItemStack item = gui.getItem(guiSlot);
                if (item != null && !isGuiDecoration(item) && item.getType() != Material.AIR) {
                    player.getInventory().setItem(i, item);
                } else {
                    player.getInventory().setItem(i, null);
                }
            }

            // Ensure other slots are locked
            lockPlayerInventorySlots(player, snp);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.2f);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        if (isLockedPane(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        // Cancel swapping hand items since offhand is locked
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        for (int rawSlot : event.getRawSlots()) {
            // Check if they are dragging into the offhand slot (slot 40 in player inv, 45 in generic)
            if (rawSlot == 40 || rawSlot == 45) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent dragging onto any slot that has a locked pane (for both children and neighbors)
        for (int slot : event.getRawSlots()) {
            ItemStack current = event.getView().getItem(slot);
            if (isLockedPane(current)) {
                event.setCancelled(true);
                return;
            }
        }

        if (!snp.isChild()) return;

        String title = event.getView().getTitle();
        boolean isCustomGui = title.contains("Inventori Anak") || title.contains("Tas Bagger");
        boolean isBagger = snp.getChildClass() == ChildClass.BAGGER;

        for (int rawSlot : event.getRawSlots()) {
            if (isCustomGui) {
                // Only allow dragging into usable GUI slots
                boolean isUsable = (rawSlot == GUI_SLOT_1 || rawSlot == GUI_SLOT_2
                        || (isBagger && rawSlot == GUI_SLOT_3));
                if (!isUsable) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                // In normal player inventory view — prevent dragging into locked hotbar slots
                int maxSlots = isBagger ? 3 : 2;
                if (rawSlot >= maxSlots && rawSlot < 36) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        if (!(event.getEntity() instanceof Player player)) return;

        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isAlive()) return;

        int maxSlots = (snp.getChildClass() == ChildClass.BAGGER) ? 3 : 2;
        int nonLockedCount = 0;

        // Count how many non-locked items they currently carry in their open slots
        for (int i = 0; i < maxSlots; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !isLockedPane(item) && item.getType() != Material.AIR) {
                nonLockedCount++;
            }
        }

        // If they already carry maximum items, cancel the pickup and show ActionBar
        if (nonLockedCount >= maxSlots) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("§c§l⚠ §eTas penuh! §7Buang item untuk mengambil yang baru."));
        } else {
            // Show pickup feedback
            final int currentCount = nonLockedCount + 1;
            final int maxS = maxSlots;
            String itemName = event.getItem().getItemStack().getType().name().replace('_', ' ').toLowerCase();
            // Capitalize first letter
            if (!itemName.isEmpty()) {
                itemName = Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1);
            }
            // Check if item has custom display name
            ItemMeta pickupMeta = event.getItem().getItemStack().getItemMeta();
            if (pickupMeta != null && pickupMeta.hasDisplayName()) {
                Component displayName = pickupMeta.displayName();
                if (displayName != null) {
                    itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
                }
            }
            final String finalItemName = itemName;
            // Delayed by 1 tick so the item is actually in inventory
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendActionBar(Component.text("§a§l+ §f" + finalItemName + " §7(" + currentCount + "/" + maxS + " slot)"));
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        // Clean up locked panes from drops so they don't drop on the ground
        event.getDrops().removeIf(InventoryListener::isLockedPane);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;
        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isChild() || !snp.isAlive()) return;

        // Lock inventory again on respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> lockPlayerInventorySlots(player, snp), 2L);
    }
}
