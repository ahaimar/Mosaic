package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Fixed registry of the starter challenges (spec's Challenge 1/2/3
 * examples), looked up by id and traversed in order by ChallengeManager.
 * The first challenges only require free-at-0★ types so a fresh save can
 * always finish them; later ones need types unlocked at higher star
 * thresholds (bush/street_lamp at 10★, etc.).
 */
public final class ChallengeCatalog {

    private static final ObjectMap<String, ChallengeDefinition> BY_ID = new ObjectMap<>();
    private static final Array<ChallengeDefinition> ALL = new Array<>();

    static {
        register(new ChallengeDefinition("challenge_1", "First Steps",
            "A cozy start: build a home and plant a little nature.",
            5, requires("small_house", 1, "tree", 2)));

        register(new ChallengeDefinition("challenge_2", "Growing Village",
            "Bigger homes and roads to connect them.",
            10, requires("large_house", 1, "tree", 1, "road_straight", 2)));

        register(new ChallengeDefinition("challenge_3", "Neighbourhood Square",
            "Settle more homes and light the way with lamps.",
            15, requires("small_house", 2, "street_lamp", 2, "flower", 2)));
    }

    private ChallengeCatalog() {
    }

    private static void register(ChallengeDefinition definition) {
        BY_ID.put(definition.getId(), definition);
        ALL.add(definition);
    }

    private static ObjectMap<String, Integer> requires(Object... typeCountPairs) {
        ObjectMap<String, Integer> requirements = new ObjectMap<>();
        for (int i = 0; i < typeCountPairs.length; i += 2) {
            requirements.put((String) typeCountPairs[i], (Integer) typeCountPairs[i + 1]);
        }
        return requirements;
    }

    public static ChallengeDefinition get(String id) {
        return BY_ID.get(id);
    }

    public static Array<ChallengeDefinition> getAll() {
        return ALL;
    }

    public static ChallengeDefinition getFirst() {
        return ALL.isEmpty() ? null : ALL.first();
    }

    /** The challenge following {@code current} in registration order, or null if it was the last. */
    public static ChallengeDefinition getNext(ChallengeDefinition current) {
        if (current == null) return null;
        int index = ALL.indexOf(current, true);
        return index >= 0 && index < ALL.size - 1 ? ALL.get(index + 1) : null;
    }
}
