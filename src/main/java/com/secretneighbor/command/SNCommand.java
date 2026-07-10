package com.secretneighbor.command;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.game.GameState;
import com.secretneighbor.map.MapGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Material;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.Bisected;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SNCommand implements CommandExecutor, TabCompleter {

    private final SecretNeighborPlugin plugin;

    public SNCommand(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
        var command = plugin.getCommand("sn");
        if (command != null) {
            command.setTabCompleter(this);
        }
        var commandAdmin = plugin.getCommand("sna");
        if (commandAdmin != null) {
            commandAdmin.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean isUsedSna = label.equalsIgnoreCase("sna");

        if (args.length == 0) {
            sendHelp(sender, isUsedSna);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(sender, isUsedSna);
            return true;
        }

        boolean isAdminCmd = isCommandAdminOnly(sub);

        if (isUsedSna) {
            if (!sender.hasPermission("secretneighbor.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }
            if (!isAdminCmd) {
                sender.sendMessage("§cUnknown admin subcommand! Type /sna for help.");
                return true;
            }
        } else {
            if (isAdminCmd) {
                sender.sendMessage("§cThis is an admin command! Please use /sna " + sub + " instead.");
                return true;
            }
        }


        switch (sub) {
            case "generatemap" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                sender.sendMessage("§e§lCopying and loading 'Secret Neighbor Map 2' world... Please wait.");
                new MapGenerator(plugin).generate(sender);
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can create lobbies!");
                    return true;
                }
                if (plugin.getGameManager().getState() != GameState.IDLE) {
                    player.sendMessage("§cAn active lobby or game already exists!");
                    return true;
                }
                plugin.getGameManager().createLobby(player);
            }
            case "time" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set the time!");
                    return true;
                }
                if (plugin.getGameManager().getState() != GameState.LOBBY) {
                    sender.sendMessage("§cNo active lobby! Run /sn create first.");
                    return true;
                }
                UUID owner = plugin.getGameManager().getLobbyOwner();
                if (owner == null || !owner.equals(player.getUniqueId())) {
                    sender.sendMessage("§cOnly the lobby owner can set the time!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /sn time <seconds>");
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);
                    plugin.getGameManager().setGameTimeSeconds(seconds);
                    sender.sendMessage("§aGame time set to " + seconds + " seconds.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number!");
                }
            }
            case "invite" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can invite!");
                    return true;
                }
                if (plugin.getGameManager().getState() != GameState.LOBBY) {
                    sender.sendMessage("§cNo active lobby! Run /sn create first.");
                    return true;
                }
                UUID owner = plugin.getGameManager().getLobbyOwner();
                if (owner == null || !owner.equals(player.getUniqueId())) {
                    sender.sendMessage("§cOnly the lobby owner can invite players!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /sn invite <player_name/all>");
                    return true;
                }
                handleInvite(sender, args[1]);
            }
            case "join", "accept" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can join lobbies!");
                    return true;
                }
                plugin.getGameManager().addPlayer(player);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can leave lobbies!");
                    return true;
                }
                plugin.getGameManager().removePlayer(player);
            }
            case "start" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can start the game!");
                    return true;
                }
                if (plugin.getGameManager().getState() != GameState.LOBBY) {
                    sender.sendMessage("§cNo active lobby! Run /sn create first.");
                    return true;
                }
                UUID owner = plugin.getGameManager().getLobbyOwner();
                if (owner == null || !owner.equals(player.getUniqueId())) {
                    sender.sendMessage("§cOnly the lobby owner can start the game!");
                    return true;
                }
                if (plugin.getGameManager().getPlayerCount() < 1) {
                    sender.sendMessage("§cNeed at least 1 player to start! Currently: " + plugin.getGameManager().getPlayerCount());
                    return true;
                }

                String forceRole = null;
                if (args.length >= 2) {
                    forceRole = args[1];
                }

                sender.sendMessage("§a§lStarting Secret Neighbor game...");
                plugin.getGameManager().startGame(forceRole);
            }
            case "setlobby" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set the lobby location!");
                    return true;
                }
                plugin.getLocationManager().setLobby(player.getLocation());
                if (plugin.getHologramManager() != null) {
                    plugin.getHologramManager().spawnLobbyHologram();
                }
                sender.sendMessage("§aLobby spawn point has been updated to your current location!");
            }
            case "setentrance" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set the entrance location!");
                    return true;
                }
                plugin.getLocationManager().setHouseEntrance(player.getLocation());
                sender.sendMessage("§aHouse entrance has been updated to your current location!");
            }
            case "sethome" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set the home location!");
                    return true;
                }
                plugin.getLocationManager().setHome(player.getLocation());
                sender.sendMessage("§aHome spawn point has been updated to your current location!");
            }
            case "items", "getitems" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can receive items!");
                    return true;
                }
                handleGiveItems(player);
            }
            case "forcestop", "stop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can stop the game!");
                    return true;
                }
                if (plugin.getGameManager().getState() == GameState.IDLE) {
                    sender.sendMessage("§cNo game is currently active!");
                    return true;
                }
                UUID owner = plugin.getGameManager().getLobbyOwner();
                if (owner == null || !owner.equals(player.getUniqueId())) {
                    sender.sendMessage("§cOnly the lobby owner can stop the game!");
                    return true;
                }
                sender.sendMessage("§cForce stopping the game...");
                plugin.getGameManager().forceStopGame();
            }
            case "findkeys" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command!");
                    return true;
                }
                if (plugin.getKeyManager() == null) {
                    player.sendMessage("§cKeyManager is not loaded.");
                    return true;
                }
                List<org.bukkit.Location> keyLocs = plugin.getKeyManager().getActiveKeyLocations();
                if (keyLocs.isEmpty()) {
                    player.sendMessage("§cNo active keys found in any drawers (are they already collected or game not started?).");
                    return true;
                }
                player.sendMessage("§6§lFound " + keyLocs.size() + " active keys in drawers:");
                int i = 1;
                for (org.bukkit.Location loc : keyLocs) {
                    double x = loc.getX();
                    double y = loc.getY() + 1.5;
                    double z = loc.getZ();
                    
                    // Add 0.5 to center on the block if getX is integer-like
                    if (x == Math.floor(x)) x += 0.5;
                    if (z == Math.floor(z)) z += 0.5;

                    Component msg = Component.text("§e" + i + ". §fKey at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " ")
                            .append(Component.text("§a§l[TELEPORT]")
                            .clickEvent(ClickEvent.runCommand(String.format(java.util.Locale.US, "/tp @s %.2f %.2f %.2f", x, y, z)))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to teleport"))));
                    player.sendMessage(msg);
                    i++;
                }
            }
            case "placedrawer" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can place drawers!");
                    return true;
                }
                plugin.getDrawerManager().spawnDrawer(player.getLocation());
                player.sendMessage("§a✔ Drawer placed at your location! Right-click to open/close.");
            }
            case "setdrawer" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set drawers!");
                    return true;
                }
                org.bukkit.Location targetLoc = player.getLocation().clone().subtract(0, 1, 0);
                plugin.getDrawerManager().spawnDrawer(targetLoc);
                player.sendMessage("§a✔ Drawer set replacing the block under your feet!");
            }
            case "setdrawerlong" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set drawers!");
                    return true;
                }
                org.bukkit.Location targetLoc = player.getLocation().clone().subtract(0, 1, 0);
                plugin.getDrawerManager().spawnLongDrawer(targetLoc);
                player.sendMessage("§a✔ Long Drawer set replacing the block under your feet!");
            }
            case "setdrawerlongalt" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set drawers!");
                    return true;
                }
                org.bukkit.Location targetLoc = player.getLocation().clone().subtract(0, 1, 0);
                plugin.getDrawerManager().spawnLongAltDrawer(targetLoc);
                player.sendMessage("§a✔ Long Alt Drawer (Colorful) set replacing the block under your feet!");
            }
            case "setdoorexit" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set the exit door!");
                    return true;
                }
                org.bukkit.block.Block block = player.getTargetBlockExact(5);
                if (block == null || !(block.getBlockData() instanceof org.bukkit.block.data.type.Door)) {
                    player.sendMessage("§cYou must look at a Door block within 5 blocks!");
                    return true;
                }

                org.bukkit.block.data.type.Door doorData = (org.bukkit.block.data.type.Door) block.getBlockData();
                org.bukkit.Location bottomLoc = block.getLocation();
                if (doorData.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) {
                    bottomLoc.add(0, -1, 0);
                }

                // Set exit door location and player standing location
                plugin.getLocationManager().setBasementDoor(bottomLoc);
                plugin.getLocationManager().setBasementDoorPlayer(player.getLocation());

                boolean flipped = false;
                double px = player.getLocation().getX();
                double pz = player.getLocation().getZ();
                double dx = bottomLoc.getX();
                double dz = bottomLoc.getZ();
                org.bukkit.block.BlockFace facing = doorData.getFacing();
                
                switch (facing) {
                    case WEST -> flipped = (px > dx + 0.1875);
                    case EAST -> flipped = (px < dx + 0.8125);
                    case SOUTH -> flipped = (pz < dz + 0.8125);
                    case NORTH -> flipped = (pz > dz + 0.1875);
                    default -> {}
                }
                plugin.getLocationManager().setBasementDoorFlipped(flipped);
                Bukkit.getLogger().info("[SN-Debug] setdoorexit: px=" + px + ", pz=" + pz + ", dx=" + dx + ", dz=" + dz + ", facing=" + facing + ", flipped=" + flipped);

                // Swap targeted door blocks with Iron Door
                org.bukkit.block.data.type.Door bottomDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.IRON_DOOR);
                bottomDoorData.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
                bottomDoorData.setFacing(facing);
                bottomDoorData.setOpen(false);

                org.bukkit.block.data.type.Door topDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(Material.IRON_DOOR);
                topDoorData.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
                topDoorData.setFacing(facing);
                topDoorData.setOpen(false);

                bottomLoc.getBlock().setType(Material.IRON_DOOR, false);
                org.bukkit.Location topLoc = bottomLoc.clone().add(0, 1, 0);
                topLoc.getBlock().setType(Material.IRON_DOOR, false);

                topLoc.getBlock().setBlockData(topDoorData, false);
                bottomLoc.getBlock().setBlockData(bottomDoorData, true);

                if (plugin.getGameManager().getState() == com.secretneighbor.game.GameState.IN_GAME) {
                    plugin.getKeyManager().spawnPadlocks(bottomLoc);
                }

                player.sendMessage("§a✔ Exit Door set! The door has been updated to the exit door texture and requires all 6 keys to open.");
            }
            case "setkeycarddoor", "setkeycard" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can set keycard doors!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /sn setkeycarddoor <1/2/3>");
                    return true;
                }

                org.bukkit.block.Block block = player.getTargetBlockExact(5);
                if (block == null || !(block.getBlockData() instanceof org.bukkit.block.data.type.Door)) {
                    player.sendMessage("§cYou must look at a Door block within 5 blocks!");
                    return true;
                }

                org.bukkit.block.data.type.Door doorData = (org.bukkit.block.data.type.Door) block.getBlockData();
                org.bukkit.Location bottomLoc = block.getLocation();
                if (doorData.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) {
                    bottomLoc.add(0, -1, 0);
                }

                int level;
                try {
                    level = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    player.sendMessage("§cUsage: /sn setkeycarddoor <1/2/3>");
                    return true;
                }

                Material newMat;
                String levelName;
                if (level == 1) {
                    newMat = Material.COPPER_DOOR;
                    levelName = "§bLevel 1 (Blue)";
                } else if (level == 2) {
                    newMat = Material.EXPOSED_COPPER_DOOR;
                    levelName = "§6Level 2 (Yellow)";
                } else if (level == 3) {
                    newMat = Material.WEATHERED_COPPER_DOOR;
                    levelName = "§cLevel 3 (Red)";
                } else {
                    player.sendMessage("§cUsage: /sn setkeycarddoor <1/2/3>");
                    return true;
                }

                org.bukkit.block.BlockFace facing = doorData.getFacing();
                org.bukkit.block.data.type.Door.Hinge hinge = doorData.getHinge();
                boolean isOpen = doorData.isOpen();

                org.bukkit.block.data.type.Door bottomDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(newMat);
                bottomDoorData.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
                bottomDoorData.setFacing(facing);
                bottomDoorData.setHinge(hinge);
                bottomDoorData.setOpen(isOpen);

                org.bukkit.block.data.type.Door topDoorData = (org.bukkit.block.data.type.Door) Bukkit.createBlockData(newMat);
                topDoorData.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
                topDoorData.setFacing(facing);
                topDoorData.setHinge(hinge);
                topDoorData.setOpen(isOpen);

                bottomLoc.getBlock().setType(newMat, false);
                org.bukkit.Location topLoc = bottomLoc.clone().add(0, 1, 0);
                topLoc.getBlock().setType(newMat, false);

                topLoc.getBlock().setBlockData(topDoorData, false);
                bottomLoc.getBlock().setBlockData(bottomDoorData, true);

                player.sendMessage("§a✔ Keycard Door " + levelName + " set replacing the door in front of you!");
            }
            case "setfridge" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can place a refrigerator!");
                    return true;
                }

                plugin.getFridgeManager().spawnFridge(player.getLocation());
                player.sendMessage("§a✔ Realistic 3D Refrigerator placed successfully!");
            }
            case "setwardrobe" -> {
                if (!sender.hasPermission("secretneighbor.admin")) {
                    sender.sendMessage("§cYou do not have permission to use this command!");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can place a wardrobe!");
                    return true;
                }

                plugin.getWardrobeManager().spawnWardrobe(player.getLocation());
                player.sendMessage("§a✔ Realistic 3D Wardrobe placed successfully!");
            }
            default -> sendHelp(sender, isUsedSna);
        }

        return true;
    }

    private void handleInvite(CommandSender sender, String targetArg) {
        Component inviteText = Component.text("§6[§eSecret Neighbor§6] §a" + sender.getName() + " has invited you to play! ")
                .append(Component.text("§e§l[CLICK HERE TO ACCEPT]")
                        .clickEvent(ClickEvent.runCommand("/sn accept"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to join the lobby!"))));

        if (targetArg.equalsIgnoreCase("all")) {
            List<Player> eligiblePlayers = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!plugin.getGameManager().getPlayerUUIDs().contains(p.getUniqueId())) {
                    eligiblePlayers.add(p);
                }
            }

            if (eligiblePlayers.isEmpty()) {
                sender.sendMessage("§cNo other online players to invite!");
                return;
            }

            Collections.shuffle(eligiblePlayers);
            int count = Math.min(eligiblePlayers.size(), 10);
            for (int i = 0; i < count; i++) {
                Player p = eligiblePlayers.get(i);
                plugin.getGameManager().addInvite(p.getUniqueId());
                p.sendMessage(inviteText);
            }
            sender.sendMessage("§aSuccessfully invited " + count + " players to join the lobby!");
        } else {
            Player target = Bukkit.getPlayer(targetArg);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }
            if (plugin.getGameManager().getPlayerUUIDs().contains(target.getUniqueId())) {
                sender.sendMessage("§cPlayer is already in the lobby queue.");
                return;
            }

            plugin.getGameManager().addInvite(target.getUniqueId());
            target.sendMessage(inviteText);
            sender.sendMessage("§aInvited " + target.getName() + " to the lobby.");
        }
    }

    private boolean isCommandAdminOnly(String sub) {
        return switch (sub) {
            case "generatemap", "setlobby", "setentrance", "sethome",
                 "items", "getitems", "placedrawer", "setdrawer", "setdrawerlong",
                 "setdrawerlongalt", "setdoorexit", "setkeycarddoor", "setfridge", "setwardrobe",
                 "findkeys" -> true;
            default -> false;
        };
    }

    private void sendHelp(CommandSender sender, boolean isAdmin) {
        if (isAdmin) {
            sender.sendMessage("§6§l=== Secret Neighbor Admin ===");
            sender.sendMessage("§e/sna findkeys §7- Cari & teleport ke lokasi kunci asli");
            sender.sendMessage("§e/sna setlobby §7- Atur koordinat lobi");
            sender.sendMessage("§e/sna setentrance §7- Atur titik masuk rumah");
            sender.sendMessage("§e/sna sethome §7- Atur titik start spawn pemain");
            sender.sendMessage("§e/sna generatemap §7- Load world Secret Neighbor");
            sender.sendMessage("§e/sna items §7- Dapatkan semua item kustom");
            sender.sendMessage("§e/sna placedrawer §7- Taruh laci di posisi kamu");
            sender.sendMessage("§e/sna setdrawer §7- Ganti blok di bawah kaki kamu dengan laci");
            sender.sendMessage("§e/sna setdrawerlong §7- Ganti blok dengan laci panjang");
            sender.sendMessage("§e/sna setdrawerlongalt §7- Ganti blok dengan laci warna-warni");
            sender.sendMessage("§e/sna setdoorexit §7- Atur pintu keluar utama di hadapan Anda");
            sender.sendMessage("§e/sna setkeycarddoor <1/2/3> §7- Atur pintu keycard di hadapan Anda");
            sender.sendMessage("§e/sna setfridge §7- Tempatkan kulkas realistis di hadapan Anda");
            sender.sendMessage("§e/sna setwardrobe §7- Tempatkan lemari persembunyian 3D di hadapan Anda");
        } else {
            sender.sendMessage("§6§l=== Secret Neighbor ===");
            sender.sendMessage("§e/sn create §7- Buat lobi permainan baru");
            sender.sendMessage("§e/sn join §7- Bergabung ke lobi aktif");
            sender.sendMessage("§e/sn leave §7- Keluar dari lobi");
            sender.sendMessage("§e/sn invite <nama/all> §7- Undang pemain (Hanya Owner)");
            sender.sendMessage("§e/sn start §7- Mulai permainan (Hanya Owner)");
            sender.sendMessage("§e/sn stop §7- Hentikan permainan (Hanya Owner)");
            sender.sendMessage("§e/sn time <detik> §7- Atur waktu permainan (Hanya Owner)");
            if (sender.hasPermission("secretneighbor.admin")) {
                sender.sendMessage("§7Admin commands: §e/sna");
            }
        }
    }

    private void handleGiveItems(Player player) {
        player.getInventory().clear();

        int[] cmdList = {
            1000, 1001, 1002, 1003, 1004, 1005, // 6 Keys
            1010, 1011, 1012, 1013, 1014, 1015, 1016, // Tools & Cards
            1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, // Throwables
            1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, // Child class items
            1300, 1301 // Neighbor items
        };
        String[] names = {
            "§cRed Key", "§9Blue Key", "§aGreen Key", "§eYellow Key", "§5Purple Key", "§6Orange Key",
            "§7Crowbar", "§fFlashlight", "§aRadar", "§cHammer", "§bKeycard Lvl 1", "§6Keycard Lvl 2", "§cKeycard Lvl 3",
            "§6Box", "§eChair", "§8TV", "§cSofa",
            "§aPainting", "§4Book", "§cTomato", "§eBroom",
            "§6Basketball", "§fPillow", "§6Hat",
            "§cBaseball Bat", "§6Camera", "§dBackpack", "§eMegaphone",
            "§9Wrench", "§aSlingshot", "§eSensor", "§bDecoy Console",
            "§4Bear Trap", "§5Neighbor Mask"
        };

        for (int i = 0; i < cmdList.length; i++) {
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.FEATHER);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(names[i]));
                meta.setCustomModelData(cmdList[i]);
                meta.setUnbreakable(true);
                item.setItemMeta(meta);
            }
            player.getInventory().addItem(item);
        }

        player.sendMessage("§a§l✔ Given all " + cmdList.length + " custom Secret Neighbor items!");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        boolean isUsedSna = alias.equalsIgnoreCase("sna");

        if (args.length == 1) {
            if (isUsedSna) {
                if (sender.hasPermission("secretneighbor.admin")) {
                    completions.add("setlobby");
                    completions.add("setentrance");
                    completions.add("sethome");
                    completions.add("generatemap");
                    completions.add("items");
                    completions.add("findkeys");
                    completions.add("placedrawer");
                    completions.add("setdrawer");
                    completions.add("setdrawerlong");
                    completions.add("setdrawerlongalt");
                    completions.add("setdoorexit");
                    completions.add("setkeycarddoor");
                    completions.add("setfridge");
                    completions.add("setwardrobe");
                }
            } else {
                completions.add("create");
                completions.add("join");
                completions.add("leave");
                completions.add("accept");
                completions.add("invite");
                completions.add("start");
                completions.add("stop");
                completions.add("time");
                completions.add("forcestop");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            completions.add("all");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!plugin.getGameManager().getPlayerUUIDs().contains(p.getUniqueId())) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("setkeycarddoor") || args[0].equalsIgnoreCase("setkeycard")) && sender.hasPermission("secretneighbor.admin")) {
            completions.add("1");
            completions.add("2");
            completions.add("3");
        }
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}
