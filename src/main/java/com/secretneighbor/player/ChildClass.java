package com.secretneighbor.player;

public enum ChildClass {
    BRAVE("Brave", "§c⚾ Baseball & Counter-Strike", 1200, java.util.Arrays.asList(
        "§7- §fStun Neighbor dengan bola kasti",
        "§7- §fKabur lebih cepat saat diserang"
    )),
    DETECTIVE("Detective", "§6🔍 Field Intel & Intuition", 1201, java.util.Arrays.asList(
        "§7- §fScan area untuk mencari kunci terdekat",
        "§7- §fMelihat kunci yang tersisa jika 2 kunci sudah masuk"
    )),
    BAGGER("Bagger", "§d🎒 3 Slots, Heavy Bones & Strong Knees", 1202, java.util.Arrays.asList(
        "§7- §fMembawa lebih banyak barang (GUI Backpack)",
        "§7- §fMemperlambat Neighbor saat ditangkap",
        "§7- §fMengurangi damage jatuh (Resistance)"
    )),
    LEADER("Leader", "§e⚡ Inspiration & Team Work", 1203, java.util.Arrays.asList(
        "§7- §fMemberikan speed boost ke teman sekitar",
        "§7- §fMembutakan Neighbor jika berada di dekatnya",
        "§7- §fMengurangi durasi stun"
    )),
    INVENTOR("Inventor", "§9🔧 Metal Detector & Tinkering", 1204, java.util.Arrays.asList(
        "§7- §fMendeteksi kunci dan gear dari balik dinding",
        "§7- §fMembuka pintu akses yang terkunci"
    )),
    SCOUT("Scout", "§a🎯 Slingshot & Nut Ammo", 1205, java.util.Arrays.asList(
        "§7- §fMenembak ketapel untuk men-stun Neighbor",
        "§7- §fMenggunakan nut sebagai amunisi"
    )),
    ENGINEER("Engineer", "§e⚙ Tripwire Sensors & Barricade", 1206, java.util.Arrays.asList(
        "§7- §fMemasang sensor yang mendeteksi Neighbor",
        "§7- §fDapat membarikade jalan"
    )),
    GAMER("Gamer", "§b🎮 Decoy Hologram & Quick Reflexes", 1207, java.util.Arrays.asList(
        "§7- §fMembuat decoy hologram untuk mengecoh",
        "§7- §fBergerak lebih cepat (Permanent Speed)"
    ));

    private final String displayName;
    private final String description;
    private final int customModelData;
    private final java.util.List<String> details;

    ChildClass(String displayName, String description, int customModelData, java.util.List<String> details) {
        this.displayName = displayName;
        this.description = description;
        this.customModelData = customModelData;
        this.details = details;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getCustomModelData() {
        return customModelData;
    }
    
    public java.util.List<String> getDetails() {
        return details;
    }
}
