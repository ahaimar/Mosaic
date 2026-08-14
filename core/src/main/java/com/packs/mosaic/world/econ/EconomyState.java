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
    public Array<StockState> inventory = new Array<>();
    public Array<ConstructionState> construction = new Array<>();

    public EconomyState() {
    }
}
