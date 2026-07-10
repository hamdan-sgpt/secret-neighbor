package com.secretneighbor.listener;

import com.secretneighbor.SecretNeighborPlugin;
import org.bukkit.event.Listener;

/**
 * Brave Listener - Holds Counter-Strike passive checks or other passive listeners if needed.
 * Active Baseball swing is unified under F key handler in ChildAbilitiesListener.
 */
public class BraveListener implements Listener {

    private final SecretNeighborPlugin plugin;

    public BraveListener(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }
}
