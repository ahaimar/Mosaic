package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;

/**
 * Tracks the player's total stars and derives which BuildingTypes are
 * currently unlocked from each type's starsToUnlock threshold (set in
 * BuildingCatalog). Single source of truth for progression — the
 * challenge system awards stars here, the selection menu asks here
 * before letting the player pick a type, and save/load persists just
 * the star total (unlocks are always re-derived, never stored
 * separately, so they can't drift out of sync).
 */
public class PlayerProgress {

    private int totalStars;

    public PlayerProgress() {
        this.totalStars = 0;
    }

    public PlayerProgress(int startingStars) {
        this.totalStars = startingStars;
    }

    public int getTotalStars() {
        return totalStars;
    }

    /** Adds stars (e.g. on challenge completion). Negative amounts are ignored — stars never decrease. */
    public void addStars(int amount) {
        if (amount > 0) {
            totalStars += amount;
        }
    }

    public boolean isUnlocked(BuildingType type) {
        return totalStars >= type.getStarsToUnlock();
    }

    /** All catalog types currently unlocked, for the selection menu to display. */
    public Array<BuildingType> getUnlockedTypes() {
        Array<BuildingType> unlocked = new Array<>();
        for (BuildingType type : BuildingCatalog.getAll()) {
            if (isUnlocked(type)) unlocked.add(type);
        }
        return unlocked;
    }

    /** Stars still needed until the next locked type unlocks, or -1 if everything is unlocked. Useful for a "next unlock" UI hint. */
    public int starsUntilNextUnlock() {
        int nextThreshold = -1;
        for (BuildingType type : BuildingCatalog.getAll()) {
            int threshold = type.getStarsToUnlock();
            if (threshold > totalStars && (nextThreshold == -1 || threshold < nextThreshold)) {
                nextThreshold = threshold;
            }
        }
        return nextThreshold == -1 ? -1 : nextThreshold - totalStars;
    }
}
