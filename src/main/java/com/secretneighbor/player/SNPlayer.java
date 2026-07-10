package com.secretneighbor.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SNPlayer {

    private final UUID uuid;
    private final String name;
    private Role role;
    private ChildClass childClass;
    private boolean alive;
    private boolean grabbed;
    private int grabProgress;
    private int hitsTaken;
    private int keysFound;
    private boolean trapped;
    private int trapTicks;
    private boolean disguised = false;
    private String originalSkinValue;
    private String originalSkinSignature;
    private String disguiseValue = null;
    private String disguiseSignature = null;
    private String disguiseName = null;
    private boolean bapakeMode = false;
    private boolean knocked = false;
    private UUID grabbedByUuid = null;
    private int struggleCount = 0;
    private long grabStartTime = 0;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public SNPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.role = Role.CHILD;
        this.childClass = null;
        this.alive = true;
        this.grabbed = false;
        this.grabProgress = 0;
        this.hitsTaken = 0;
        this.keysFound = 0;
        this.trapped = false;
        this.trapTicks = 0;
        this.disguised = false;
        this.originalSkinValue = null;
        this.originalSkinSignature = null;
        this.disguiseValue = null;
        this.disguiseSignature = null;
        this.disguiseName = null;
        this.bapakeMode = false;
        this.knocked = false;
        this.grabbedByUuid = null;
        this.struggleCount = 0;
        this.grabStartTime = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public ChildClass getChildClass() { return childClass; }
    public void setChildClass(ChildClass childClass) { this.childClass = childClass; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isKnocked() { return knocked; }
    public void setKnocked(boolean knocked) { this.knocked = knocked; }

    public boolean isGrabbed() { return grabbed; }
    public void setGrabbed(boolean grabbed) { this.grabbed = grabbed; }

    public int getGrabProgress() { return grabProgress; }
    public void setGrabProgress(int grabProgress) { this.grabProgress = grabProgress; }
    public void incrementGrabProgress() { this.grabProgress++; }

    public UUID getGrabbedByUuid() { return grabbedByUuid; }
    public void setGrabbedByUuid(UUID grabbedByUuid) { this.grabbedByUuid = grabbedByUuid; }

    public int getStruggleCount() { return struggleCount; }
    public void setStruggleCount(int struggleCount) { this.struggleCount = struggleCount; }
    public void incrementStruggleCount() { this.struggleCount++; }

    public long getGrabStartTime() { return grabStartTime; }
    public void setGrabStartTime(long grabStartTime) { this.grabStartTime = grabStartTime; }

    public int getHitsTaken() { return hitsTaken; }
    public void incrementHitsTaken() { this.hitsTaken++; }
    public void resetHitsTaken() { this.hitsTaken = 0; }

    public int getKeysFound() { return keysFound; }
    public void incrementKeysFound() { this.keysFound++; }

    public boolean isTrapped() { return trapped; }
    public void setTrapped(boolean trapped) { this.trapped = trapped; }

    public int getTrapTicks() { return trapTicks; }
    public void setTrapTicks(int trapTicks) { this.trapTicks = trapTicks; }
    public void decrementTrapTicks() { if (this.trapTicks > 0) this.trapTicks--; }

    public boolean isNeighbor() { return role == Role.NEIGHBOR; }
    public boolean isChild() { return role == Role.CHILD; }

    public boolean isDisguised() { return disguised; }
    public void setDisguised(boolean disguised) { this.disguised = disguised; }

    // --- Cooldowns ---
    public boolean isOnCooldown(String ability) {
        Long expiry = cooldowns.get(ability);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    public void setCooldown(String ability, int seconds) {
        cooldowns.put(ability, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getRemainingCooldown(String ability) {
        Long expiry = cooldowns.get(ability);
        if (expiry == null) return 0;
        long diff = expiry - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }

    public Map<String, Integer> getActiveCooldowns() {
        Map<String, Integer> active = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            long diff = entry.getValue() - now;
            if (diff > 0) {
                active.put(entry.getKey(), (int)(diff / 1000) + 1);
            }
        }
        return active;
    }

    public void clearCooldowns() { cooldowns.clear(); }

    public String getOriginalSkinValue() { return originalSkinValue; }
    public void setOriginalSkinValue(String originalSkinValue) { this.originalSkinValue = originalSkinValue; }

    public String getOriginalSkinSignature() { return originalSkinSignature; }
    public void setOriginalSkinSignature(String originalSkinSignature) { this.originalSkinSignature = originalSkinSignature; }

    public String getDisguiseValue() { return disguiseValue; }
    public void setDisguiseValue(String disguiseValue) { this.disguiseValue = disguiseValue; }

    public String getDisguiseSignature() { return disguiseSignature; }
    public void setDisguiseSignature(String disguiseSignature) { this.disguiseSignature = disguiseSignature; }

    public String getDisguiseName() { return disguiseName; }
    public void setDisguiseName(String disguiseName) { this.disguiseName = disguiseName; }

    public boolean isBapakeMode() { return bapakeMode; }
    public void setBapakeMode(boolean bapakeMode) { this.bapakeMode = bapakeMode; }

    public void reset() {
        this.role = Role.CHILD;
        this.childClass = null;
        this.alive = true;
        this.grabbed = false;
        this.grabProgress = 0;
        this.hitsTaken = 0;
        this.keysFound = 0;
        this.trapped = false;
        this.trapTicks = 0;
        this.disguised = false;
        this.originalSkinValue = null;
        this.originalSkinSignature = null;
        this.disguiseValue = null;
        this.disguiseSignature = null;
        this.disguiseName = null;
        this.bapakeMode = false;
        this.knocked = false;
        this.grabbedByUuid = null;
        this.struggleCount = 0;
        this.grabStartTime = 0;
        clearCooldowns();
    }
}
