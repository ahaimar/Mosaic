package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Fixed registry of every special-building unlock: 20+ count-based
 * DISCOVERIES (e.g. "plant 20 flowers" → Flower Palace) and combination
 * RECIPES (e.g. "house + garden + fence" → Cozy Cottage). Built once and
 * looked up by id, mirroring BuildingCatalog/ChallengeCatalog. Add a new
 * unlock by adding one registration line here.
 *
 * Recipe ingredients (garden, fence, tower, crystal, magic_gate) are
 * themselves discovery rewards, so unlocks form a progression chain: grow
 * your village, discover ingredients, then combine them into special
 * structures.
 */
public final class UnlockCatalog {

    private static final ObjectMap<String, UnlockDefinition> BY_ID = new ObjectMap<>();
    private static final Array<UnlockDefinition> ALL = new Array<>();
    private static final Array<UnlockDefinition> DISCOVERIES = new Array<>();
    private static final Array<UnlockDefinition> RECIPES = new Array<>();

    static {
        // ---------- Discoveries: count-based goals ----------

        registerDiscovery(new UnlockDefinition("discovery_flower_palace", UnlockDefinition.Kind.DISCOVERY,
            "flower_palace", reqs(count("flower", 20))));
        registerDiscovery(new UnlockDefinition("discovery_grand_town_hall", UnlockDefinition.Kind.DISCOVERY,
            "grand_town_hall", reqs(sum("req.houses", 10, "small_house", "large_house"))));
        registerDiscovery(new UnlockDefinition("discovery_royal_garden", UnlockDefinition.Kind.DISCOVERY,
            "royal_garden", reqs(count("tree", 15))));
        registerDiscovery(new UnlockDefinition("discovery_crystal_quarry", UnlockDefinition.Kind.DISCOVERY,
            "crystal_quarry", reqs(count("rock", 10))));
        registerDiscovery(new UnlockDefinition("discovery_lamp_plaza", UnlockDefinition.Kind.DISCOVERY,
            "lamp_plaza", reqs(count("street_lamp", 12))));
        registerDiscovery(new UnlockDefinition("discovery_bench_park", UnlockDefinition.Kind.DISCOVERY,
            "bench_park", reqs(count("bench", 8))));
        registerDiscovery(new UnlockDefinition("discovery_bush_maze", UnlockDefinition.Kind.DISCOVERY,
            "bush_maze", reqs(count("bush", 12))));
        registerDiscovery(new UnlockDefinition("discovery_highway", UnlockDefinition.Kind.DISCOVERY,
            "highway", reqs(sum("req.roads", 15, "road_straight", "road_corner", "road_cross"))));
        registerDiscovery(new UnlockDefinition("discovery_university", UnlockDefinition.Kind.DISCOVERY,
            "university", reqs(count("school", 3))));
        registerDiscovery(new UnlockDefinition("discovery_grand_market", UnlockDefinition.Kind.DISCOVERY,
            "grand_market", reqs(count("shop", 3))));
        registerDiscovery(new UnlockDefinition("discovery_amusement_park", UnlockDefinition.Kind.DISCOVERY,
            "amusement_park", reqs(count("playground", 3))));
        registerDiscovery(new UnlockDefinition("discovery_grand_bridge", UnlockDefinition.Kind.DISCOVERY,
            "grand_bridge", reqs(count("bridge_small", 3))));
        registerDiscovery(new UnlockDefinition("discovery_nature_reserve", UnlockDefinition.Kind.DISCOVERY,
            "nature_reserve", reqs(sum("req.environment", 30,
                "tree", "bush", "flower", "rock", "bench", "street_lamp", "garden", "crystal"))));
        registerDiscovery(new UnlockDefinition("discovery_central_station", UnlockDefinition.Kind.DISCOVERY,
            "central_station", reqs(sum("req.infrastructure", 20,
                "road_straight", "road_corner", "road_cross", "bridge_small", "fence", "magic_gate"))));
        registerDiscovery(new UnlockDefinition("discovery_megalopolis_tower", UnlockDefinition.Kind.DISCOVERY,
            "megalopolis_tower", reqs(totalBuildings(50))));

        // Discoveries that unlock recipe ingredients.
        registerDiscovery(new UnlockDefinition("discovery_garden", UnlockDefinition.Kind.DISCOVERY,
            "garden", reqs(count("flower", 10), count("bush", 6))));
        registerDiscovery(new UnlockDefinition("discovery_fence", UnlockDefinition.Kind.DISCOVERY,
            "fence", reqs(count("bench", 8))));
        registerDiscovery(new UnlockDefinition("discovery_tower", UnlockDefinition.Kind.DISCOVERY,
            "tower", reqs(totalBuildings(12))));
        registerDiscovery(new UnlockDefinition("discovery_crystal", UnlockDefinition.Kind.DISCOVERY,
            "crystal", reqs(count("rock", 6))));
        registerDiscovery(new UnlockDefinition("discovery_magic_gate", UnlockDefinition.Kind.DISCOVERY,
            "magic_gate", reqs(count("street_lamp", 5))));

        // ---------- Recipes: combinations present at once ----------

        registerRecipe(new UnlockDefinition("recipe_cozy_cottage", UnlockDefinition.Kind.RECIPE,
            "cozy_cottage", reqs(
                anyOf("req.house", "small_house", "large_house"),
                presence("garden"),
                presence("fence"))));
        registerRecipe(new UnlockDefinition("recipe_small_town", UnlockDefinition.Kind.RECIPE,
            "small_town", reqs(
                presence("shop"),
                anyOf("req.road", "road_straight", "road_corner", "road_cross"),
                anyOf("req.house", "small_house", "large_house"))));
        registerRecipe(new UnlockDefinition("recipe_wizard_tower", UnlockDefinition.Kind.RECIPE,
            "wizard_tower", reqs(
                presence("tower"),
                presence("crystal"),
                presence("magic_gate"))));
        registerRecipe(new UnlockDefinition("recipe_park_square", UnlockDefinition.Kind.RECIPE,
            "park_square", reqs(
                presence("playground"),
                presence("tree"),
                presence("bench"))));
        registerRecipe(new UnlockDefinition("recipe_market_street", UnlockDefinition.Kind.RECIPE,
            "market_street", reqs(
                presence("shop"),
                anyOf("req.road", "road_straight", "road_corner", "road_cross"),
                presence("street_lamp"))));
        registerRecipe(new UnlockDefinition("recipe_village_center", UnlockDefinition.Kind.RECIPE,
            "village_center", reqs(
                presence("school"),
                anyOf("req.road", "road_straight", "road_corner", "road_cross"),
                anyOf("req.house", "small_house", "large_house"))));
    }

