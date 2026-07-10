package com.secretneighbor;

import com.secretneighbor.command.SNCommand;
import com.secretneighbor.furniture.DrawerManager;
import com.secretneighbor.furniture.FridgeManager;
import com.secretneighbor.furniture.WardrobeManager;
import com.secretneighbor.game.AtmosphereManager;
import com.secretneighbor.game.GameManager;
import com.secretneighbor.game.LocationManager;
import com.secretneighbor.listener.DoorListener;
import com.secretneighbor.listener.FlashlightListener;
import com.secretneighbor.listener.FurnitureListener;
import com.secretneighbor.listener.LobbyListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class SecretNeighborPlugin extends JavaPlugin {

    private LocationManager locationManager;
    private GameManager gameManager;
    private DrawerManager drawerManager;
    private FridgeManager fridgeManager;
    private WardrobeManager wardrobeManager;
    private com.secretneighbor.game.KeyManager keyManager;
    private FlashlightListener flashlightListener;
    private AtmosphereManager atmosphereManager;
    private com.sun.net.httpserver.HttpServer httpServer;
    private com.secretneighbor.listener.NeighborAbilityListener neighborAbilityListener;
    private com.secretneighbor.listener.ChildAbilitiesListener childAbilitiesListener;
    private com.secretneighbor.listener.KnockoutPacketHandler knockoutPacketHandler;
    private com.secretneighbor.game.ScoreboardManager scoreboardManager;

    private com.secretneighbor.game.HologramManager hologramManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.locationManager = new LocationManager(this);
        this.gameManager = new GameManager(this);
        this.drawerManager = new DrawerManager(this);
        this.fridgeManager = new FridgeManager(this);
        this.wardrobeManager = new WardrobeManager(this);
        this.keyManager = new com.secretneighbor.game.KeyManager(this);
        this.flashlightListener = new FlashlightListener(this);
        this.atmosphereManager = new AtmosphereManager(this);

        this.neighborAbilityListener = new com.secretneighbor.listener.NeighborAbilityListener(this);
        this.childAbilitiesListener = new com.secretneighbor.listener.ChildAbilitiesListener(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new FurnitureListener(this), this);
        getServer().getPluginManager().registerEvents(new com.secretneighbor.listener.ThrowableListener(this), this);
        getServer().getPluginManager().registerEvents(new com.secretneighbor.listener.BraveListener(this), this);
        getServer().getPluginManager().registerEvents(this.neighborAbilityListener, this);
        getServer().getPluginManager().registerEvents(this.childAbilitiesListener, this);
        getServer().getPluginManager().registerEvents(this.keyManager, this);
        getServer().getPluginManager().registerEvents(this.flashlightListener, this);
        getServer().getPluginManager().registerEvents(new DoorListener(this), this);
        getServer().getPluginManager().registerEvents(new com.secretneighbor.listener.InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new com.secretneighbor.listener.KnockoutListener(this), this);

        SNCommand snCommandExecutor = new SNCommand(this);
        var snCommand = getCommand("sn");
        if (snCommand != null) {
            snCommand.setExecutor(snCommandExecutor);
        }
        var snaCommand = getCommand("sna");
        if (snaCommand != null) {
            snaCommand.setExecutor(snCommandExecutor);
        }

        // Start the resource pack web server
        startHttpServer();

        this.hologramManager = new com.secretneighbor.game.HologramManager(this);
        getServer().getScheduler().runTaskLater(this, () -> {
            hologramManager.spawnLobbyHologram();
        }, 20L);

        // Register PlaceholderAPI Expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.secretneighbor.placeholder.SecretNeighborPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI Expansion registered successfully!");
        }

        // Register ProtocolLib Packet Listener
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            this.knockoutPacketHandler = new com.secretneighbor.listener.KnockoutPacketHandler(this);
            this.knockoutPacketHandler.register();
            getLogger().info("ProtocolLib integration registered for knockout crawling pose!");
        }

        // Initialize ScoreboardManager and start repeating updater task
        this.scoreboardManager = new com.secretneighbor.game.ScoreboardManager(this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (this.scoreboardManager != null) {
                this.scoreboardManager.updateAll();
            }
        }, 0L, 20L);

        getLogger().info("====================================");
        getLogger().info(" Secret Neighbor Map Loader Enabled! ");
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.resetAll();
        }
        if (drawerManager != null) {
            drawerManager.removeAll();
        }
        if (fridgeManager != null) {
            fridgeManager.removeAll();
        }
        if (wardrobeManager != null) {
            wardrobeManager.removeAll();
        }
        if (keyManager != null) {
            keyManager.clearPadlocks();
        }
        if (flashlightListener != null) {
            flashlightListener.cleanUpAll();
        }
        if (atmosphereManager != null) {
            atmosphereManager.stop();
        }
        if (httpServer != null) {
            httpServer.stop(0);
            getLogger().info("Resource pack HTTP server stopped.");
        }
        if (hologramManager != null) {
            hologramManager.clearLobbyHologram();
        }
        if (scoreboardManager != null) {
            scoreboardManager.clearAll();
        }
        getLogger().info("Secret Neighbor Map Loader Disabled!");
    }

    private void startHttpServer() {
        int port = getConfig().getInt("resourcepack-port", 22285);
        try {
            File file = new File(getDataFolder(), "resourcepack.zip");
            getDataFolder().mkdirs();
            saveResource("resourcepack.zip", true);

            httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/resourcepack.zip", exchange -> {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });
            httpServer.setExecutor(null);
            httpServer.start();
            getLogger().info("Resource pack HTTP server started on port " + port);
        } catch (Exception e) {
            getLogger().severe("Could not start resource pack HTTP server on port " + port + ": " + e.getMessage());
        }
    }

    public void sendResourcePack(Player player) {
        String host = getConfig().getString("resourcepack-host", "basic-7.alstore.space");
        int port = getConfig().getInt("resourcepack-port", 22285);

        // Auto-detect local connection and fallback to localhost/127.0.0.1 to avoid NAT loopback issues
        if (player.getAddress() != null) {
            String playerIp = player.getAddress().getAddress().getHostAddress();
            if (playerIp.equals("127.0.0.1") || playerIp.equals("0:0:0:0:0:0:0:1") || playerIp.startsWith("192.168.") || playerIp.startsWith("10.") || playerIp.startsWith("172.16.")) {
                host = "127.0.0.1";
                try {
                    if (!playerIp.equals("127.0.0.1") && !playerIp.equals("0:0:0:0:0:0:0:1")) {
                        host = java.net.InetAddress.getLocalHost().getHostAddress();
                    }
                } catch (Exception ignored) {}
            }
        }

        String url = "http://" + host + ":" + port + "/resourcepack.zip?v=" + System.currentTimeMillis();
        try {
            player.setResourcePack(url);
            getLogger().info("Sent resource pack to " + player.getName() + " using host: " + host + ":" + port);
        } catch (Exception e) {
            getLogger().warning("Failed to send resource pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public DrawerManager getDrawerManager() {
        return drawerManager;
    }

    public FridgeManager getFridgeManager() {
        return fridgeManager;
    }

    public WardrobeManager getWardrobeManager() {
        return wardrobeManager;
    }

    public com.secretneighbor.game.KeyManager getKeyManager() {
        return keyManager;
    }

    public AtmosphereManager getAtmosphereManager() {
        return atmosphereManager;
    }

    public com.secretneighbor.game.HologramManager getHologramManager() {
        return hologramManager;
    }

    public com.secretneighbor.listener.NeighborAbilityListener getNeighborAbilityListener() {
        return neighborAbilityListener;
    }

    public com.secretneighbor.listener.ChildAbilitiesListener getChildAbilitiesListener() {
        return childAbilitiesListener;
    }

    public com.secretneighbor.listener.KnockoutPacketHandler getKnockoutPacketHandler() {
        return knockoutPacketHandler;
    }

    public com.secretneighbor.game.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
