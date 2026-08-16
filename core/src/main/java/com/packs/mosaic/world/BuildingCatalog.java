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

        // Economic buildings (Task 5): producers and converters. Their
        // economic behaviour — outputs, inputs, workforce, upkeep, construction
        // cost — lives entirely in the EconomyData table, never on the type.
        register(new BuildingType("lumber_hut", "Lumber Hut", BuildingType.Category.BUILDING,
            2, 1, new Color(0.45f, 0.35f, 0.25f, 1f), 0));
        register(new BuildingType("stone_mine", "Stone Mine", BuildingType.Category.BUILDING,
            2, 1, new Color(0.5f, 0.5f, 0.55f, 1f), 0));
        register(new BuildingType("farm", "Farm", BuildingType.Category.BUILDING,
            2, 2, new Color(0.85f, 0.7f, 0.3f, 1f), 0));
        register(new BuildingType("workshop", "Workshop", BuildingType.Category.BUILDING,
            2, 1, new Color(0.6f, 0.5f, 0.4f, 1f), 0));
        register(new BuildingType("dairy", "Dairy", BuildingType.Category.BUILDING,
            2, 1, new Color(0.92f, 0.92f, 0.96f, 1f), 0));
        register(new BuildingType("coop", "Coop", BuildingType.Category.BUILDING,
            1, 1, new Color(0.85f, 0.6f, 0.4f, 1f), 0));
        register(new BuildingType("ranch", "Ranch", BuildingType.Category.BUILDING,
            2, 2, new Color(0.7f, 0.5f, 0.35f, 1f), 0));
        register(new BuildingType("iron_mine", "Iron Mine", BuildingType.Category.BUILDING,
            2, 1, new Color(0.55f, 0.45f, 0.5f, 1f), 0));
        register(new BuildingType("coal_mine", "Coal Mine", BuildingType.Category.BUILDING,
            2, 1, new Color(0.3f, 0.3f, 0.35f, 1f), 0));
        register(new BuildingType("smelter", "Smelter", BuildingType.Category.BUILDING,
            2, 1, new Color(0.75f, 0.35f, 0.25f, 1f), 0));
        register(new BuildingType("carpentry", "Carpentry", BuildingType.Category.BUILDING,
            2, 1, new Color(0.65f, 0.5f, 0.35f, 1f), 0));
        register(new BuildingType("machine_factory", "Machine Factory", BuildingType.Category.BUILDING,
            2, 1, new Color(0.5f, 0.55f, 0.7f, 1f), 0));
        register(new BuildingType("assembly_factory", "Assembly Factory", BuildingType.Category.BUILDING,
            2, 1, new Color(0.75f, 0.6f, 0.75f, 1f), 0));

        // Warehouses (Task 8): storage buildings that extend the settlement's
        // shared capacity and charge a fee per stored unit per tick.
        register(new BuildingType("warehouse", "Warehouse", BuildingType.Category.BUILDING,
            2, 1, new Color(0.6f, 0.5f, 0.3f, 1f), 0));
        register(new BuildingType("large_warehouse", "Large Warehouse", BuildingType.Category.BUILDING,
            2, 1, new Color(0.55f, 0.4f, 0.25f, 1f), 0));

        // Transport & logistics (Task 13): truck depots grow the delivery
        // fleet, roads shorten delivery time. Their economic effect lives in
        // the EconomyData table, never on the type.
        register(new BuildingType("truck_depot", "Truck Depot", BuildingType.Category.BUILDING,
            1, 1, new Color(0.45f, 0.45f, 0.7f, 1f), 0));
        register(new BuildingType("dirt_road", "Dirt Road", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.55f, 0.45f, 0.3f, 1f), 0));
        register(new BuildingType("cobbled_road", "Cobbled Road", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.6f, 0.55f, 0.45f, 1f), 0));

        // Energy production (Task 14): power plants feed the settlement's
        // energy network. Their output lives in the EconomyData table, never
        // on the type.
        register(new BuildingType("generator", "Generator", BuildingType.Category.BUILDING,
            1, 1, new Color(0.9f, 0.8f, 0.35f, 1f), 0));
        register(new BuildingType("power_plant", "Power Plant", BuildingType.Category.BUILDING,
            2, 2, new Color(0.55f, 0.5f, 0.9f, 1f), 0));
        register(new BuildingType("solar_plant", "Solar Plant", BuildingType.Category.BUILDING,
            2, 2, new Color(0.95f, 0.85f, 0.3f, 1f), 0));
        register(new BuildingType("advanced_power_plant", "Advanced Power Plant", BuildingType.Category.BUILDING,
            3, 2, new Color(0.6f, 0.45f, 0.85f, 1f), 0));

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

        // Discovery/recipe ingredients — unlocked via discoveries, never via stars.
        register(new BuildingType("garden", "Garden", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.45f, 0.75f, 0.4f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("fence", "Fence", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.72f, 0.6f, 0.4f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("tower", "Tower", BuildingType.Category.BUILDING,
            1, 1, new Color(0.55f, 0.45f, 0.8f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("crystal", "Crystal", BuildingType.Category.ENVIRONMENT,
            1, 1, new Color(0.5f, 0.8f, 0.95f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("magic_gate", "Magic Gate", BuildingType.Category.INFRASTRUCTURE,
            1, 1, new Color(0.35f, 0.3f, 0.65f, 1f), Integer.MAX_VALUE, true));

        // Discovery reward buildings.
        register(new BuildingType("flower_palace", "Flower Palace", BuildingType.Category.BUILDING,
            2, 2, new Color(0.95f, 0.5f, 0.75f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("grand_town_hall", "Grand Town Hall", BuildingType.Category.BUILDING,
            3, 2, new Color(0.85f, 0.6f, 0.35f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("royal_garden", "Royal Garden", BuildingType.Category.ENVIRONMENT,
            2, 2, new Color(0.35f, 0.7f, 0.4f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("crystal_quarry", "Crystal Quarry", BuildingType.Category.ENVIRONMENT,
            2, 2, new Color(0.55f, 0.7f, 0.85f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("lamp_plaza", "Lamp Plaza", BuildingType.Category.INFRASTRUCTURE,
            2, 1, new Color(0.9f, 0.8f, 0.4f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("bench_park", "Bench Park", BuildingType.Category.ENVIRONMENT,
            2, 2, new Color(0.55f, 0.45f, 0.3f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("bush_maze", "Bush Maze", BuildingType.Category.ENVIRONMENT,
            2, 2, new Color(0.3f, 0.6f, 0.35f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("highway", "Highway", BuildingType.Category.INFRASTRUCTURE,
            2, 1, new Color(0.35f, 0.35f, 0.4f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("university", "University", BuildingType.Category.BUILDING,
            3, 3, new Color(0.45f, 0.5f, 0.85f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("grand_market", "Grand Market", BuildingType.Category.BUILDING,
            3, 2, new Color(0.95f, 0.75f, 0.3f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("amusement_park", "Amusement Park", BuildingType.Category.BUILDING,
            3, 2, new Color(0.95f, 0.55f, 0.65f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("grand_bridge", "Grand Bridge", BuildingType.Category.INFRASTRUCTURE,
            3, 1, new Color(0.55f, 0.45f, 0.35f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("nature_reserve", "Nature Reserve", BuildingType.Category.ENVIRONMENT,
            3, 3, new Color(0.25f, 0.55f, 0.3f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("central_station", "Central Station", BuildingType.Category.INFRASTRUCTURE,
            3, 2, new Color(0.45f, 0.5f, 0.55f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("megalopolis_tower", "Megalopolis Tower", BuildingType.Category.BUILDING,
            2, 2, new Color(0.4f, 0.45f, 0.55f, 1f), Integer.MAX_VALUE, true));

        // Recipe reward buildings.
        register(new BuildingType("cozy_cottage", "Cozy Cottage", BuildingType.Category.BUILDING,
            2, 2, new Color(0.85f, 0.45f, 0.3f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("small_town", "Small Town", BuildingType.Category.BUILDING,
            3, 2, new Color(0.8f, 0.55f, 0.35f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("wizard_tower", "Wizard Tower", BuildingType.Category.BUILDING,
            2, 2, new Color(0.45f, 0.4f, 0.75f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("park_square", "Park Square", BuildingType.Category.BUILDING,
            2, 2, new Color(0.5f, 0.75f, 0.45f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("market_street", "Market Street", BuildingType.Category.INFRASTRUCTURE,
            3, 1, new Color(0.9f, 0.7f, 0.35f, 1f), Integer.MAX_VALUE, true));
        register(new BuildingType("village_center", "Village Center", BuildingType.Category.BUILDING,
            2, 2, new Color(0.7f, 0.5f, 0.6f, 1f), Integer.MAX_VALUE, true));

        // ── Map-specific buildings ─────────────────────────────────────────
        // Free starter types restricted to their own world (Task 3). A type
        // with a map restriction only appears in the toolbar while building
        // on that map; base types (no restriction) work everywhere.
        register(mapType("pine", "Pine Tree", BuildingType.Category.ENVIRONMENT,
            new Color(0.2f, 0.5f, 0.3f, 1f), "forest"));
        register(mapType("mushroom", "Mushroom", BuildingType.Category.ENVIRONMENT,
            new Color(0.9f, 0.35f, 0.3f, 1f), "forest"));
        register(mapType("cabin", "Log Cabin", BuildingType.Category.BUILDING,
            2, 1, new Color(0.6f, 0.42f, 0.28f, 1f), "forest"));

        register(mapType("palm", "Palm Tree", BuildingType.Category.ENVIRONMENT,
            new Color(0.35f, 0.7f, 0.35f, 1f), "beach"));
        register(mapType("beach_umbrella", "Beach Umbrella", BuildingType.Category.ENVIRONMENT,
            new Color(0.95f, 0.35f, 0.35f, 1f), "beach"));
        register(mapType("lifeguard_hut", "Lifeguard Hut", BuildingType.Category.BUILDING,
            new Color(0.9f, 0.5f, 0.3f, 1f), "beach"));

        register(mapType("summit_rock", "Summit Rock", BuildingType.Category.ENVIRONMENT,
            new Color(0.55f, 0.58f, 0.62f, 1f), "mountain"));
        register(mapType("alpine_hut", "Alpine Hut", BuildingType.Category.BUILDING,
            2, 1, new Color(0.55f, 0.48f, 0.4f, 1f), "mountain"));
        register(mapType("mountain_lookout", "Mountain Lookout", BuildingType.Category.INFRASTRUCTURE,
            2, 2, new Color(0.62f, 0.55f, 0.42f, 1f), "mountain"));

        register(mapType("snowman", "Snowman", BuildingType.Category.ENVIRONMENT,
            new Color(0.92f, 0.94f, 0.97f, 1f), "snowland"));
        register(mapType("igloo", "Igloo", BuildingType.Category.BUILDING,
            new Color(0.88f, 0.92f, 0.98f, 1f), "snowland"));
        register(mapType("ski_lodge", "Ski Lodge", BuildingType.Category.BUILDING,
            2, 2, new Color(0.5f, 0.65f, 0.8f, 1f), "snowland"));

        register(mapType("lava_rock", "Lava Rock", BuildingType.Category.ENVIRONMENT,
            new Color(0.35f, 0.28f, 0.25f, 1f), "volcano"));
        register(mapType("magma_spring", "Magma Spring", BuildingType.Category.BUILDING,
            2, 2, new Color(0.9f, 0.4f, 0.15f, 1f), "volcano"));
        register(mapType("obsidian_tower", "Obsidian Tower", BuildingType.Category.BUILDING,
            2, 1, new Color(0.3f, 0.25f, 0.42f, 1f), "volcano"));

        register(mapType("coconut_palm", "Coconut Palm", BuildingType.Category.ENVIRONMENT,
            new Color(0.4f, 0.72f, 0.35f, 1f), "island"));
        register(mapType("tiki_hut", "Tiki Hut", BuildingType.Category.BUILDING,
            new Color(0.85f, 0.68f, 0.4f, 1f), "island"));
        register(mapType("coral_spring", "Coral Spring", BuildingType.Category.ENVIRONMENT,
            new Color(0.35f, 0.75f, 0.8f, 1f), "island"));
    }

    private BuildingCatalog() {
    }

    private static void register(BuildingType type) {
        BY_ID.put(type.getId(), type);
        ALL.add(type);
    }

    /** 1x1 map-specific type with no star cost (its map is its gate). */
    private static BuildingType mapType(String id, String displayName, BuildingType.Category category,
                                        Color color, String mapId) {
        return mapType(id, displayName, category, 1, 1, color, mapId);
    }

    /** Map-specific type with an explicit footprint and no star cost. */
    private static BuildingType mapType(String id, String displayName, BuildingType.Category category,
                                        int widthCells, int heightCells, Color color, String mapId) {
        Array<String> maps = new Array<>();
        maps.add(mapId);
        return new BuildingType(id, displayName, category, widthCells, heightCells, color, 0, false, maps);
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