    private UnlockCatalog() {
    }

    private static void registerDiscovery(UnlockDefinition unlock) {
        register(unlock);
        DISCOVERIES.add(unlock);
    }

    private static void registerRecipe(UnlockDefinition unlock) {
        register(unlock);
        RECIPES.add(unlock);
    }

    private static void register(UnlockDefinition unlock) {
        BY_ID.put(unlock.getId(), unlock);
        ALL.add(unlock);
    }

    public static UnlockDefinition get(String id) {
        return BY_ID.get(id);
    }

    public static Array<UnlockDefinition> getAll() {
        return ALL;
    }

    public static Array<UnlockDefinition> getDiscoveries() {
        return DISCOVERIES;
    }

    public static Array<UnlockDefinition> getRecipes() {
        return RECIPES;
    }

    // ---------- small requirement factories to keep registrations compact ----------

    private static Array<UnlockRequirement> reqs(UnlockRequirement... requirements) {
        return new Array<>(requirements);
    }

    private static CountRequirement count(String typeId, int needed) {
        return new CountRequirement(typeId, needed);
    }

    private static SumCountRequirement sum(String labelKey, int needed, String... typeIds) {
        return new SumCountRequirement(new Array<>(typeIds), needed, labelKey);
    }

    private static PresenceRequirement presence(String typeId) {
        return new PresenceRequirement(new Array<>(new String[]{typeId}), "building." + typeId);
    }

    private static AnyPresenceRequirement anyOf(String labelKey, String... typeIds) {
        return new AnyPresenceRequirement(new Array<>(typeIds), labelKey);
    }

    private static SumCountRequirement totalBuildings(int needed) {
        Array<String> ids = new Array<>();
        for (BuildingType type : BuildingCatalog.getAll()) {
            ids.add(type.getId());
        }
        return new SumCountRequirement(ids, needed, "req.total");
    }
}
