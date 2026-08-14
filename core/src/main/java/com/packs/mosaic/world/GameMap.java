package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

/**
 * Immutable definition of a playable world (Task 3). Each map is a fully
 * independent building space — it has its own {@link VillageGrid} snapshot,
 * its own ground theme, baked-in decorations, ambient particle effect and a
 * set of map-specific buildings that only appear while building there.
 *
 * Data-only: rendering consumes these values via MapGroundFactory and
 * AmbientEffect, and persistence keys off {@link #getId()}.
 */
public class GameMap {

    /** Procedural ground style, consumed by MapGroundFactory. */
    public enum Terrain {
        GRASS, FOREST, SAND, ROCK, SNOW, VOLCANIC, TROPICAL
    }

    /** Ambient particle style, consumed by AmbientEffect. */
    public enum Effect {
        NONE, SNOW, LEAVES, EMBERS, MIST, RAIN, GLINTS,
        /** Task 4 — spring: light rain plus fluttering butterflies. */
        SPRING,
        /** Task 4 — summer: birds gliding across the sky. */
        BIRD
    }

    private final String id;
    private final String nameKey;
    private final String descriptionKey;
    private final Terrain terrain;
    private final Effect effect;
    private final Color groundColor;
    private final Color accentColor;
    private final Color clearColor;
    private final Color gridLineColor;

    public GameMap(String id, String nameKey, String descriptionKey,
                   Terrain terrain, Effect effect,
                   Color groundColor, Color accentColor,
                   Color clearColor, Color gridLineColor) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.terrain = terrain;
        this.effect = effect;
        this.groundColor = groundColor;
        this.accentColor = accentColor;
        this.clearColor = clearColor;
        this.gridLineColor = gridLineColor;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public Terrain getTerrain() { return terrain; }
    public Effect getEffect() { return effect; }
    public Color getGroundColor() { return groundColor; }
    public Color getAccentColor() { return accentColor; }
    public Color getClearColor() { return clearColor; }
    public Color getGridLineColor() { return gridLineColor; }

    /** Every catalog building the player can place on this map (base + map-specific). */
    public Array<BuildingType> getAvailableBuildings() {
        Array<BuildingType> result = new Array<>();
        for (BuildingType type : BuildingCatalog.getAll()) {
            if (type.isAvailableOn(id)) result.add(type);
        }
        return result;
    }

    @Override
    public String toString() {
        return "GameMap{" + id + '}';
    }
}
