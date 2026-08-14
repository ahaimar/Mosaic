package com.packs.mosaic.persist;

import com.badlogic.gdx.utils.Array;
import com.packs.mosaic.world.econ.EconomyState;

/**
 * Plain serializable snapshot of a game state, persisted as JSON by
 * SaveManager. Deliberately stores only derived/state data: placed
 * objects (type id + cell + rotation), total stars, and the active
 * challenge id. Unlocks are never stored — PlayerProgress always
 * re-derives them from the star total, so a save file cannot drift out
 * of sync with the catalog.
 */
public class SaveData {

    /** One placed building, enough to fully restore it. */
    public static class PlacedObject {
        public String typeId;
        public int col;
        public int row;
        public int rotationDegrees;

        public PlacedObject() {
        }

        public PlacedObject(String typeId, int col, int row, int rotationDegrees) {
            this.typeId = typeId;
            this.col = col;
            this.row = row;
            this.rotationDegrees = rotationDegrees;
        }
    }

    /**
     * One world's own building space (Task 3): each map keeps an independent
     * grid, so switching worlds never disturbs another map's buildings.
     */
    public static class MapSaveData {
        public String mapId;
        public Array<PlacedObject> placedObjects = new Array<>();

        public MapSaveData() {
        }
    }

    public Array<PlacedObject> placedObjects = new Array<>();
    public int totalStars;
    public String currentChallengeId;
    /** Ids of every discovery/recipe already unlocked, so progress survives a reload. */
    public Array<String> discoveredUnlocks = new Array<>();
    /** Per-world building spaces; empty on legacy saves (which lived in {@link #placedObjects}). */
    public Array<MapSaveData> maps = new Array<>();
    /** Active visual season id (Task 4); null/unknown on legacy saves falls back to Spring. */
    public String season;
    /** Economic simulation snapshot (Task 5); null on legacy saves starts a fresh economy. */
    public EconomyState economy;

    public SaveData() {
    }
}
