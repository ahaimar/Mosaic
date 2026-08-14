package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

/**
 * Tracks which special buildings the player has discovered (count-based
 * discoveries and combination recipes). Re-reads the grid like
 * ChallengeManager: any unmet unlock whose requirements are now satisfied
 * is marked unlocked — permanently — and listeners hear about it. Rewards
 * unlocked here are handed to the toolbar via
 * {@link #getRewardUnlockedTypes()}, so hidden buildings only become
 * placeable once discovered. Unlocked ids are persisted by SaveData and
 * restored with {@link #restoreUnlocked(Array)}.
 */
public class DiscoveryManager {

    /** Receives unlock events (toasts, panel refreshes, toolbar updates). */
    public interface DiscoveryListener {
        default void onUnlocked(UnlockDefinition unlock) {}
    }

    private final VillageGrid grid;
    private final ObjectSet<String> unlockedIds = new ObjectSet<>();
    private final Array<DiscoveryListener> listeners = new Array<>();

    public DiscoveryManager(VillageGrid grid) {
        this.grid = grid;
    }

    public void addListener(DiscoveryListener listener) {
        listeners.add(listener);
    }

    public ObjectMap<String, Integer> getCurrentCounts() {
        return GridCounts.count(grid);
    }

    /**
     * Re-reads the grid and unlocks every currently-unmet definition whose
     * requirements are satisfied. Unlocks are permanent: once marked, they
     * are never re-evaluated or revoked. Safe to call after any grid
     * mutation (placement, deletion, undo, redo).
     */
    public void checkUnlocks() {
        ObjectMap<String, Integer> counts = getCurrentCounts();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            if (unlockedIds.contains(unlock.getId())) continue;
            if (unlock.isMet(counts)) {
                unlockedIds.add(unlock.getId());
                for (DiscoveryListener l : listeners) l.onUnlocked(unlock);
            }
        }
    }

    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }

    public boolean isUnlocked(UnlockDefinition unlock) {
        return isUnlocked(unlock.getId());
    }

    /** Unlocked definitions in catalog order. */
    public Array<UnlockDefinition> getUnlocked() {
        Array<UnlockDefinition> result = new Array<>();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            if (isUnlocked(unlock)) result.add(unlock);
        }
        return result;
    }

    /** Not-yet-unlocked definitions in catalog order. */
    public Array<UnlockDefinition> getLocked() {
        Array<UnlockDefinition> result = new Array<>();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            if (!isUnlocked(unlock)) result.add(unlock);
        }
        return result;
    }

    /** Every BuildingType unlocked through discoveries/recipes, for the toolbar. */
    public Array<BuildingType> getRewardUnlockedTypes() {
        Array<BuildingType> result = new Array<>();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            if (!isUnlocked(unlock)) continue;
            BuildingType type = BuildingCatalog.get(unlock.getRewardBuildingId());
            if (type != null) result.add(type);
        }
        return result;
    }

    /** Unlocked definition ids, in catalog order — for SaveData. */
    public Array<String> getUnlockedIds() {
        Array<String> result = new Array<>();
        for (UnlockDefinition unlock : UnlockCatalog.getAll()) {
            if (isUnlocked(unlock)) result.add(unlock.getId());
        }
        return result;
    }

    /** Marks already-discovered unlocks as unlocked (unknown ids are ignored). */
    public void restoreUnlocked(Array<String> ids) {
        if (ids == null) return;
        for (String id : ids) {
            if (UnlockCatalog.get(id) != null) {
                unlockedIds.add(id);
            }
        }
    }
}
