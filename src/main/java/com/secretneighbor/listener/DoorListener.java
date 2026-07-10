package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import com.secretneighbor.player.SNPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

/**
 * DoorListener — Handles wooden door interactions during gameplay.
 *
 * In the real Secret Neighbor game:
 * - All wooden doors can be opened and closed by any player
 * - Door sounds are audible from a distance (15+ blocks)
 * - This provides tactical information: the Neighbor can hear children opening doors
 * - Children can hear the Neighbor approaching via door sounds
 * - Iron doors are special (key-locked basement door)
 * - Copper doors are keycard-locked (handled separately in KeyManager)
 */
public class DoorListener implements Listener {

    private final SecretNeighborPlugin plugin;

    // All wooden door materials that should be interactable
    private static final Set<Material> WOODEN_DOORS = Set.of(
            Material.OAK_DOOR,
            Material.SPRUCE_DOOR,
            Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.MANGROVE_DOOR,
            Material.CHERRY_DOOR,
            Material.BAMBOO_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR
    );

    public DoorListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // Only handle wooden doors
        if (!WOODEN_DOORS.contains(block.getType())) return;

        // Only during active game
        if (plugin.getGameManager().getState() != com.secretneighbor.game.GameState.IN_GAME) return;

        Player player = event.getPlayer();
        SNPlayer snp = plugin.getGameManager().getPlayers().get(player.getUniqueId());
        if (snp == null || !snp.isAlive()) return;

        // Let vanilla handle the actual door toggle, we just add loud sound effects
        // The door open/close state will change naturally

        // Determine if door is currently open or closed
        if (block.getBlockData() instanceof Door doorData) {
            boolean wasOpen = doorData.isOpen();
            Sound doorSound;
            float volume;
            float pitch;

            if (wasOpen) {
                // Door is being CLOSED
                doorSound = Sound.BLOCK_WOODEN_DOOR_CLOSE;
                volume = 1.5f;
                pitch = 0.8f + (float)(Math.random() * 0.2);
            } else {
                // Door is being OPENED
                doorSound = Sound.BLOCK_WOODEN_DOOR_OPEN;
                volume = 1.5f;
                pitch = 0.9f + (float)(Math.random() * 0.2);
            }

            // Play the door sound with EXTENDED range (15 blocks) so other players can hear
            // Default Minecraft door sound range is ~16 blocks, but we amplify it
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), doorSound, volume, pitch);

            // Additional creak sound for atmosphere
            if (!wasOpen) {
                // Doors opening get an extra creak
                block.getWorld().playSound(
                    block.getLocation().add(0.5, 0.5, 0.5),
                    Sound.BLOCK_FENCE_GATE_OPEN,
                    0.4f,
                    0.3f + (float)(Math.random() * 0.3)
                );
            }
        }
    }
}
