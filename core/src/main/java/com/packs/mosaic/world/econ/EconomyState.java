package com.packs.mosaic.world.econ;

import com.badlogic.gdx.utils.Array;

/**
 * Plain serializable snapshot of the economic simulation (Task 5/6), stored in
 * SaveData and persisted as JSON by SaveManager. Only *state* is stored —
 * money, population, inventory and in-progress construction — never derived
 * figures like prices, demand or profit, which the simulation always
 * recomputes from the building layout.
 *
 * Since the Task 6 resource system, money lives in the inventory as the COINS
 * stock. The {@code money} field is kept for legacy saves written before that
 * change: when restoring, an explicit COINS stock wins over {@code money}.
 *
 * <p>Task 9 (workforce and employment) adds the {@code averageWage} — the one
 * mutable workforce figure that has history; everything else (working
 * population, employed/unemployed, required workers) is recomputed each tick
 * from the population and the building layout.
 *
 * <p>Task 15 (technology and productivity) adds the {@code techLevel} — the
 * settlement's unlocked tier (1 = Manual … 5 = Advanced Technology) — plus the
 * in-progress research project (coins still owed and ticks still needed). The
 * bonuses themselves are derived from the tier in the technology catalog, so
 * only the level and the outstanding research are persisted.
 *
 * <p>Task 16 (investment decisions) adds the active investment project (id +
 * coins still owed) and the set of completed investment ids. The permanent
 * bonuses an investment grants are recomputed from the completed ids on
 * restore, so only the ids and the outstanding cost are persisted.
 *
 * <p>Task 18 (development levels) adds the settlement's development stage and
 * the three lifetime metrics that drive it (cumulative production, market
 * sales and revenue). These must be persisted because the development
 * conditions compare against them — a reloaded settlement keeps its progress
 * toward the next stage.
 */
public class EconomyState {

    /** One inventory line: a good id plus its current quantity. */
    public static class StockState {
        public String goodId;
        public float amount;

        public StockState() {
        }
    }

    /** One in-progress construction site. */
    public static class ConstructionState {
        public String typeId;
        public float remainingCost;
        public float remainingTicks;

        public ConstructionState() {
        }
    }

    public float money;
    public float population;
    /** Average wage one worker earns per tick (Task 9). */
    public float averageWage;
    /** Unlocked technology tier, 1 = Manual … 5 = Advanced (Task 15). */
    public int techLevel;
    /** Coins still owed on the in-progress research project (Task 15). */
    public float researchRemainingCost;
    /** Ticks still needed to complete the research project (Task 15). */
    public float researchRemainingTicks;
    /** Id of the active investment project, or null (Task 16). */
    public String activeInvestmentId;
    /** Coins still owed on the active investment project (Task 16). */
    public float investmentRemainingCost;
    /** Ids of every completed investment (Task 16). */
    public Array<String> completedInvestments = new Array<>();
    /** Development stage, 1 = Small Settlement … 6 = Major Economic Center (Task 18). */
    public int developmentLevel;
    /** Cumulative units produced since the save (Task 18 condition). */
    public float lifetimeProduced;
    /** Cumulative units sold to the market since the save (Task 18 condition). */
    public float lifetimeSold;
    /** Cumulative coin revenue since the save (Task 18 condition). */
    public float lifetimeRevenue;
    public Array<StockState> inventory = new Array<>();
    public Array<ConstructionState> construction = new Array<>();

    public EconomyState() {
    }
}
