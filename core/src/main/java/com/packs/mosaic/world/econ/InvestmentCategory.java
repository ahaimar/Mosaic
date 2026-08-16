package com.packs.mosaic.world.econ;

/**
 * The five investment tracks (Task 16 — Investment Decisions). Each category
 * grants a different permanent, long-term effect once the investment is fully
 * paid:
 *
 * <ul>
 *   <li>{@link #PRODUCTION} — more output per completed batch.</li>
 *   <li>{@link #INFRASTRUCTURE} — more shared storage capacity.</li>
 *   <li>{@link #TECHNOLOGY} — cheaper future research projects.</li>
 *   <li>{@link #WORKFORCE} — a larger share of the population is of working age.</li>
 *   <li>{@link #MARKETS} — higher sale revenue for everything sold.</li>
 * </ul>
 */
public enum InvestmentCategory {
    PRODUCTION,
    INFRASTRUCTURE,
    TECHNOLOGY,
    WORKFORCE,
    MARKETS
}
