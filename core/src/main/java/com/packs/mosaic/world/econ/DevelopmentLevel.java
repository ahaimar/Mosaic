package com.packs.mosaic.world.econ;

/**
 * Task 18 — one development stage (Small Settlement … Major Economic Center).
 * Each level lists the threshold every one of the eight conditions must reach
 * for the settlement to advance. Levels are ordered and their requirements are
 * monotonic, so a settlement grows through the stages and can never lose one.
 */
public final class DevelopmentLevel {

    private final int level;
    private final String id;
    private final float[] required;

    /**
     * @param level    1-based stage number
     * @param id       unique catalog id, also the base of the i18n key
     * @param required one threshold per {@link DevelopmentCondition}, in enum
     *                 order; missing entries default to 0 (never a blocker)
     */
    public DevelopmentLevel(int level, String id, float... required) {
        this.level = level;
        this.id = id;
        this.required = new float[DevelopmentCondition.values().length];
        System.arraycopy(required, 0, this.required, 0,
            Math.min(required.length, this.required.length));
    }

    public int getLevel() { return level; }

    public String getId() { return id; }

    /** i18n key for the stage's name, e.g. {@code development.village}. */
    public String getNameKey() { return "development." + id; }

    /** The threshold of one condition, or 0 when the level imposes none. */
    public float required(DevelopmentCondition condition) {
        return required[condition.ordinal()];
    }

    /** True when the simulation meets every threshold of this level. */
    public boolean isFullyMet(EconomySimulation simulation) {
        for (DevelopmentCondition condition : DevelopmentCondition.values()) {
            if (simulation.currentValue(condition) < required(condition)) return false;
        }
        return true;
    }
}
