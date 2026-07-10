package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final SecretNeighborPlugin plugin;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public ScoreboardManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public void setupScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        
        Objective obj = board.registerNewObjective("sn_board", Criteria.DUMMY, Component.text("§6§lSECRET NEIGHBOR"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Create teams for lines to prevent flickering (10 lines total)
        for (int i = 1; i <= 10; i++) {
            Team team = board.registerNewTeam("line_" + i);
            String entry = getEntryForLine(i);
            team.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }

        player.setScoreboard(board);
        playerBoards.put(player.getUniqueId(), board);
        updateScoreboard(player);
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        playerBoards.remove(player.getUniqueId());
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().getPlayers().containsKey(p.getUniqueId())) {
                if (!playerBoards.containsKey(p.getUniqueId())) {
                    setupScoreboard(p);
                } else {
                    updateScoreboard(p);
                }
            } else {
                if (playerBoards.containsKey(p.getUniqueId())) {
                    removeScoreboard(p);
                }
            }
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(playerBoards.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                removeScoreboard(p);
            }
        }
        playerBoards.clear();
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) return;

        GameManager gameManager = plugin.getGameManager();
        GameState state = gameManager.getState();
        SNPlayer snp = gameManager.getPlayer(player.getUniqueId());

        List<String> lines = new ArrayList<>();
        lines.add("§7------------------ "); // line 10

        if (state == GameState.LOBBY) {
            lines.add("§fPemain: §a" + gameManager.getPlayerCount() + "/10");
            lines.add("§fStatus: §eMenunggu Host...");
            lines.add(" ");
            lines.add("§7Mulai game dengan:");
            lines.add("§e/sn start");
        } else if (state == GameState.IN_GAME) {
            int timeSecs = gameManager.getGameTimeSeconds();
            int mins = timeSecs / 60;
            int secs = timeSecs % 60;
            String timeStr = String.format("%02d:%02d", mins, secs);

            int keysInserted = plugin.getKeyManager() != null ? plugin.getKeyManager().getKeysInserted() : 0;
            
            int childrenAlive = 0;
            for (SNPlayer s : gameManager.getPlayers().values()) {
                if (s.isChild() && s.isAlive()) childrenAlive++;
            }

            lines.add("§fSisa Waktu: §e" + timeStr);
            lines.add("§fKunci: §a" + keysInserted + "/6");
            lines.add("§fAnak Hidup: §b" + childrenAlive);
            lines.add("  ");

            if (snp != null) {
                if (snp.isNeighbor()) {
                    lines.add("§fPeran: §c§lNEIGHBOR");
                    lines.add("§fMode: " + (snp.isBapakeMode() ? "§4§lKILL MODE" : "§d§lDISGUISE"));
                } else {
                    lines.add("§fPeran: §aChild");
                    lines.add("§fKelas: §d" + (snp.getChildClass() != null ? snp.getChildClass().getDisplayName() : "None"));
                }
            }
        } else if (state == GameState.ENDING) {
            lines.add("§fStatus: §c§lGame Selesai!");
            lines.add("§fMeregenerasi map...");
            lines.add("   ");
        } else {
            lines.add("§fStatus: §8Idle");
            lines.add("    ");
        }

        lines.add("§7------------------  ");
        lines.add("§eplugin by hams");

        // Set the lines from top to bottom (teams line_10 down to line_1)
        int score = 10;
        for (int i = 0; i < 10; i++) {
            Team team = board.getTeam("line_" + (score - i));
            if (team != null) {
                if (i < lines.size()) {
                    team.prefix(Component.text(lines.get(i)));
                } else {
                    team.prefix(Component.text(""));
                }
            }
        }
    }

    private String getEntryForLine(int line) {
        return "§" + line + "§r";
    }
}
