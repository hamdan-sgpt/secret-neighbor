package com.secretneighbor.placeholder;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.game.GameManager;
import com.secretneighbor.game.GameState;
import com.secretneighbor.player.SNPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SecretNeighborPlaceholderExpansion extends PlaceholderExpansion {

    private final SecretNeighborPlugin plugin;

    public SecretNeighborPlaceholderExpansion(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "secretneighbor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SecretNeighbor Team";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return "";

        switch (params.toLowerCase()) {
            case "state":
                GameState state = gameManager.getState();
                if (state == null) return "Idle";
                return switch (state) {
                    case IDLE -> "Idle";
                    case LOBBY -> "Lobby";
                    case IN_GAME -> "In Game";
                    case ENDING -> "Ending";
                };
            case "time":
                if (gameManager.getState() != GameState.IN_GAME) {
                    return "00:00";
                }
                int timeSecs = gameManager.getGameTimeSeconds();
                int mins = timeSecs / 60;
                int secs = timeSecs % 60;
                return String.format("%02d:%02d", mins, secs);
            case "keys_inserted":
                if (plugin.getKeyManager() == null) return "0";
                return String.valueOf(plugin.getKeyManager().getKeysInserted());
            case "keys_needed":
                if (plugin.getKeyManager() == null) return "6";
                return String.valueOf(Math.max(0, 6 - plugin.getKeyManager().getKeysInserted()));
            case "players_alive":
                int aliveCount = 0;
                for (SNPlayer snp : gameManager.getPlayers().values()) {
                    if (snp.isChild() && snp.isAlive()) {
                        aliveCount++;
                    }
                }
                return String.valueOf(aliveCount);
            case "players_total":
                return String.valueOf(gameManager.getPlayerCount());
        }

        // Placeholders requiring player-specific context
        if (player == null) return "";
        SNPlayer snp = gameManager.getPlayers().get(player.getUniqueId());
        if (snp == null) return "None";

        switch (params.toLowerCase()) {
            case "role":
                if (snp.getRole() == null) return "None";
                return snp.isNeighbor() ? "Neighbor" : "Child";
            case "class":
                if (snp.getChildClass() == null) return "None";
                return snp.getChildClass().getDisplayName();
            case "alive":
                if (!snp.isAlive()) return "Dead";
                return snp.isKnocked() ? "Knocked" : "Alive";
        }

        return null;
    }
}
