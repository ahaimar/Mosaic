package com.packs.mosaic.world.econ;

/**
 * One technology tier (Task 15 — Technology and Productivity). A settlement
 * progresses through the five tiers Manual → Semi-Automated → Automated →
 * Robotic → Advanced Technology by funding research; each tier carries the
 * settlement-wide productivity improvements listed in the spec:
 *
 * <ul>
 *   <li><b>Production speed</b> — faster batch progress.</li>
 *   <li><b>Resource efficiency</b> — fewer inputs consumed per batch.</li>
 *   <li><b>Energy efficiency</b> — less grid energy drawn per consumer.</li>
 *   <li><b>Storage efficiency</b> — more capacity per warehouse/stockpile.</li>
 *   <li><b>Worker productivity</b> — more output per staffed worker.</li>
 *   <li><b>Product quality</b> — higher sale prices for what is sold.</li>
 *   <li><b>Transportation efficiency</b> — more goods per delivery trip.</li>
 * </ul>
 *
 * The bonuses are <em>cumulative</em>: a tier's values are the total effect
 * of every tier up to and including it, so a tier's factors apply directly
 * without summing anything. Manual (level 1) is the neutral baseline where
 * every factor is 1.
 */
public final class Technology {

    private final String id;
    private final int level;
    private final float researchCost;
    private final float researchTicks;
    private final float speedBonus;
    private final float resourceBonus;
    private final float energyBonus;
    private final float storageBonus;
    private final float productivityBonus;
    private final float qualityBonus;
    private final float transportBonus;

    public Technology(String id, int level, float researchCost, float researchTicks,
                      float speedBonus, float resourceBonus, float energyBonus,
                      float storageBonus, float productivityBonus, float qualityBonus,
                      float transportBonus) {
        this.id = id;
        this.level = level;
        this.researchCost = Math.max(0f, researchCost);
        this.researchTicks = Math.max(0f, researchTicks);
        this.speedBonus = Math.max(0f, speedBonus);
        this.resourceBonus = Math.max(0f, resourceBonus);
        this.energyBonus = Math.max(0f, energyBonus);
        this.storageBonus = Math.max(0f, storageBonus);
        this.productivityBonus = Math.max(0f, productivityBonus);
        this.qualityBonus = Math.max(0f, qualityBonus);
        this.transportBonus = Math.max(0f, transportBonus);
    }

    public String getId() { return id; }

    /** i18n key for this tier's name, e.g. {@code tech.semi}. */
    public String getNameKey() { return "tech." + id; }

    /** 1 = Manual … 5 = Advanced Technology. */
    public int getLevel() { return level; }

    /** Coins that must be invested to unlock this tier (0 for Manual). */
    public float getResearchCost() { return researchCost; }

    /** Ticks a fully-funded research project needs to finish. */
    public float getResearchTicks() { return researchTicks; }

    /** Fractional production-speed gain, cumulative (0.25 = +25%). */
    public float getSpeedBonus() { return speedBonus; }

    /** Fractional reduction of input consumption, cumulative (0.05 = −5%). */
    public float getResourceBonus() { return resourceBonus; }

    /** Fractional reduction of grid energy draw, cumulative (0.05 = −5%). */
    public float getEnergyBonus() { return energyBonus; }

    /** Fractional gain of warehouse/stockpile capacity, cumulative. */
    public float getStorageBonus() { return storageBonus; }

    /** Fractional gain of throughput per staffed worker, cumulative. */
    public float getProductivityBonus() { return productivityBonus; }

    /** Fractional gain of sale prices, cumulative (0.05 = +5%). */
    public float getQualityBonus() { return qualityBonus; }

    /** Fractional gain of per-trip delivery capacity, cumulative. */
    public float getTransportBonus() { return transportBonus; }

    /** Production-speed factor (1 at Manual, 2 at Advanced Technology). */
    public float speedFactor() { return 1f + speedBonus; }

    /** Worker-productivity factor (1 at Manual, 1.5 at Advanced). */
    public float productivityFactor() { return 1f + productivityBonus; }

    /** Combined throughput factor: speed × productivity. */
    public float throughputFactor() { return speedFactor() * productivityFactor(); }

    /** Input-consumption factor (1 at Manual, 0.8 at Advanced). */
    public float resourceFactor() { return 1f - resourceBonus; }

    /** Grid-energy factor (1 at Manual, 0.8 at Advanced). */
    public float energyFactor() { return 1f - energyBonus; }

    /** Storage-capacity factor (1 at Manual, 1.4 at Advanced). */
    public float storageFactor() { return 1f + storageBonus; }

    /** Sale-price factor (1 at Manual, 1.2 at Advanced). */
    public float qualityFactor() { return 1f + qualityBonus; }

    /** Per-trip delivery factor (1 at Manual, 1.4 at Advanced). */
    public float transportFactor() { return 1f + transportBonus; }
}
