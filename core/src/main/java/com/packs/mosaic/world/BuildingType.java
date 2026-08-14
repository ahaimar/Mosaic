package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

/**
 * Static definition of a placeable object type — the reusable "recipe"
 * (matches the spec's BuildingData resource). One instance per type,
 * shared by every placed BuildingObject of that type. Add new types by
 * adding enum-like constants via a registry (next: BuildingCatalog),
 * not by subclassing.
 */
public class BuildingType {

    public enum Category {
        BUILDING, CONSTRUCTION, ENVIRONMENT, INFRASTRUCTURE
    }

    private final String id;
    private final String displayName;
    private final Category category;
    private final int widthCells;
    private final int heightCells;
    private final Color placeholderColor;
    private final int starsToUnlock;
    private final boolean discoveryReward;
    /** Map ids this type is placeable on; null/empty means available on every map. */
    private final Array<String> mapIds;

    public BuildingType(String id, String displayName, Category category,
                        int widthCells, int heightCells,
                        Color placeholderColor, int starsToUnlock) {
        this(id, displayName, category, widthCells, heightCells, placeholderColor, starsToUnlock, false, null);
    }

    /**
     * Full constructor. Discovery-reward types are hidden from the star
     * unlock system (their starsToUnlock is effectively ignored) and only
     * become available once their discovery/recipe is met — DiscoveryManager
     * hands them to the toolbar directly.
     */
    public BuildingType(String id, String displayName, Category category,
                        int widthCells, int heightCells,
                        Color placeholderColor, int starsToUnlock, boolean discoveryReward) {
        this(id, displayName, category, widthCells, heightCells, placeholderColor, starsToUnlock, discoveryReward, null);
    }

    /**
     * Constructor with a map restriction. Passing null (or an empty array)
     * makes the type available on every map; otherwise the type only shows
     * up in the toolbar while the player is building on one of the listed
     * maps (e.g. palm trees only on the Beach).
     */
    public BuildingType(String id, String displayName, Category category,
                        int widthCells, int heightCells,
                        Color placeholderColor, int starsToUnlock,
                        boolean discoveryReward, Array<String> mapIds) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.widthCells = widthCells;
        this.heightCells = heightCells;
        this.placeholderColor = placeholderColor;
        this.starsToUnlock = starsToUnlock;
        this.discoveryReward = discoveryReward;
        this.mapIds = mapIds;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Category getCategory() { return category; }
    public int getWidthCells() { return widthCells; }
    public int getHeightCells() { return heightCells; }
    public Color getPlaceholderColor() { return placeholderColor; }
    public int getStarsToUnlock() { return starsToUnlock; }

    /** True for buildings that unlock via discoveries/recipes, never via stars. */
    public boolean isDiscoveryReward() { return discoveryReward; }

    /**
     * True if this type can be placed on the given map. Types without a map
     * restriction are available everywhere; map-specific types only on their
     * own maps. A null map id (no active map) is treated as "everywhere".
     */
    public boolean isAvailableOn(String mapId) {
        if (mapId == null || mapIds == null || mapIds.size == 0) return true;
        return mapIds.contains(mapId, false);
    }

    public Array<String> getMapIds() { return mapIds; }
}
