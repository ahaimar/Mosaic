package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Fixed registry of the game's worlds (Task 3). The original grass village
 * ("meadow") is preserved untouched as the default map; every other entry is
 * a new environment with its own theme, decorations and effect.
 */
public final class MapCatalog {

    public static final String MEADOW_ID = "meadow";

    private static final ObjectMap<String, GameMap> BY_ID = new ObjectMap<>();
    private static final Array<GameMap> ALL = new Array<>();

    static {
        register(new GameMap(MEADOW_ID, "map.meadow.name", "map.meadow.desc",
            GameMap.Terrain.GRASS, GameMap.Effect.NONE,
            new Color(0.45f, 0.72f, 0.38f, 1f), new Color(0.62f, 0.82f, 0.42f, 1f),
            new Color(0.55f, 0.78f, 0.45f, 1f), new Color(1f, 1f, 1f, 0.28f)));

        register(new GameMap("forest", "map.forest.name", "map.forest.desc",
            GameMap.Terrain.FOREST, GameMap.Effect.LEAVES,
            new Color(0.22f, 0.42f, 0.22f, 1f), new Color(0.35f, 0.60f, 0.28f, 1f),
            new Color(0.25f, 0.45f, 0.30f, 1f), new Color(1f, 1f, 1f, 0.22f)));

        register(new GameMap("beach", "map.beach.name", "map.beach.desc",
            GameMap.Terrain.SAND, GameMap.Effect.GLINTS,
            new Color(0.85f, 0.76f, 0.52f, 1f), new Color(0.95f, 0.88f, 0.65f, 1f),
            new Color(0.55f, 0.80f, 0.95f, 1f), new Color(0.45f, 0.40f, 0.30f, 0.30f)));

        register(new GameMap("mountain", "map.mountain.name", "map.mountain.desc",
            GameMap.Terrain.ROCK, GameMap.Effect.MIST,
            new Color(0.50f, 0.52f, 0.56f, 1f), new Color(0.64f, 0.66f, 0.70f, 1f),
            new Color(0.50f, 0.55f, 0.62f, 1f), new Color(1f, 1f, 1f, 0.20f)));

        register(new GameMap("snowland", "map.snowland.name", "map.snowland.desc",
            GameMap.Terrain.SNOW, GameMap.Effect.SNOW,
            new Color(0.88f, 0.92f, 0.98f, 1f), new Color(0.72f, 0.82f, 0.95f, 1f),
            new Color(0.55f, 0.65f, 0.85f, 1f), new Color(0.30f, 0.40f, 0.60f, 0.35f)));

        register(new GameMap("volcano", "map.volcano.name", "map.volcano.desc",
            GameMap.Terrain.VOLCANIC, GameMap.Effect.EMBERS,
            new Color(0.30f, 0.24f, 0.22f, 1f), new Color(0.55f, 0.30f, 0.20f, 1f),
            new Color(0.22f, 0.16f, 0.18f, 1f), new Color(0.9f, 0.5f, 0.3f, 0.35f)));

        register(new GameMap("island", "map.island.name", "map.island.desc",
            GameMap.Terrain.TROPICAL, GameMap.Effect.RAIN,
            new Color(0.62f, 0.72f, 0.48f, 1f), new Color(0.35f, 0.68f, 0.80f, 1f),
            new Color(0.40f, 0.70f, 0.85f, 1f), new Color(1f, 1f, 1f, 0.22f)));
    }

    private MapCatalog() {
    }

    private static void register(GameMap map) {
        BY_ID.put(map.getId(), map);
        ALL.add(map);
    }

    public static GameMap get(String id) {
        return BY_ID.get(id);
    }

    /** The original grass village, unchanged from before Task 3. */
    public static GameMap getMeadow() {
        return get(MEADOW_ID);
    }

    public static Array<GameMap> getAll() {
        return ALL;
    }
}
