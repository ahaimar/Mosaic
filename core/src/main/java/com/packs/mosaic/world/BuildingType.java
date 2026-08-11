package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;

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

    public BuildingType(String id, String displayName, Category category,
                        int widthCells, int heightCells,
                        Color placeholderColor, int starsToUnlock) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.widthCells = widthCells;
        this.heightCells = heightCells;
        this.placeholderColor = placeholderColor;
        this.starsToUnlock = starsToUnlock;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Category getCategory() { return category; }
    public int getWidthCells() { return widthCells; }
    public int getHeightCells() { return heightCells; }
    public Color getPlaceholderColor() { return placeholderColor; }
    public int getStarsToUnlock() { return starsToUnlock; }
}
