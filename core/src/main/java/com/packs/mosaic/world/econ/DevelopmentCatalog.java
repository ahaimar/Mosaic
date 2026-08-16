package com.packs.mosaic.world.econ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task 18 — the six development stages, from Small Settlement to Major
 * Economic Center. Thresholds are order-of-magnitude steps in the eight
 * measurable conditions: population, cumulative production, employment rate,
 * active buildings, cumulative revenue, technology tier, housing capacity and
 * cumulative market activity. Money alone never unlocks a stage — every
 * threshold is a real activity metric.
 */
public final class DevelopmentCatalog {

    private static final Map<String, DevelopmentLevel> BY_ID = new LinkedHashMap<>();
    private static final List<DevelopmentLevel> ALL = new ArrayList<>();

    static {
        // Threshold order follows DevelopmentCondition: population, production,
        // employment, infrastructure, revenue, technology, housing, market.
        register(new DevelopmentLevel(1, "small_settlement",
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f));
        register(new DevelopmentLevel(2, "village",
            20f, 100f, 0.80f, 5f, 300f, 1f, 3f, 50f));
        register(new DevelopmentLevel(3, "town",
            50f, 400f, 0.85f, 12f, 1500f, 2f, 8f, 200f));
        register(new DevelopmentLevel(4, "industrial_town",
            100f, 1000f, 0.85f, 20f, 4000f, 3f, 15f, 600f));
        register(new DevelopmentLevel(5, "city",
            200f, 2500f, 0.90f, 32f, 10000f, 4f, 30f, 1500f));
        register(new DevelopmentLevel(6, "major_economic_center",
            350f, 6000f, 0.90f, 45f, 25000f, 5f, 50f, 4000f));
    }

    private DevelopmentCatalog() {
    }

    private static void register(DevelopmentLevel level) {
        BY_ID.put(level.getId(), level);
        ALL.add(level);
    }

    /** The level at the given stage, clamped to the valid range 1..6. */
    public static DevelopmentLevel get(int level) {
        return ALL.get(Math.max(0, Math.min(maxLevel() - 1, level - 1)));
    }

    /** The level with the given id, or null when unknown. */
    public static DevelopmentLevel get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** Every level, in ascending order. */
    public static List<DevelopmentLevel> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static int maxLevel() {
        return ALL.size();
    }
}
