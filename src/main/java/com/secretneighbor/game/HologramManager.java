package com.secretneighbor.game;

import com.secretneighbor.SecretNeighborPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;

public class HologramManager {
    private final SecretNeighborPlugin plugin;
    private final List<ArmorStand> lobbyHolograms = new ArrayList<>();

    public HologramManager(SecretNeighborPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnLobbyHologram() {
        clearLobbyHologram(); // clear existing first
        
        Location lobbyLoc = plugin.getLocationManager().getLobby();
        if (lobbyLoc == null || lobbyLoc.getWorld() == null) {
            return;
        }

        // 1. TUTORIAL UTAMA (Tengah)
        Location centerLoc = lobbyLoc.clone().add(0.5, 3.5, 0.5); 
        String[] centerLines = {
            "§6§lSecret Neighbor",
            " ",
            "§e§lTujuan Utama:",
            "§fAnak-anak harus mencari §aKunci §funtuk membuka pintu basement.",
            "§fNeighbor menyamar dan harus mencegah anak-anak masuk.",
            " ",
            "§fGunakan §bCompass §funtuk memilih kelas karaktermu!",
            "§fJika mati, tunggu hingga rekanmu menyelamatkanmu."
        };
        spawnHologramGroup(centerLoc, centerLines);

        // 2. INTERAKSI & ITEM (Geser X + 4)
        Location itemLoc = lobbyLoc.clone().add(4.5, 3.5, 0.5);
        String[] itemLines = {
            "§b§lInteraksi & Item",
            " ",
            "§f1. §eBarang di lantai: §fKlik kanan pada item untuk mengambilnya.",
            "§f2. §eMelempar Barang: §fTekan §cTombol Drop (Q) §funtuk melempar.",
            "§f3. §eLaci & Kulkas: §fKlik kanan untuk mencari item di dalamnya.",
            "§f4. §eLemari Besar: §fTekan §bSNEAK (Shift) §fdi depan lemari untuk sembunyi.",
            "§f5. §eSenter (Flashlight): §fGanti ke tangan utama untuk nyala/mati.",
            "§f6. §eBarang Berat: §fBeberapa barang memperlambat jalanmu."
        };
        spawnHologramGroup(itemLoc, itemLines);

        // 3. TIPS & PERAN (Geser X - 4)
        Location roleLoc = lobbyLoc.clone().add(-3.5, 3.5, 0.5);
        String[] roleLines = {
            "§c§lTips & Peran",
            " ",
            "§a§lPeran Anak:",
            "§f- Manfaatkan skill unik kelasmu (baca info di Compass).",
            "§f- Tetap bersama! Jangan berpencar sendirian.",
            "§f- Lempar barang ke Neighbor untuk men-stun dia.",
            " ",
            "§c§lPeran Neighbor:",
            "§f- Menyamarlah dengan baik, jangan gunakan skill jika ada yang melihat.",
            "§f- Incar anak yang sedang terpisah dari kelompoknya.",
            "§f- Jaga pintu basement!"
        };
        spawnHologramGroup(roleLoc, roleLines);
    }

    private void spawnHologramGroup(Location startLoc, String[] lines) {
        Location currentLoc = startLoc.clone();
        for (String line : lines) {
            ArmorStand as = currentLoc.getWorld().spawn(currentLoc, ArmorStand.class, armorStand -> {
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setMarker(true);
                armorStand.setCustomNameVisible(true);
                armorStand.customName(Component.text(line));
            });
            lobbyHolograms.add(as);
            
            // Move down for the next line
            currentLoc.subtract(0, 0.3, 0);
        }
    }

    public void clearLobbyHologram() {
        for (ArmorStand as : lobbyHolograms) {
            if (as != null && !as.isDead()) {
                as.remove();
            }
        }
        lobbyHolograms.clear();
    }
}
