package com.packs.mosaic.world.econ;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The technology tree (Task 15): the five tiers a settlement unlocks through
 * research. Bonuses are cumulative per tier, so {@link #get(int)} returns the
 * tier whose factors apply directly to the simulation.
 *
 * <p>Costs are meant to be a real, long-term commitment: reaching Advanced
 * Technology funds four research projects (total 4,400 coins over 75 ticks)
 * in exchange for a 3× production throughput, 20% fewer inputs, 20% less grid
 * draw, +40% storage, +50% worker productivity, +20% sale prices and +40%
 * delivery capacity.
 */
public final class TechnologyCatalog {

    private static final Map<String, Technology> BY_ID = new LinkedHashMap<>();
    private static final List<Technology> ALL = new ArrayList<>();

    static {
        register(new Technology("manual", 1, 0f, 0f,
            0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f));
        register(new Technology("semi", 2, 200f, 10f,
            0.25f, 0.05f, 0.05f, 0.10f, 0.10f, 0.05f, 0.10f));
        register(new Technology("automated", 3, 500f, 15f,
            0.50f, 0.10f, 0.10f, 0.20f, 0.20f, 0.10f, 0.20f));
        register(new Technology("robotic", 4, 1200f, 20f,
            0.75f, 0.15f, 0.15f, 0.30f, 0.35f, 0.15f, 0.30f));
        register(new Technology("advanced", 5, 2500f, 30f,
            1.00f, 0.20f, 0.20f, 0.40f, 0.50f, 0.20f, 0.40f));
    }

    private TechnologyCatalog() {
    }

    private static void register(Technology technology) {
        BY_ID.put(technology.getId(), technology);
        ALL.add(technology);
    }

    /** The tier with the given id, or Manual when unknown. */
    public static Technology get(String id) {
        Technology technology = BY_ID.get(id);
        return technology == null ? get(1) : technology;
    }

    /** The tier with the given level (1..5), clamped to the tree. */
    public static Technology get(int level) {
        if (level <= 1) return ALL.get(0);
        for (Technology technology : ALL) {
            if (technology.getLevel() == level) return technology;
        }
        return ALL.get(ALL.size() - 1);
    }

    /** The highest tier in the tree (5 = Advanced Technology). */
    public static int maxLevel() {
        return ALL.get(ALL.size() - 1).getLevel();
    }

    /** Every tier, Manual first. */
    public static List<Technology> all() {
        return ALL;
    }
}
