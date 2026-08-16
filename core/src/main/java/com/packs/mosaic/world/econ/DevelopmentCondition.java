package com.packs.mosaic.world.econ;

/**
 * Task 18 — the eight measurable conditions a settlement must satisfy to
 * reach the next development level. Every condition is read from a real
 * economy metric, never from the treasury alone, so advancement follows the
 * settlement's actual growth: population, cumulative production, employment
 * rate, active infrastructure, cumulative revenue, technology tier, housing
 * capacity and cumulative market activity.
 */
public enum DevelopmentCondition {

    /** How many citizens live in the settlement. */
    POPULATION,
    /** Cumulative units produced since the simulation started. */
    PRODUCTION,
    /** Share of the working-age population that is employed, 0..1. */
    EMPLOYMENT,
    /** How many buildings are fully built and operating. */
    INFRASTRUCTURE,
    /** Cumulative coin revenue since the simulation started. */
    REVENUE,
    /** The unlocked technology tier (1 = Manual … 5 = Advanced). */
    TECHNOLOGY,
    /** Total housing capacity of the active houses. */
    HOUSING,
    /** Cumulative units sold to the market since the simulation started. */
    MARKET_ACTIVITY;

    /** Catalog id, e.g. {@code population}, also the i18n key suffix. */
    public String getId() {
        return name().toLowerCase();
    }

    /** i18n key for the condition's display name, e.g. {@code dev.condition.population}. */
    public String getNameKey() {
        return "dev.condition." + getId();
    }
}
