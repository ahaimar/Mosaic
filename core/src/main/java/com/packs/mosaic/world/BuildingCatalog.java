package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Fixed registry of starter BuildingTypes (spec: "Initially include...").
 * Built once, looked up by id. Adding a new object later means adding
 * one line here — no other class needs to change (spec's reusability
 * requirement).
 */
public final class BuildingCatalog {

    private static final ObjectMap<String, BuildingType> BY_ID = new ObjectMap<>();
    private static final Array<BuildingType> ALL = new Array<>();

    static {
        register(new BuildingType("small_house", "Small House", BuildingType.Category.BUILDING,
            1, 1, new Color(0.95f, 0.6f, 0.4f, 1f), 0));
        register(new BuildingType("large_house", "Large House", BuildingType.Category.BUILDING,
            2, 1, new Color(0.85f, 0.45f, 0.35f, 1f), 0));
        register(new BuildingType("shop", "Shop", BuildingType.Category.BUILDING,
            1, 1, new Color(0.95f, 0.8f, 0.3f, 1f), 25));
        register(new BuildingType("school", "School", BuildingType.Category.BUILDING,
            2, 2, new Color(0.9f, 0.5f, 0.6f, 1f), 25));
        register(new BuildingType("playground", "Playground", BuildingType.Category.BUILDING,
            2, 1, new Color(1f, 0.7f, 0.8f, 1f), 50));

        register(new BuildingType("tree", "Tree", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.3f, 0.65f, 0.3f, 1f), 0));
        register(new BuildingType("bush", "Bush", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.35f, 0.7f, 0.35f, 1f), 10));
        register(new BuildingType("flower", "Flower", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.9f, 0.4f, 0.7f, 1f), 10));
        register(new BuildingType("rock", "Rock", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.6f, 0.6f, 0.6f, 1f), 10));
        register(new BuildingType("bench", "Bench", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.55f, 0.4f, 0.25f, 1f), 10));
        register(new BuildingType("street_lamp", "Street Lamp", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.8f, 0.75f, 0.3f, 1f), 10));

        register(new BuildingType("road_straight", "Straight Road", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.4f, 0.4f, 0.4f, 1f), 0));
        register(new BuildingType("road_corner", "Corner Road", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.45f, 0.45f, 0.45f, 1f), 0));
        register(new BuildingType("road_cross", "Crossroad", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.35f, 0.35f, 0.35f, 1f), 0));
        register(new BuildingType("bridge_small", "Small Bridge", BuildingType.Category.INFRASTRUCTURE,
            2, 1, new Color(0.6f, 0.5f, 0.35f, 1f), 25));
    }

    private BuildingCatalog() {
    }

    private static void register(BuildingType type) {
        BY_ID.put(type.getId(), type);
        ALL.add(type);
    }

    public static BuildingType get(String id) {
        return BY_ID.get(id);
    }

    public static Array<BuildingType> getAll() {
        return ALL;
    }

    public static Array<BuildingType> getByCategory(BuildingType.Category category) {
        Array<BuildingType> result = new Array<>();
        for (BuildingType type : ALL) {
            if (type.getCategory() == category) result.add(type);
        }
        return result;
    }
}
