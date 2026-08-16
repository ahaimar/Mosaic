package com.packs.mosaic.world.econ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The central economic simulation (Task 5, extended by the Task 6 resource
 * system, Task 7 production chains and Task 8 inventory/warehousing). It ties
 * the game's building systems together into one deterministic simulation tick
 * that runs once per second. Every figure is either computed from the data
 * table ({@link EconomyData}) or derived from it — economic results are never
 * hardcoded into individual buildings.
 *
 * <p>Money is a resource like any other ({@link Resource#COINS}): the
 * treasury is the COINS quantity of the settlement's inventory, so revenue,
 * operating costs and construction investment are ordinary resource flows.
 * The COINS resource itself is never produced by a building and never
 * exported — it is minted only by the starting grant and by selling surplus.
 *
 * <p>Task 8 — Inventory and Warehousing: the settlement holds a shared
 * {@link ResourceInventory} with a finite capacity (base storage plus every
 * active warehouse's inventory capacity). Each factory owns a small on-site
 * stockpile for its output. When a production batch starts it <em>reserves</em>
 * its full inputs plus energy from the settlement (reserved stock is visible
 * and cannot be claimed by another factory); when the batch completes the
 * reservation is fulfilled and the inputs are really consumed. Finished goods
 * accumulate in the factory stockpile and are shipped into the settlement
 * while shared storage has room. When a factory's stockpile is full,
 * production stops until space frees; warehouses additionally charge a
 * storage fee per stored unit per tick.
 *
 * <p>Task 9 — Workforce and Employment: only part of the population is of
 * working age, so the <em>working population</em> (population × participation
 * rate) is the pool every active building hires from. Buildings are staffed
 * greedily in sorted type order up to their workforce requirement; the pool
 * that is not hired is the <em>unemployed</em> figure. Because factories need
 * staff to run, a growing economy has to grow its (fed and housed)
 * population — housing and infrastructure matter. Employed workers are paid
 * an <em>average wage</em> that drifts toward a target driven by labor
 * scarcity (the tighter the labor market, the higher the wage); wages are a
 * real operating cost paid from the treasury.
 *
 * <p>Task 10 — Building operating costs: every productive building carries
 * recurring costs, and the tick's costs are exposed as an inspectable
 * {@link CostLedger} with the buckets <em>wages</em>, <em>maintenance</em>
 * (each active building's operating cost), <em>transport</em> (warehousing
 * fees), <em>energy</em> (coal burnt by production) and <em>materials</em>
 * (inputs consumed by production). Wages, maintenance and transport are paid
 * in coins; energy and materials are the in-kind resources the factories used
 * up, valued at the tick's market prices so the player sees their economic
 * weight. The ledger reports the total cost and the net income
 *  (revenue − total).
 *
 *  <p>Task 11 — Supply and demand: the market now models both sides. On the
 *  supply side, what reaches the market is production capacity (batch slots ×
 *  capacity), limited by the available resources, the staffed workforce and
 *  factory efficiency, and moved by transportation (ship-out into shared
 *  storage). On the demand side, the factories' input needs are joined by the
 *  population's own appetite: each citizen carries a per-good consumer
 *  preference, tempered by price elasticity (a high price curbs the appetite,
 *  a bargain inflates it) and scaled by local development (each active
 *  building makes the town want more). Prices respond gradually — each tick
 *  drifts only part of the way toward the balanced target — so the market
 *  never jumps, and consumer pressure is what keeps finished goods valuable
 *  instead of crashing to their price floor.
 *
 *  <p>Task 12 — Market and sales logic: goods are sold on a real market. For
 *  consumer goods (components, consumer goods and finished products) the
 *  market clears the smaller of the available supply and the consumer demand:
 *  the population buys what it wants at the going price, and whatever it does
 *  not want stays in inventory. Overproduction therefore piles inventory up
 *  and drags the price down, while a shortage drains inventory and pushes the
 *  price up. Everything else keeps the classic export rule — food feeds the
 *  population directly and the remaining goods are exported only when they
 *  overflow their storage cap. Each good's market state (price vs target,
 *  demand vs supply, sales volume and the price trend) is inspectable through
 *  {@link MarketInfo}, so the player can always see why prices and sales
 *  changed.
 *
 *  <p>Task 13 — Transportation and logistics: producers can be connected to
 *  the settlement by {@link DeliveryRoute}s. A route carries one good from a
 *  producer's <em>loading point</em> (its on-site stockpile) to the
 *  settlement, which doubles as the central warehouse/staging yard — so the
 *  chain "Mine → Truck → Warehouse → Factory" is a routed mine feeding the
 *  settlement, and the factory drawing its inputs from it. Trucks take
 *  {@code tripTicks} to travel (roads shorten the trip, a truck depot adds
 *  speed), each trip carries at most the assigned trucks' combined capacity,
 *  and every dispatched trip costs a hauling fee. Goods with a route are no
 *  longer shipped instantly; goods without a route keep the classic instant
 *  ship-out, so adding a route is an explicit choice between speed and real
 *  transport.
 *
 *  <p>Task 14 — Energy system: the settlement runs an energy network. Power
 *  plants (generator, power plant, solar plant, advanced power plant)
 *  generate energy; factories, warehouses, shops, infrastructure and research
 *  facilities draw it. Each tick the grid computes the spec's balance
 *  "energy production − energy consumption". When the grid is short,
 *  production efficiency scales down to the fraction of demand that is met —
 *  factories slow rather than stop, and buildings that need no energy are
 *  never affected.
 *
 *  <p>Task 15 — Technology and productivity: the settlement progresses
 *  through the five tiers Manual → Semi-Automated → Automated → Robotic →
 *  Advanced Technology by funding research projects (coins over time, like
 *  construction). Each unlocked tier's cumulative bonuses scale production
 *  speed, worker productivity, resource efficiency (fewer inputs per batch),
 *  energy efficiency (less grid draw), storage efficiency (more capacity),
 *  product quality (higher sale prices) and transportation efficiency (more
 *  goods per delivery trip). Research is capital investment: it is paid from
 *  the treasury each tick but is not an operating cost.
 *
 *  <p>Task 16 — Investment decisions: the settlement can also fund one
 *  investment project at a time (capacity expansion, storage expansion,
 *  research endowment, worker education, export agreement). Each is presented
 *  with its cost, expected benefit, payback period and risk, and once fully
 *  paid it grants a permanent long-term effect: +output per batch, +shared
 *  storage, −research cost, +working population share or +sale revenue.
 *  Investments compete with construction and research for the same treasury,
 *  each can be taken at most once, and a risky deal's expected benefit is
 *  discounted — so the player has to plan rather than simply buy everything.
 *
 *  <p>Task 18 — Development levels: the settlement advances through six
 *  stages (Small Settlement → Village → Town → Industrial Town → City →
 *  Major Economic Center) driven by eight measurable conditions — population,
 *  cumulative production, employment rate, active infrastructure, cumulative
 *  revenue, technology tier, housing capacity and cumulative market activity.
 *  Every stage's thresholds come from the development catalog and the level
 *  rises automatically when all of them are met, never from money alone.
 *
 * <p>Flow of a single tick:
 *
 * <pre>
 *   Construction → Workforce (hire + wages) → Energy (grid balance)
 *                  ↓
 *            Production (reserve + batches) → Routes (dispatch + arrivals)
 *                  ↓
 *       Ship-out → Inventory → Consumption → Market → Sales → Revenue
 *                  ↓
 *   Cost ledger (wages / maintenance / transport / hauling / energy / materials)
 *                  ↓
 *                 Profit → Investment
 *                  ↓
 *                 Population
 * </pre>
 *
 * Deterministic by design: there is no randomness anywhere. The same
 * building feed and the same starting state always produce the same
 * sequence of states, so the simulation is safe to unit-test and to
 * serialize between frames.
 */
public final class EconomySimulation {

    /** One simulation step per real-time second (accumulator-driven). */
    public static final float TICK_SECONDS = 1f;

    private static final float START_MONEY = 100f;
    private static final float START_POPULATION = 3f;
    private static final float FOOD_PER_PERSON = 0.25f;
    private static final float GROWTH_RATE = 0.10f;
    private static final float STARVATION_PENALTY = 0.25f;
    private static final float INVEST_SPEED = 4f;
    private static final float DEMAND_PRESSURE = 0.6f;
    private static final float PRICE_DRIFT = 0.35f;
    private static final float PRICE_FLOOR = 0.3f;
    private static final float PRICE_CEILING = 2.5f;
    private static final float MAX_MARKET_MULTIPLIER = 3f;

    // ── Task 15 technology & productivity constants ──────────────────────
    /**
     * Coins invested into an in-progress research project per tick. Research
     * is funded like construction — small payments over time — so unlocking a
     * tier requires both the treasury to stay solvent and the research to
     * finish ticking down.
     */
    private static final float RESEARCH_SPEED = 4f;

    // ── Task 16 investment constants ─────────────────────────────────────
    /**
     * Coins invested into an active investment project per tick. Like
     * construction and research, an investment is paid in small amounts from
     * the treasury over time, so it never completes while the treasury is
     * empty — the player must keep the settlement profitable to fund it.
     */
    private static final float INVESTMENT_SPEED = 6f;

    // ── Task 11 supply & demand constants ────────────────────────────────
    /**
     * How strongly a price away from its base curbs (or kindles) consumer
     * appetite: at a price 50% above base, demand falls by roughly half the
     * elasticity; at a low price the town buys noticeably more.
     */
    private static final float PRICE_ELASTICITY = 1.2f;
    /** Cap on how much cheap goods can inflate consumer demand. */
    private static final float MAX_ELASTICITY_FACTOR = 2f;
    /**
     * Extra consumer demand per active building: a developed settlement wants
     * more of everything, so prices hold up as the town grows.
     */
    private static final float DEVELOPMENT_PRESSURE = 0.05f;
    /** Cap on the development demand multiplier. */
    private static final float MAX_DEVELOPMENT_FACTOR = 3f;

    /** Shared storage every settlement starts with (Task 8). */
    private static final float BASE_STORAGE_CAPACITY = 200f;

    // ── Task 13 transport & logistics constants ─────────────────────────
    /** Trucks a settlement starts with before any truck depot is built. */
    private static final int BASE_TRUCKS = 3;
    /** Units one truck can carry per delivery trip. */
    private static final float BASE_TRUCK_CAPACITY = 5f;
    /** Distance units a truck covers per tick (base trip = 6 ticks). */
    private static final float BASE_TRUCK_SPEED = 1f;
    /** Coins one dispatched delivery trip costs (fleet, fuel, driver). */
    private static final float BASE_TRIP_COST = 0.1f;
    /** Distance from any loading point to the settlement, in time units. */
    private static final float ROUTE_DISTANCE = 6f;
    /** Delivery trips any route can start per tick before warehouses stage. */
    private static final int DISPATCH_BASE = 1;

    // ── Task 14 energy network constants ────────────────────────────────
    /**
     * The settlement's base grid power before any power plant is built (a
     * small backup generator). Like the base truck fleet, it keeps a small
     * settlement self-sufficient; once consumption outgrows it, power plants
     * are the only way to keep every factory at full speed.
     */
    private static final float BASE_ENERGY = 5f;

    // ── Task 9 workforce / employment constants ──────────────────────────
    /** Fraction of the population that is of working age. */
    private static final float WORKFORCE_PARTICIPATION = 0.5f;
    /** Coins one worker earns per tick when the labor market is idle. */
    private static final float WAGE_BASE = 0.05f;
    /** How fast the average wage drifts toward its target. */
    private static final float WAGE_DRIFT = 0.05f;
    /** Wage premium above the base when every worker is already employed. */
    private static final float LABOR_SCARCITY = 0.5f;

    private float population;
    private float workingPopulation;
    private float employedWorkers;
    private float averageWage = WAGE_BASE;
    private float wageCosts;
    private final ResourceInventory settlement;
    private final float[] price;
    /** Task 12: the balanced price the market drifts toward this tick. */
    private final float[] targetPrice;
    /** Per-tick produced/consumed/sold/demand (reset every tick). */
    private final float[] produced;
    private final float[] consumed;
    private final float[] sold;
    private final float[] demand;
    /** Task 11: the population's own appetite, part of the per-tick demand. */
    private final float[] consumerDemand;
    private float developmentFactor = 1f;
    private float revenue;
    private float operatingCosts;
    private float storageCosts;
    /** Task 10: per-tick cost ledger buckets. */
    private float maintenanceCosts;
    private float energyCosts;
    private float materialsCosts;
    private float profit;
    private float totalProfit;
    private float totalInvestment;
    private float investedThisTick;
    private float workforceDemand;
    private float workforceAssigned;
    private int tickCount;
    private float accumulator;

    /** Placed building counts by type id (including under-construction sites). */
    private final TreeMap<String, Integer> placedCounts = new TreeMap<>();
    private final List<ConstructionSite> sites = new ArrayList<>();

    /** Task 9: instances of each type actually staffed this tick. */
    private final TreeMap<String, Integer> staffedByType = new TreeMap<>();

    /**
     * Task 7/8 batch state per producing type. Batch progress accumulates in
     * production-time units; the number of in-flight {@code batchSlots} is the
     * number of instances that managed to reserve their inputs at batch start
     * (never more than the staffable workforce). Each slot holds its full input
     * reservation in {@code batchReservations} until the batch completes, then
     * the reservation is fulfilled and the output lands in the type's own
     * {@code buildingInventories} stockpile before being shipped to the
     * settlement.
     */
    private final TreeMap<String, Float> productionProgress = new TreeMap<>();
    private final TreeMap<String, Integer> batchSlots = new TreeMap<>();
    private final TreeMap<String, float[]> batchReservations = new TreeMap<>();
    private final TreeMap<String, ResourceInventory> buildingInventories = new TreeMap<>();

    /** Task 13: delivery routes from producers' loading points to the settlement. */
    private final List<DeliveryRoute> routes = new ArrayList<>();
    /** Task 13: route trip fees paid this tick (part of the operating costs). */
    private float haulingCosts;

    /** Task 14: grid energy generated by the active power plants this tick. */
    private float energyProduction;
    /** Task 14: grid energy drawn by the active consumers this tick. */
    private float energyConsumption;
    /** Task 14: production − consumption. Positive is a surplus. */
    private float energyBalance;
    /**
     * Task 14: how much of the grid's demand is met, 0..1. When the grid is
     * short, every consuming building runs at this fraction of its capacity.
     */
    private float energyEfficiency = 1f;

    // ── Task 15 technology & productivity state ──────────────────────────
    /**
     * The settlement's unlocked technology tier (1 = Manual … 5 = Advanced
     * Technology). Bonuses are cumulative and read from the catalog.
     */
    private int techLevel = 1;
    /** Coins still owed on the in-progress research project (0 = none). */
    private float researchRemainingCost;
    /** Ticks still needed to finish the research project (0 = none). */
    private float researchRemainingTicks;
    /** Coins invested into research this tick (capital, not an operating cost). */
    private float researchInvestedThisTick;

    // ── Task 16 investment & planning state ──────────────────────────────
    /** The active investment's catalog id (null when none is being funded). */
    private String activeInvestmentId;
    /** Coins still owed on the active investment (0 = paid off). */
    private float investmentRemainingCost;
    /** Coins invested into the active investment this tick (capital). */
    private float investmentInvestedThisTick;
    /** Ids of every completed investment; each can be taken at most once. */
    private final java.util.LinkedHashSet<String> completedInvestments = new java.util.LinkedHashSet<>();
    /** Cumulative permanent bonuses granted by completed investments. */
    private float investmentProductionBonus;
    private float investmentStorageBonus;
    private float investmentResearchDiscount;
    private float investmentWorkforceBonus;
    private float investmentRevenueBonus;

    // ── Task 18 development levels state ─────────────────────────────────
    /**
     * The settlement's development stage (1 = Small Settlement … 6 = Major
     * Economic Center). Advances automatically when the next level's eight
     * conditions are all met — money alone never unlocks a stage.
     */
    private int developmentLevel = 1;
    /** Cumulative units produced since the simulation started. */
    private float lifetimeProduced;
    /** Cumulative units sold to the market since the simulation started. */
    private float lifetimeSold;
    /** Cumulative coin revenue since the simulation started. */
    private float lifetimeRevenue;

    private static final class ConstructionSite {
        final String typeId;
        float remainingCost;
        float remainingTicks;

        ConstructionSite(String typeId, float remainingCost, float remainingTicks) {
            this.typeId = typeId;
            this.remainingCost = remainingCost;
            this.remainingTicks = remainingTicks;
        }
    }

    /**
     * Task 10 — immutable snapshot of one tick's operating-cost calculation.
     * {@code wages}, {@code maintenance}, {@code transport} and (Task 13)
     * {@code hauling} are real coin outflows; {@code energy} and
     * {@code materials} value the in-kind resources consumed by production at
     * the tick's market prices. {@code total} is the sum of every bucket and
     * {@code net} is revenue minus that total, so the whole calculation can
     * be inspected.
     */
    public static final class CostLedger {
        public final float revenue;
        public final float wages;
        public final float energy;
        public final float materials;
        public final float maintenance;
        public final float transport;
        /** Route trip fees paid this tick (Task 13). */
        public final float hauling;
        public final float total;
        public final float net;

        CostLedger(float revenue, float wages, float energy, float materials,
                   float maintenance, float transport, float hauling) {
            this.revenue = revenue;
            this.wages = wages;
            this.energy = energy;
            this.materials = materials;
            this.maintenance = maintenance;
            this.transport = transport;
            this.hauling = hauling;
            this.total = wages + energy + materials + maintenance + transport + hauling;
            this.net = revenue - total;
        }
    }

    /** Task 12 — which way the market is pulling a good's price. */
    public enum PriceTrend {
        /** Demand exceeds supply: the price is drifting up. */
        RISING,
        /** Supply exceeds demand: the price is drifting down. */
        FALLING,
        /** Demand and supply are balanced: the price is stable. */
        STABLE;

        static PriceTrend of(float target, float price) {
            float gap = target - price;
            if (gap > 0.001f) return RISING;
            if (gap < -0.001f) return FALLING;
            return STABLE;
        }
    }

    /**
     * Task 12 — immutable snapshot of one good's market state, showing why the
     * price moved and what sold. {@code demand} is everything pulling on the
     * good (factory inputs + consumer demand), {@code supply} is what reached
     * the market this tick, and the market clears at the smaller of the two:
     * that clearing amount is the {@code salesVolume}, earned at the current
     * {@code price}. The {@code price} drifts toward the balanced
     * {@code targetPrice} every tick, so the gap between the two explains the
     * price movement (see {@link PriceTrend}).
     */
    public static final class MarketInfo {
        public final Resource resource;
        public final float basePrice;
        public final float price;
        public final float targetPrice;
        /** Total demand on this good: factory inputs plus the town's appetite. */
        public final float demand;
        /** The part of the demand the population itself exerts. */
        public final float consumerDemand;
        /** Available supply: produced this tick plus what the settlement holds. */
        public final float supply;
        /** What actually sold on the market this tick. */
        public final float salesVolume;
        public final boolean consumerGood;
        /** Why the price moved: target vs current price. */
        public final PriceTrend priceTrend;

        MarketInfo(Resource resource, float basePrice, float price, float targetPrice,
                   float demand, float consumerDemand, float supply, float salesVolume) {
            this.resource = resource;
            this.basePrice = basePrice;
            this.price = price;
            this.targetPrice = targetPrice;
            this.demand = demand;
            this.consumerDemand = consumerDemand;
            this.supply = supply;
            this.salesVolume = salesVolume;
            this.consumerGood = resource.isConsumerGood();
            this.priceTrend = PriceTrend.of(targetPrice, price);
        }
    }

    /**
     * Task 13 — a delivery route carrying one good from a producer's loading
     * point (its on-site stockpile) to the settlement, which doubles as the
     * central warehouse/staging yard. Trucks leave the loading point, travel
     * for {@code tripTicks} ticks and then hand their load to the settlement.
     * Each dispatched trip is bounded by the trucks assigned to the route
     * times the fleet's per-truck capacity, and costs a hauling fee, so
     * transportation is a real constraint: without a route the good is still
     * shipped instantly, with a route the pipeline takes time and money.
     * Each in-flight trip is a {@link Load} that arrives on its own
     * {@code arrivalTick}.
     */
    public static final class DeliveryRoute {
        /** The producing building type whose loading point feeds the route. */
        public final String sourceTypeId;
        /** The good carried on this route. */
        public final Resource resource;
        /** Trucks assigned (0 = the whole fleet). */
        public int trucks;
        /** Ticks one trip took at dispatch time (travel time). */
        public int tripTicks;
        /** Trips dispatched since the route was created (cumulative). */
        public int tripsLaunched;
        /** In-flight trips that have not reached the settlement yet. */
        private final List<Load> loads = new ArrayList<>();

        DeliveryRoute(String sourceTypeId, Resource resource, int trucks) {
            this.sourceTypeId = sourceTypeId;
            this.resource = resource;
            this.trucks = Math.max(0, trucks);
        }

        /** Trucks actually on the road for this route (fleet default resolved). */
        int effectiveTrucks(int fleetTrucks) {
            return trucks <= 0 ? fleetTrucks : Math.min(trucks, fleetTrucks);
        }

        /** Trips currently on the road. */
        int inFlight() {
            return loads.size();
        }

        /** Units currently travelling to the settlement. */
        float inTransit() {
            float total = 0f;
            for (Load load : loads) total += load.amount;
            return total;
        }

        /** Tick the next load arrives, or 0 when nothing is on the road. */
        int nextArrivalTick() {
            int earliest = 0;
            for (Load load : loads) {
                if (earliest == 0 || load.arrivalTick < earliest) earliest = load.arrivalTick;
            }
            return earliest;
        }

        /** One in-flight truck load. */
        static final class Load {
            float amount;
            int arrivalTick;
        }
    }

    public EconomySimulation() {
        this(null);
    }

    /** Restores a simulation from a persisted snapshot (or starts fresh if null). */
    public EconomySimulation(EconomyState state) {
        int count = Resource.values().length;
        settlement = new ResourceInventory(BASE_STORAGE_CAPACITY, 0f);
        price = new float[count];
        targetPrice = new float[count];
        produced = new float[count];
        consumed = new float[count];
        sold = new float[count];
        demand = new float[count];
        consumerDemand = new float[count];
        for (Resource resource : Resource.values()) {
            price[resource.ordinal()] = resource.getBasePrice();
            targetPrice[resource.ordinal()] = resource.getBasePrice();
        }
        if (state == null) {
            population = START_POPULATION;
            settlement.setStored(Resource.COINS, START_MONEY);
            settlement.setStored(Resource.WOOD, 10f);
            settlement.setStored(Resource.STONE, 10f);
            settlement.setStored(Resource.FOOD, 12f);
        } else {
            population = state.population;
            if (state.averageWage > 0f) averageWage = state.averageWage;
            if (state.inventory != null) {
                for (EconomyState.StockState stock : state.inventory) {
                    Resource resource = Resource.byId(stock.goodId);
                    if (resource != null) settlement.setStored(resource, stock.amount);
                }
            }
            // Legacy saves stored the treasury in EconomyState.money and had no
            // COINS stock. The explicit COINS stock wins when present.
            if (settlement.getStored(Resource.COINS) <= 0f && state.money > 0f) {
                settlement.setStored(Resource.COINS, state.money);
            }
            if (state.construction != null) {
                for (EconomyState.ConstructionState construction : state.construction) {
                    sites.add(new ConstructionSite(construction.typeId,
                        construction.remainingCost, construction.remainingTicks));
                }
            }
            // Task 15: restore the unlocked tier and any in-progress research.
            // Saves without a valid tier fall back to Manual; research debts
            // and durations are clamped so a corrupt save cannot stall ticks.
            if (state.techLevel >= 1 && state.techLevel <= TechnologyCatalog.maxLevel()) {
                techLevel = state.techLevel;
            }
            researchRemainingCost = Math.max(0f, state.researchRemainingCost);
            researchRemainingTicks = Math.max(0f, state.researchRemainingTicks);
            // Task 16: restore completed investments (their permanent bonuses
            // are recomputed from the ids) and any in-progress project. Unknown
            // ids are ignored so a corrupt save cannot revive a mystery deal.
            if (state.completedInvestments != null) {
                for (String id : state.completedInvestments) {
                    Investment investment = InvestmentCatalog.get(id);
                    if (investment != null && completedInvestments.add(id)) {
                        applyInvestmentEffect(investment);
                    }
                }
            }
            if (state.activeInvestmentId != null
                && !completedInvestments.contains(state.activeInvestmentId)
                && InvestmentCatalog.get(state.activeInvestmentId) != null) {
                activeInvestmentId = state.activeInvestmentId;
                investmentRemainingCost = Math.max(0f, state.investmentRemainingCost);
            }
            // Task 18: restore the development stage and the lifetime metrics
            // that drive it. Invalid levels fall back to Small Settlement and
            // counters are clamped so a corrupt save cannot skip stages.
            if (state.developmentLevel >= 1 && state.developmentLevel <= DevelopmentCatalog.maxLevel()) {
                developmentLevel = state.developmentLevel;
            }
            lifetimeProduced = Math.max(0f, state.lifetimeProduced);
            lifetimeSold = Math.max(0f, state.lifetimeSold);
            lifetimeRevenue = Math.max(0f, state.lifetimeRevenue);
        }
    }

    // ── Building feed ────────────────────────────────────────────────────

    /** Registers one placed building of the given type. */
    public void addBuilding(String typeId, boolean alreadyBuilt) {
        placedCounts.put(typeId, placedCounts.getOrDefault(typeId, 0) + 1);
        BuildingEconomy profile = EconomyData.get(typeId);
        if (!alreadyBuilt && profile != null && profile.hasConstruction()) {
            sites.add(new ConstructionSite(typeId, profile.getConstructionCost(), profile.getConstructionTicks()));
        }
    }

    /**
     * Removes one placed building (and its construction site, if any). When
     * the last instance of a producing type is removed, its in-flight input
     * reservations are returned to the settlement and its stockpile is
     * discarded.
     */
    public void removeBuilding(String typeId) {
        Integer count = placedCounts.get(typeId);
        if (count == null || count <= 0) return;
        if (count == 1) {
            placedCounts.remove(typeId);
            releaseReservations(typeId);
        } else {
            placedCounts.put(typeId, count - 1);
        }
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).typeId.equals(typeId)) {
                sites.remove(i);
                return;
            }
        }
    }

    /**
     * Reconciles the placed-building counts against the given per-type counts
     * (computed from the grid). Missing buildings are added, extra ones removed
     * — so placement, deletion, undo/redo and save restoration all converge on
     * the same state. Deterministic.
     */
    public void reconcileBuildings(Map<String, Integer> counts, boolean alreadyBuilt) {
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int have = placedCounts.getOrDefault(entry.getKey(), 0);
            for (int i = have; i < entry.getValue(); i++) {
                addBuilding(entry.getKey(), alreadyBuilt);
            }
        }
        for (String typeId : new ArrayList<>(placedCounts.keySet())) {
            int want = counts.getOrDefault(typeId, 0);
            while (placedCounts.getOrDefault(typeId, 0) > want) {
                removeBuilding(typeId);
            }
        }
    }

    // ── Delivery routes (Task 13) ────────────────────────────────────────

    /**
     * Connects a producer's loading point to the settlement with a delivery
     * route for one good, using the whole truck fleet.
     *
     * @see #addRoute(String, Resource, int)
     */
    public DeliveryRoute addRoute(String sourceTypeId, Resource resource) {
        return addRoute(sourceTypeId, resource, 0);
    }

    /**
     * Connects a producer's loading point to the settlement with a delivery
     * route for one good. While the route exists the good is <em>not</em>
     * shipped instantly: trucks carry it over {@code tripTicks} ticks, each
     * trip carries at most the assigned {@code trucks}' combined capacity and
     * pays a hauling fee (see {@link #getHaulingCosts()}). Trucks, capacity
     * and speed come from the settlement fleet (truck depots) and the travel
     * time shrinks with every active road; warehouses raise the number of
     * trips that can be dispatched per tick. {@code trucks <= 0} assigns the
     * whole fleet. Returns the route so its state can be inspected.
     */
    public DeliveryRoute addRoute(String sourceTypeId, Resource resource, int trucks) {
        DeliveryRoute route = new DeliveryRoute(sourceTypeId, resource, trucks);
        routes.add(route);
        return route;
    }

    /** Removes a delivery route, restoring the good's instant ship-out. */
    public void removeRoute(String sourceTypeId, Resource resource) {
        for (int i = 0; i < routes.size(); i++) {
            DeliveryRoute route = routes.get(i);
            if (route.sourceTypeId.equals(sourceTypeId) && route.resource == resource) {
                routes.remove(i);
                return;
            }
        }
    }

    private boolean hasRoute(String sourceTypeId, Resource resource) {
        return findRoute(sourceTypeId, resource) != null;
    }

    private DeliveryRoute findRoute(String sourceTypeId, Resource resource) {
        for (DeliveryRoute route : routes) {
            if (route.sourceTypeId.equals(sourceTypeId) && route.resource == resource) {
                return route;
            }
        }
        return null;
    }

    // ── Time ─────────────────────────────────────────────────────────────

    /** Advances the simulation in real-time seconds, ticking at TICK_SECONDS. */
    public void update(float delta) {
        if (delta <= 0f) return;
        accumulator += delta;
        while (accumulator >= TICK_SECONDS) {
            tick();
            accumulator -= TICK_SECONDS;
        }
    }

    /** Runs exactly one simulation step. Deterministic. */
    public void tick() {
        tickCount++;
        processConstruction();
        processResearch();
        processInvestments();

        // Active buildings = placed buildings whose construction has finished.
        TreeMap<String, Integer> active = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            int activeCount = entry.getValue() - countSites(entry.getKey());
            if (activeCount > 0) active.put(entry.getKey(), activeCount);
        }

        // Technology (Task 15): the current tier's cumulative bonuses are
        // read once per tick and scale production speed, worker productivity,
        // resource efficiency, energy efficiency, storage efficiency, sale
        // prices and delivery capacity below. At Manual every factor is 1.
        Technology tech = TechnologyCatalog.get(techLevel);
        float productionFactor = tech.throughputFactor();
        float resourceFactor = tech.resourceFactor();
        float energyFactor = tech.energyFactor();
        float storageFactor = tech.storageFactor();
        float qualityFactor = tech.qualityFactor();
        float transportFactor = tech.transportFactor();

        for (int i = 0; i < produced.length; i++) produced[i] = 0f;
        for (int i = 0; i < consumed.length; i++) consumed[i] = 0f;
        for (int i = 0; i < sold.length; i++) sold[i] = 0f;
        for (int i = 0; i < demand.length; i++) demand[i] = 0f;
        for (int i = 0; i < consumerDemand.length; i++) consumerDemand[i] = 0f;

        // Workforce (Task 9): only part of the population is of working age,
        // so the working population is the pool every active building hires
        // from. Each active instance with a workforce requirement takes up to
        // that many workers; the pool is assigned greedily in sorted type
        // order. Whatever cannot be filled stays unemployed, and the wage is
        // paid only for the workers actually hired.
        // Task 16: the worker-education investment raises the working-age
        // share, so a larger pool hires from the same population.
        workingPopulation = population * (WORKFORCE_PARTICIPATION + investmentWorkforceBonus);
        workforceDemand = 0f;
        employedWorkers = 0f;
        staffedByType.clear();
        float remainingWorkforce = workingPopulation;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile == null || profile.getWorkforce() <= 0f) continue;
            float workforce = profile.getWorkforce();
            workforceDemand += workforce * entry.getValue();
            int staffable = (int) Math.min(entry.getValue(), remainingWorkforce / workforce);
            if (staffable <= 0) continue;
            staffedByType.put(entry.getKey(), staffable);
            remainingWorkforce -= staffable * workforce;
            employedWorkers += staffable * workforce;
        }
        workforceAssigned = employedWorkers;

        // Shared storage: base capacity plus every active warehouse/storage
        // building's inventory capacity, scaled by storage efficiency.
        settlement.setCapacity(settlementCapacityFor(active, storageFactor));

        // Energy network (Task 14): the settlement's backup grid plus every
        // active power plant feeds the network; every active consumer draws
        // from it. The balance is the spec's "energy production − energy
        // consumption". When the grid is short (consumption exceeds
        // production) the settlement runs at the fraction of its demand that
        // is actually met, and every consuming building's production
        // efficiency drops to that fraction.
        energyProduction = BASE_ENERGY;
        energyConsumption = 0f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile == null) continue;
            energyProduction += profile.getEnergyProduced() * entry.getValue();
            energyConsumption += profile.getEnergyConsumed() * entry.getValue() * energyFactor;
        }
        energyBalance = energyProduction - energyConsumption;
        energyEfficiency = energyConsumption <= 0f ? 1f
            : clamp(energyProduction / energyConsumption, 0f, 1f);

        // Production (Task 7/8): a batch starts by reserving its full inputs
        // plus energy from the settlement's available stock; it then accrues
        // one production-time unit per tick (scaled by capacity) and, on
        // completion, fulfils the reservation (real consumption) and places its
        // output into the building's own stockpile. Partial runs never happen,
        // and a factory whose stockpile is full stops starting batches.
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile == null || !profile.isProducer()) continue;
            int count = entry.getValue();
            // Free nature buildings run without staff; everything else runs
            // only as many instances as the workforce pass could hire.
            int staffable;
            if (profile.getWorkforce() > 0f) {
                staffable = staffedByType.getOrDefault(entry.getKey(), 0);
            } else {
                staffable = count;
            }
            if (staffable <= 0) continue;

            ResourceInventory inv = buildingInventory(entry.getKey(), profile, staffable, storageFactor);

            int slots = batchSlots.getOrDefault(entry.getKey(), 0);
            while (slots < staffable) {
                // Storage full → production limited: never start a batch when
                // the factory's own stockpile cannot hold the output.
                if (inv.getCapacity() > 0f && inv.getStoredTotal() >= inv.getCapacity()) {
                    break;
                }
                boolean canReserve = true;
                for (Resource input : Resource.values()) {
                    float need = profile.getInput(input) * resourceFactor;
                    if (need <= 0f) continue;
                    demand[input.ordinal()] += need;
                    if (settlement.getAvailable(input) < need) canReserve = false;
                }
                if (profile.getEnergyRequired() > 0f) {
                    demand[Resource.COAL.ordinal()] += profile.getEnergyRequired();
                    if (settlement.getAvailable(Resource.COAL) < profile.getEnergyRequired()) {
                        canReserve = false;
                    }
                }
                if (!canReserve) break;

                float[] held = batchReservations.computeIfAbsent(entry.getKey(),
                    key -> new float[Resource.values().length]);
                for (Resource input : Resource.values()) {
                    float need = profile.getInput(input) * resourceFactor;
                    if (need <= 0f) continue;
                    settlement.reserve(input, need);
                    inv.markIncoming(input, need);
                    inv.reserve(input, need);
                    held[input.ordinal()] += need;
                }
                if (profile.getEnergyRequired() > 0f) {
                    float energy = profile.getEnergyRequired();
                    settlement.reserve(Resource.COAL, energy);
                    inv.markIncoming(Resource.COAL, energy);
                    inv.reserve(Resource.COAL, energy);
                    held[Resource.COAL.ordinal()] += energy;
                }
                slots++;
            }

            float progress = productionProgress.getOrDefault(entry.getKey(), 0f);
            // Task 14: an energy shortage throttles only the consuming
            // buildings. Producers that draw no grid power run at full speed.
            float typeEfficiency = profile.getEnergyConsumed() > 0f ? energyEfficiency : 1f;
            // Task 15: production speed × worker productivity scale the batch
            // progress, so staffed producers finish batches faster.
            progress += slots * profile.getProductionCapacity() * typeEfficiency * productionFactor;
            float productionTime = profile.getProductionTime();
            while (progress >= productionTime && slots > 0) {
                float[] held = batchReservations.get(entry.getKey());
                if (held != null) {
                    for (Resource input : Resource.values()) {
                        float need = held[input.ordinal()];
                        if (need <= 0f) continue;
                        settlement.fulfillReservation(input, need);
                        consumed[input.ordinal()] += need;
                        inv.releaseReservation(input, need);
                        inv.markConsumed(input, need);
                        held[input.ordinal()] = 0f;
                    }
                }
                for (Resource output : Resource.values()) {
                    float outputRate = profile.getOutput(output);
                    if (outputRate <= 0f) continue;
                    // Task 16: the capacity-expansion investment yields more
                    // product per completed batch.
                    float batchOutput = outputRate * (1f + investmentProductionBonus);
                    inv.produceInto(output, batchOutput);
                    produced[output.ordinal()] += batchOutput;
                }
                slots--;
                progress -= productionTime;
            }
            batchSlots.put(entry.getKey(), slots);
            if (slots == 0) batchReservations.remove(entry.getKey());
            productionProgress.put(entry.getKey(), progress);
        }

        // Routes (Task 13): delivery trips carry goods from a producer's
        // loading point (its stockpile) to the settlement, which doubles as
        // the central warehouse. Trucks take tripTicks to travel, each trip
        // carries at most the route's trucks × the fleet's per-truck capacity,
        // and every dispatched trip costs BASE_TRIP_COST (hauling) paid from
        // the treasury. Truck depots grow the fleet and its capacity/speed,
        // roads shorten travel, and each warehouse stages extra dispatches per
        // tick. Routed goods arrive here; the ship-out step below then skips
        // them so nothing is duplicated.
        haulingCosts = 0f;
        if (!routes.isEmpty()) {
            int fleetTrucks = fleetTrucks();
            float truckCapacity = truckCapacity();
            int currentTick = tickCount;
            for (DeliveryRoute route : routes) {
                // Arrivals: loads whose travel finished hand their cargo to
                // the settlement. A full warehouse delays a load by one tick.
                float room = Math.max(0f, settlement.getCapacity() - nonMoneyStored() - nonMoneyReserved());
                for (int i = route.loads.size() - 1; i >= 0; i--) {
                    DeliveryRoute.Load load = route.loads.get(i);
                    if (load.arrivalTick > currentTick) continue;
                    if (room <= 0f) {
                        load.arrivalTick = currentTick + 1;
                        continue;
                    }
                    float delivered = Math.min(load.amount, room);
                    settlement.receiveProduced(route.resource, delivered);
                    load.amount -= delivered;
                    room -= delivered;
                    if (load.amount <= 0f) route.loads.remove(i);
                }
                // Dispatch: load the trucks at the loading point and send them
                // off. Each trip is bounded by the assigned trucks' combined
                // capacity; at most dispatchBudget trips start per tick.
                ResourceInventory inv = buildingInventories.get(route.sourceTypeId);
                int trucksOnRoute = route.effectiveTrucks(fleetTrucks);
                int launches = 0;
                while (launches < dispatchBudget()) {
                    if (inv == null || inv.getStored(route.resource) <= 0f) break;
                    int tripTicks = tripTicks(truckSpeed(), roadFactor());
                    float capacity = trucksOnRoute * truckCapacity * transportFactor;
                    float load = Math.min(inv.getStored(route.resource), capacity);
                    inv.shipOut(route.resource, load);
                    DeliveryRoute.Load trip = new DeliveryRoute.Load();
                    trip.amount = load;
                    trip.arrivalTick = currentTick + tripTicks;
                    route.loads.add(trip);
                    route.tripTicks = tripTicks;
                    route.tripsLaunched++;
                    haulingCosts += BASE_TRIP_COST;
                    launches++;
                }
            }
        }

        // Ship-out: finished output moves from building stockpiles into the
        // settlement while shared storage has room (reserved stock still counts
        // against the capacity). Goods covered by a delivery route are moved
        // by trucks instead, so they stay at the loading point until picked up.
        float room = Math.max(0f, settlement.getCapacity() - nonMoneyStored() - nonMoneyReserved());
        for (Map.Entry<String, ResourceInventory> entry : buildingInventories.entrySet()) {
            ResourceInventory inv = entry.getValue();
            if (inv.getStoredTotal() <= 0f) continue;
            for (Resource resource : Resource.values()) {
                if (resource.isMoney()) continue;
                if (hasRoute(entry.getKey(), resource)) continue;
                float stored = inv.getStored(resource);
                if (stored <= 0f || room <= 0f) continue;
                float ship = Math.min(stored, room);
                inv.shipOut(resource, ship);
                settlement.receiveProduced(resource, ship);
                room -= ship;
            }
        }

        // Consumption: the population eats food every tick.
        float foodNeed = population * FOOD_PER_PERSON;
        demand[Resource.FOOD.ordinal()] += foodNeed;
        float foodConsumed = Math.min(foodNeed, settlement.getStored(Resource.FOOD));
        settlement.consume(Resource.FOOD, foodConsumed);
        consumed[Resource.FOOD.ordinal()] += foodConsumed;
        float foodShortfall = foodNeed - foodConsumed;

        // Consumer demand (Task 11): beyond what the factories need for their
        // batches, the population itself wants goods. Every citizen has a
        // per-product preference weight; the appetite is cut when the price
        // rises above its base (elasticity) and grows with the settlement's
        // development — the more buildings are active, the more the town wants
        // to live well. This consumer pressure is what keeps finished goods
        // valuable instead of crashing to their price floor. It is a market
        // signal, not a consumption flow: only food is really eaten, the rest
        // just tells the market how much the town would like.
        developmentFactor = 1f;
        int activeBuildingCount = 0;
        for (Integer count : active.values()) activeBuildingCount += count;
        if (activeBuildingCount > 0) {
            developmentFactor = clamp(1f + DEVELOPMENT_PRESSURE * activeBuildingCount,
                1f, MAX_DEVELOPMENT_FACTOR);
        }
        consumerDemand[Resource.FOOD.ordinal()] = foodNeed;
        for (Resource resource : Resource.values()) {
            if (resource.isMoney() || resource == Resource.FOOD) continue;
            float preference = resource.getConsumerPreference();
            if (preference <= 0f) continue;
            float appetite = population * preference * getPriceElasticity(resource) * developmentFactor;
            consumerDemand[resource.ordinal()] = appetite;
            demand[resource.ordinal()] += appetite;
        }

        // Market: prices drift toward demand/supply balance. Money has no
        // market — it is a medium of exchange, never produced or priced.
        // The supply the market sees is what the settlement produced this tick
        // plus what is stored (production capacity, available resources,
        // workforce, factory efficiency and transport all feed into it):
        //    production capacity  — batch slots × production capacity per tick
        //    available resources  — reservations only start when inputs exist
        //    workforce            — staffed instances set the batch slot count
        //    factory efficiency   — progress accrues at capacity per slot
        //    transportation       — ship-out and warehouses move output to market
        // Prices respond gradually: each tick closes only PRICE_DRIFT of the
        // gap toward the target, so the market never jumps.
        for (Resource resource : Resource.values()) {
            if (resource.isMoney()) continue;
            float supply = getMarketSupply(resource);
            float base = resource.getBasePrice();
            float target = base * (1f + DEMAND_PRESSURE * (demand[resource.ordinal()] - supply)
                / Math.max(supply, 1f));
            target = clamp(target, base * PRICE_FLOOR, base * PRICE_CEILING);
            targetPrice[resource.ordinal()] = target;
            price[resource.ordinal()] += (target - price[resource.ordinal()]) * PRICE_DRIFT;
        }

        // Sales (Task 12): the market clears the smaller of available supply
        // and consumer demand — the town buys what it wants and the rest stays
        // unsold. Overproduced goods pile up in inventory and their price
        // falls; scarce goods sell out and their price rises. Food is never a
        // market transaction (the population eats it directly), and the other
        // non-consumer goods (raw materials, energy, industrial inputs) are
        // exported only when they overflow their own storage cap, exactly as
        // before.
        float storageBonus = storageCapacity(active);
        float marketMultiplier = marketMultiplier(active);
        revenue = 0f;
        for (Resource resource : Resource.values()) {
            if (resource.isMoney()) continue;
            float volume;
            if (resource.isConsumerGood()) {
                volume = Math.min(getMarketSupply(resource), consumerDemand[resource.ordinal()]);
            } else {
                float capacity = resource.getStorageLimit() + storageBonus;
                volume = Math.max(0f, settlement.getStored(resource) - capacity);
            }
            if (volume <= 0f) continue;
            settlement.shipOut(resource, volume);
            sold[resource.ordinal()] += volume;
            // Task 15: better product quality sells for more per unit.
            // Task 16: the export-agreement investment lifts every sale price.
            revenue += volume * price[resource.ordinal()] * marketMultiplier * qualityFactor
                * (1f + investmentRevenueBonus);
        }
        settlement.receive(Resource.COINS, revenue);

        // Wages (Task 9): every employed worker is paid the average wage,
        // which drifts toward a target driven by labor scarcity — the tighter
        // the labor market, the higher wages go. Wages are an operating cost
        // paid from the treasury, so staffing a large factory base is real
        // ongoing expenditure that must be covered by sales.
        float unemploymentRate = workingPopulation > 0f
            ? 1f - Math.min(1f, employedWorkers / workingPopulation) : 1f;
        float targetWage = WAGE_BASE * (1f + LABOR_SCARCITY * (1f - unemploymentRate));
        averageWage += (targetWage - averageWage) * WAGE_DRIFT;
        wageCosts = employedWorkers * averageWage;

        // Operating costs and profit (Task 10). The tick's cost ledger splits
        // the recurring costs of the whole settlement into named buckets:
        //   wages       — employed workers × average wage (paid in coins)
        //   maintenance — each active building's operating cost (paid in coins)
        //   transport   — warehousing fees on stored stock (paid in coins)
        //   hauling     — delivery route trip fees (paid in coins, Task 13)
        //   energy      — coal burnt by production, valued at the coal price
        //   materials   — inputs consumed by production, valued at market price
        // The treasury actually pays only wages, maintenance, transport and
        // hauling; energy and materials are the in-kind resources the
        // factories used up, so their cost is presented as a valuation, not a
        // coin outflow. Population food is not a production material (it is
        // how the settlement feeds itself), so it stays out of the ledger.
        maintenanceCosts = 0f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null) maintenanceCosts += profile.getOperatingCost() * entry.getValue();
        }
        float storageCostRate = 0f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null && profile.isStorage()) {
                storageCostRate += profile.getStorageCostPerUnit() * entry.getValue();
            }
        }
        storageCosts = nonMoneyStored() * storageCostRate;
        energyCosts = consumed[Resource.COAL.ordinal()] * price[Resource.COAL.ordinal()];
        materialsCosts = 0f;
        for (Resource resource : Resource.values()) {
            if (resource.isMoney() || resource == Resource.COAL || resource == Resource.FOOD) continue;
            materialsCosts += consumed[resource.ordinal()] * price[resource.ordinal()];
        }
        operatingCosts = maintenanceCosts + storageCosts + wageCosts + haulingCosts;
        settlement.consume(Resource.COINS, operatingCosts);
        profit = revenue - operatingCosts;
        totalProfit += profit;

        // Population: grows toward housing when fed, shrinks when starving.
        float housing = housingCapacity(active);
        if (foodShortfall > 0f) {
            population = Math.max(0f, population - foodShortfall * STARVATION_PENALTY);
        } else if (population < housing) {
            population = Math.min(housing, population + GROWTH_RATE);
        }

        // Task 18: lifetime metrics feed the development conditions, and the
        // settlement advances a stage when every threshold of the next level
        // is met. Development is driven by real growth, never money alone.
        lifetimeProduced += sum(produced);
        lifetimeSold += sum(sold);
        lifetimeRevenue += revenue;
        updateDevelopment();
    }

    private void processConstruction() {
        investedThisTick = 0f;
        for (int i = sites.size() - 1; i >= 0; i--) {
            ConstructionSite site = sites.get(i);
            if (site.remainingCost > 0f && settlement.getStored(Resource.COINS) > 0f) {
                float invest = Math.min(settlement.getStored(Resource.COINS), Math.min(site.remainingCost, INVEST_SPEED));
                settlement.consume(Resource.COINS, invest);
                site.remainingCost -= invest;
                investedThisTick += invest;
            }
            if (site.remainingCost <= 0f) {
                site.remainingTicks -= 1f;
                if (site.remainingTicks <= 0f) {
                    sites.remove(i);
                }
            }
        }
        totalInvestment += investedThisTick;
    }

    /**
     * Task 15: funds the in-progress research project like construction —
     * coins each tick until the price is paid, then the research ticks down
     * until it finishes. Research is capital investment, so what is paid is
     * reported separately from the operating costs.
     */
    private void processResearch() {
        researchInvestedThisTick = 0f;
        if (isMaxTechnology()) return;
        if (researchRemainingCost > 0f && settlement.getStored(Resource.COINS) > 0f) {
            float invest = Math.min(settlement.getStored(Resource.COINS),
                Math.min(researchRemainingCost, RESEARCH_SPEED));
            settlement.consume(Resource.COINS, invest);
            researchRemainingCost -= invest;
            researchInvestedThisTick += invest;
        }
        if (researchRemainingCost <= 0f && researchRemainingTicks > 0f) {
            researchRemainingTicks -= 1f;
            if (researchRemainingTicks <= 0f) {
                techLevel = Math.min(TechnologyCatalog.maxLevel(), techLevel + 1);
                researchRemainingTicks = 0f;
            }
        }
    }

    /**
     * Task 16: funds the active investment project like construction and
     * research — coins each tick until the price is paid, then the investment
     * completes and its permanent long-term effect is applied. An investment
     * can never be repeated, and only one is funded at a time.
     */
    private void processInvestments() {
        investmentInvestedThisTick = 0f;
        if (activeInvestmentId == null) return;
        if (investmentRemainingCost > 0f && settlement.getStored(Resource.COINS) > 0f) {
            float invest = Math.min(settlement.getStored(Resource.COINS),
                Math.min(investmentRemainingCost, INVESTMENT_SPEED));
            settlement.consume(Resource.COINS, invest);
            investmentRemainingCost -= invest;
            investmentInvestedThisTick += invest;
        }
        if (investmentRemainingCost <= 0f) {
            completeInvestment(activeInvestmentId);
        }
    }

    /** Applies an investment's permanent effect and records it as done. */
    private void completeInvestment(String id) {
        Investment investment = InvestmentCatalog.get(id);
        completedInvestments.add(id);
        if (investment != null) applyInvestmentEffect(investment);
        activeInvestmentId = null;
        investmentRemainingCost = 0f;
    }

    private void applyInvestmentEffect(Investment investment) {
        switch (investment.getCategory()) {
            case PRODUCTION:
                investmentProductionBonus += investment.getEffectMagnitude();
                break;
            case INFRASTRUCTURE:
                investmentStorageBonus += investment.getEffectMagnitude();
                break;
            case TECHNOLOGY:
                investmentResearchDiscount += investment.getEffectMagnitude();
                break;
            case WORKFORCE:
                investmentWorkforceBonus += investment.getEffectMagnitude();
                break;
            case MARKETS:
                investmentRevenueBonus += investment.getEffectMagnitude();
                break;
            default:
                break;
        }
    }

    private int countSites(String typeId) {
        int count = 0;
        for (ConstructionSite site : sites) {
            if (site.typeId.equals(typeId)) count++;
        }
        return count;
    }

    private float marketMultiplier(Map<String, Integer> active) {
        float multiplier = 1f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null) multiplier += profile.getMarketBonus() * entry.getValue();
        }
        return Math.min(multiplier, MAX_MARKET_MULTIPLIER);
    }

    private float storageCapacity(Map<String, Integer> active) {
        float capacity = 0f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null) capacity += profile.getStorageBonus() * entry.getValue();
        }
        return capacity;
    }

    private float housingCapacity(Map<String, Integer> active) {
        float housing = 0f;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null) housing += profile.getHousing() * entry.getValue();
        }
        return housing;
    }

    private float settlementCapacityFor(Map<String, Integer> active, float storageFactor) {
        float capacity = BASE_STORAGE_CAPACITY;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null && profile.isStorage()) {
                capacity += profile.getInventoryCapacity() * entry.getValue() * storageFactor;
            }
        }
        // Task 16: the storage-expansion investment adds permanent capacity.
        return capacity + investmentStorageBonus;
    }

    private float nonMoneyStored() {
        float total = 0f;
        for (Resource resource : Resource.values()) {
            if (!resource.isMoney()) total += settlement.getStored(resource);
        }
        return total;
    }

    private float nonMoneyReserved() {
        float total = 0f;
        for (Resource resource : Resource.values()) {
            if (!resource.isMoney()) total += settlement.getReserved(resource);
        }
        return total;
    }

    // ── Task 13 transport fleet / roads / dispatch ───────────────────────

    /**
     * The settlement's truck fleet: the base fleet plus every active truck
     * depot's trucks. Routes share this fleet.
     */
    private int fleetTrucks() {
        int trucks = BASE_TRUCKS;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0) {
                trucks += (int) profile.getTrucksProvided() * activeCount;
            }
        }
        return trucks;
    }

    /** Units one truck can carry per trip, scaled by the depots' bonus. */
    private float truckCapacity() {
        float bonus = 0f;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0) {
                bonus += profile.getTruckCapacityBonus() * activeCount;
            }
        }
        return BASE_TRUCK_CAPACITY * (1f + bonus);
    }

    /** Distance units a truck covers per tick, scaled by the depots' bonus. */
    private float truckSpeed() {
        float bonus = 0f;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0) {
                bonus += profile.getTruckSpeedBonus() * activeCount;
            }
        }
        return BASE_TRUCK_SPEED * (1f + bonus);
    }

    /** 1 + every active road's bonus: how much faster trips are today. */
    private float roadFactor() {
        float bonus = 0f;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0) {
                bonus += profile.getRoadBonus() * activeCount;
            }
        }
        return 1f + bonus;
    }

    /** Delivery trips any route can start per tick (1 + warehouse staging). */
    private int dispatchBudget() {
        int staging = 0;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0) {
                staging += (int) profile.getStagingBonus() * activeCount;
            }
        }
        return DISPATCH_BASE + staging;
    }

    /** Travel time of one delivery trip, rounded to whole ticks. */
    private int tripTicks(float truckSpeed, float roadFactor) {
        return Math.max(1, Math.round(ROUTE_DISTANCE / Math.max(0.1f, truckSpeed * roadFactor)));
    }

    private ResourceInventory buildingInventory(String typeId, BuildingEconomy profile, int staffable, float storageFactor) {
        ResourceInventory inv = buildingInventories.get(typeId);
        if (inv == null) {
            inv = new ResourceInventory(0f, 0f);
            buildingInventories.put(typeId, inv);
        }
        inv.setCapacity(profile.getInventoryCapacity() * staffable * storageFactor);
        return inv;
    }

    /** Returns a producer's in-flight input reservations to the settlement. */
    private void releaseReservations(String typeId) {
        float[] held = batchReservations.remove(typeId);
        if (held != null) {
            ResourceInventory inv = buildingInventories.get(typeId);
            for (Resource resource : Resource.values()) {
                float amount = held[resource.ordinal()];
                if (amount <= 0f) continue;
                settlement.releaseReservation(resource, amount);
                if (inv != null) inv.releaseReservation(resource, amount);
            }
        }
        batchSlots.remove(typeId);
        productionProgress.remove(typeId);
        buildingInventories.remove(typeId);
    }

    // ── Read-outs ────────────────────────────────────────────────────────

    /** The treasury: the COINS quantity of the settlement inventory. */
    public float getMoney() { return settlement.getStored(Resource.COINS); }
    public float getPopulation() { return population; }
    public float getInventory(Resource resource) { return settlement.getStored(resource); }
    public float getPrice(Resource resource) { return price[resource.ordinal()]; }
    public float getProduced(Resource resource) { return produced[resource.ordinal()]; }
    public float getConsumed(Resource resource) { return consumed[resource.ordinal()]; }
    public float getSold(Resource resource) { return sold[resource.ordinal()]; }
    public float getDemand(Resource resource) { return demand[resource.ordinal()]; }
    public float getRevenue() { return revenue; }
    public float getOperatingCosts() { return operatingCosts; }
    /** Warehousing fees paid this tick (part of the operating costs). */
    public float getStorageCosts() { return storageCosts; }
    public float getProfit() { return profit; }

    // ── Task 11 supply & demand read-outs ────────────────────────────────

    /**
     * The demand the population itself exerts on the market this tick, on top
     * of the factories' input needs. Food's consumer demand is simply the
     * meals the population needs to eat; every other good is appetite driven
     * by preference, price elasticity and local development.
     */
    public float getConsumerDemand(Resource resource) {
        return consumerDemand[resource.ordinal()];
    }

    /**
     * How strongly the current price curbs consumer appetite for one good.
     * 1 means "as much as the town would like at the base price"; below 1 a
     * high price is dampening demand, above 1 a bargain is inflating it
     * (capped at {@value #MAX_ELASTICITY_FACTOR}).
     */
    public float getPriceElasticity(Resource resource) {
        float base = resource.getBasePrice();
        return clamp(1f + PRICE_ELASTICITY * (base - price[resource.ordinal()]) / base,
            0f, MAX_ELASTICITY_FACTOR);
    }

    /**
     * Demand multiplier from local development: 1 + DEVELOPMENT_PRESSURE per
     * active building (capped at {@value #MAX_DEVELOPMENT_FACTOR}). A town with
     * many active buildings wants more of everything.
     */
    public float getDevelopmentFactor() { return developmentFactor; }

    /**
     * The supply the market sees for one good this tick: what was produced
     * plus what the settlement has stored. Production capacity, available
     * resources, workforce, factory efficiency and transportation all flow
     * into this figure.
     */
    public float getMarketSupply(Resource resource) {
        return produced[resource.ordinal()] + settlement.getStored(resource);
    }

    // ── Task 12 market & sales read-outs ─────────────────────────────────

    /**
     * The balanced price the market is drifting toward this tick. The gap
     * between the current {@link #getPrice(Resource)} and this target is why
     * the price is rising, falling or staying put.
     */
    public float getMarketTargetPrice(Resource resource) {
        return targetPrice[resource.ordinal()];
    }

    /**
     * One good's full market picture for this tick: price vs base vs target,
     * demand vs supply, and the sales volume the market actually cleared —
     * everything the player needs to see why prices and sales changed.
     */
    public MarketInfo getMarketInfo(Resource resource) {
        return new MarketInfo(resource, resource.getBasePrice(),
            price[resource.ordinal()], targetPrice[resource.ordinal()],
            demand[resource.ordinal()], consumerDemand[resource.ordinal()],
            getMarketSupply(resource), sold[resource.ordinal()]);
    }

    // ── Task 13 transport & logistics read-outs ─────────────────────────

    /** Delivery trip fees paid this tick (part of the operating costs). */
    public float getHaulingCosts() { return haulingCosts; }

    /** Total transportation cost this tick: warehousing fees plus hauling. */
    public float getTransportCosts() { return storageCosts + haulingCosts; }

    /** The settlement's truck fleet (base fleet plus truck depots). */
    public int getTruckCount() { return fleetTrucks(); }

    /** Units one truck can carry per trip today. */
    public float getTruckCapacity() { return truckCapacity(); }

    /** Distance units a truck covers per tick today. */
    public float getTruckSpeed() { return truckSpeed(); }

    /** How much faster delivery trips are today (1 + road bonuses). */
    public float getRoadFactor() { return roadFactor(); }

    /** Delivery trips a route can start per tick (1 + warehouse staging). */
    public int getDispatchBudget() { return dispatchBudget(); }

    /** One trip's travel time in ticks, given the current roads and fleet. */
    public float getRouteTripTicks(String sourceTypeId, Resource resource) {
        return tripTicks(truckSpeed(), roadFactor());
    }

    /** Total units the whole fleet can carry on one trip today. */
    public float getTransportCapacity() {
        return fleetTrucks() * truckCapacity();
    }

    /** The delivery route for a source type + good, or null. */
    public DeliveryRoute getRoute(String sourceTypeId, Resource resource) {
        return findRoute(sourceTypeId, resource);
    }

    /** Delivery trips currently on the road for a route (0 if none). */
    public int getRouteTrips(String sourceTypeId, Resource resource) {
        DeliveryRoute route = findRoute(sourceTypeId, resource);
        return route == null ? 0 : route.inFlight();
    }

    /** Units a route currently has travelling to the settlement. */
    public float getRouteInTransit(String sourceTypeId, Resource resource) {
        DeliveryRoute route = findRoute(sourceTypeId, resource);
        return route == null ? 0f : route.inTransit();
    }

    /** Tick the route's next load arrives, or 0 when nothing is on the road. */
    public int getRouteArrivalTick(String sourceTypeId, Resource resource) {
        DeliveryRoute route = findRoute(sourceTypeId, resource);
        return route == null ? 0 : route.nextArrivalTick();
    }

    /** Units one trip of the route can carry today (assigned trucks × capacity, scaled by transport efficiency). */
    public float getRouteCapacity(String sourceTypeId, Resource resource) {
        DeliveryRoute route = findRoute(sourceTypeId, resource);
        if (route == null) return 0f;
        return route.effectiveTrucks(fleetTrucks()) * truckCapacity() * TechnologyCatalog.get(techLevel).transportFactor();
    }

    /** Number of delivery routes currently serving the settlement. */
    public int getActiveRouteCount() { return routes.size(); }

    /** Units of every routed good currently travelling to the settlement. */
    public float getTotalInTransit() {
        float total = 0f;
        for (DeliveryRoute route : routes) total += route.inTransit();
        return total;
    }

    // ── Task 14 energy network read-outs ─────────────────────────────────

    /** Grid energy the active power plants feed the network this tick. */
    public float getEnergyProduction() { return energyProduction; }

    /** Grid energy the active consumers draw from the network this tick. */
    public float getEnergyConsumption() { return energyConsumption; }

    /** Energy balance: production − consumption (positive is a surplus). */
    public float getEnergyBalance() { return energyBalance; }

    /**
     * How much of the grid's demand is met, 0..1 (1 when the grid is
     * self-sufficient). Every consuming building runs at this efficiency, so
     * an energy shortage throttles factories instead of stopping them dead.
     */
    public float getEnergyEfficiency() { return energyEfficiency; }

    /** Grid energy the active instances of a type produce per tick. */
    public float getBuildingEnergyProduced(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getEnergyProduced() * getActiveCount(typeId);
    }

    /** Grid energy the active instances of a type draw per tick. */
    public float getBuildingEnergyConsumed(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getEnergyConsumed() * getActiveCount(typeId);
    }

    // ── Task 15 technology & productivity read-outs ──────────────────────

    /** The settlement's unlocked tier level (1 = Manual … 5 = Advanced Technology). */
    public int getTechLevel() { return techLevel; }

    /** The current tier's full data (bonuses, name key). */
    public Technology getTechnology() { return TechnologyCatalog.get(techLevel); }

    /** i18n key for the current tier's name, e.g. {@code tech.semi}. */
    public String getTechnologyNameKey() { return getTechnology().getNameKey(); }

    /** True when the highest tier has been reached. */
    public boolean isMaxTechnology() { return techLevel >= TechnologyCatalog.maxLevel(); }

    /** True while a research project is in progress. */
    public boolean isResearching() { return researchRemainingCost > 0f || researchRemainingTicks > 0f; }

    /** Coins still owed on the in-progress research project (0 when none). */
    public float getResearchRemainingCost() { return researchRemainingCost; }

    /** Ticks still needed to finish the research project (0 when none). */
    public float getResearchRemainingTicks() { return researchRemainingTicks; }

    /** Coins invested into research this tick (capital, not an operating cost). */
    public float getResearchInvestedThisTick() { return researchInvestedThisTick; }

    /**
     * Overall progress of the in-progress project as coin-equivalent work:
     * cost paid plus funded research ticks, out of the project's total. 0
     * when nothing is being researched, 1 when the current tier is the last.
     */
    public float getResearchProgress() {
        if (isMaxTechnology()) return 1f;
        if (!isResearching()) return 0f;
        Technology next = TechnologyCatalog.get(techLevel + 1);
        float total = next.getResearchCost() + next.getResearchTicks() * RESEARCH_SPEED;
        if (total <= 0f) return 0f;
        float done = (next.getResearchCost() - researchRemainingCost)
            + (next.getResearchTicks() - researchRemainingTicks) * RESEARCH_SPEED;
        return Math.max(0f, Math.min(1f, done / total));
    }

    /**
     * Starts research on the next tier. Fails (returns false) when the
     * highest tier is already unlocked or a project is already running.
     */
    public boolean startResearch() {
        if (isMaxTechnology()) return false;
        if (isResearching()) return false;
        Technology next = TechnologyCatalog.get(techLevel + 1);
        // Task 16: the research-endowment investment discounts the next project.
        researchRemainingCost = next.getResearchCost() * (1f - investmentResearchDiscount);
        researchRemainingTicks = next.getResearchTicks();
        return true;
    }

    /** Combined speed × productivity throughput factor of the current tier. */
    public float getProductivityFactor() { return getTechnology().throughputFactor(); }

    /** Production-speed factor (1 at Manual, 2 at Advanced Technology). */
    public float getProductionSpeedFactor() { return getTechnology().speedFactor(); }

    /** Worker-productivity factor (1 at Manual, 1.5 at Advanced Technology). */
    public float getWorkerProductivityFactor() { return getTechnology().productivityFactor(); }

    /** Resource-efficiency factor (1 at Manual, 0.8 at Advanced Technology). */
    public float getResourceEfficiencyFactor() { return getTechnology().resourceFactor(); }

    /** Energy-efficiency factor (1 at Manual, 0.8 at Advanced Technology). */
    public float getEnergyEfficiencyFactor() { return getTechnology().energyFactor(); }

    /** Storage-efficiency factor (1 at Manual, 1.4 at Advanced Technology). */
    public float getStorageEfficiencyFactor() { return getTechnology().storageFactor(); }

    /** Product-quality factor (1 at Manual, 1.2 at Advanced Technology). */
    public float getProductQualityFactor() { return getTechnology().qualityFactor(); }

    /** Transportation-efficiency factor (1 at Manual, 1.4 at Advanced Technology). */
    public float getTransportEfficiencyFactor() { return getTechnology().transportFactor(); }

    /**
     * Test/sandbox hook: sets the tier directly, cancelling any in-progress
     * research. Clamped to the valid range 1..5. Package-private on purpose —
     * the live game always progresses through funded research.
     */
    void setTechnologyLevel(int level) {
        techLevel = Math.max(1, Math.min(TechnologyCatalog.maxLevel(), level));
        researchRemainingCost = 0f;
        researchRemainingTicks = 0f;
    }

    // ── Task 16 investment & planning read-outs ──────────────────────────

    /**
     * Starts funding an investment project. Fails (returns false) when the id
     * is unknown, an investment is already being funded, or the investment was
     * already completed — each can be taken at most once.
     */
    public boolean startInvestment(String id) {
        if (id == null) return false;
        if (activeInvestmentId != null) return false;
        if (completedInvestments.contains(id)) return false;
        Investment investment = InvestmentCatalog.get(id);
        if (investment == null) return false;
        activeInvestmentId = id;
        investmentRemainingCost = investment.getCost();
        return true;
    }

    /** True while an investment project is being funded. */
    public boolean isInvesting() { return activeInvestmentId != null; }

    /** The active investment's catalog id, or null. */
    public String getActiveInvestmentId() { return activeInvestmentId; }

    /** Coins still owed on the active investment (0 when none). */
    public float getInvestmentRemainingCost() { return investmentRemainingCost; }

    /** Coins invested into the active investment this tick (capital). */
    public float getInvestmentInvestedThisTick() { return investmentInvestedThisTick; }

    /** Overall funding progress of the active investment, 0..1 (0 when none). */
    public float getInvestmentProgress() {
        if (activeInvestmentId == null) return 0f;
        Investment investment = InvestmentCatalog.get(activeInvestmentId);
        float total = investment == null ? investmentRemainingCost : investment.getCost();
        if (total <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, 1f - investmentRemainingCost / total));
    }

    /** True once an investment has been fully paid and its effect applied. */
    public boolean isInvestmentCompleted(String id) { return completedInvestments.contains(id); }

    /** True when the investment can be started right now. */
    public boolean canStartInvestment(String id) {
        return id != null && activeInvestmentId == null
            && !completedInvestments.contains(id) && InvestmentCatalog.get(id) != null;
    }

    /** The ids of every completed investment, in completion order. */
    public List<String> getCompletedInvestments() {
        return new ArrayList<>(completedInvestments);
    }

    /** Permanent per-batch output bonus from completed investments (0.1 = +10%). */
    public float getInvestmentProductionBonus() { return investmentProductionBonus; }

    /** Permanent shared-storage bonus from completed investments (flat capacity). */
    public float getInvestmentStorageBonus() { return investmentStorageBonus; }

    /** Permanent research-cost discount from completed investments (0.1 = −10%). */
    public float getInvestmentResearchDiscount() { return investmentResearchDiscount; }

    /** Permanent working-age share bonus from completed investments (0.05 = +5%). */
    public float getInvestmentWorkforceBonus() { return investmentWorkforceBonus; }

    /** Permanent sale-revenue bonus from completed investments (0.05 = +5%). */
    public float getInvestmentRevenueBonus() { return investmentRevenueBonus; }

    // ── Task 18 development levels ───────────────────────────────────────

    /** One condition's standing toward the next development stage. */
    public static final class DevelopmentConditionStatus {
        public final DevelopmentCondition condition;
        public final float current;
        public final float required;
        public final boolean met;

        DevelopmentConditionStatus(DevelopmentCondition condition, float current, float required) {
            this.condition = condition;
            this.current = current;
            this.required = required;
            this.met = current >= required;
        }
    }

    /**
     * The current value of one development condition. Every condition is a
     * real economy metric; none of them is money in the treasury, so a rich
     * but stagnant settlement cannot advance.
     */
    public float currentValue(DevelopmentCondition condition) {
        switch (condition) {
            case POPULATION:
                return population;
            case PRODUCTION:
                return lifetimeProduced;
            case EMPLOYMENT:
                return workingPopulation > 0f ? employedWorkers / workingPopulation : 0f;
            case INFRASTRUCTURE:
                int activeBuildings = 0;
                for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
                    activeBuildings += getActiveCount(entry.getKey());
                }
                return activeBuildings;
            case REVENUE:
                return lifetimeRevenue;
            case TECHNOLOGY:
                return techLevel;
            case HOUSING:
                return getHousingCapacity();
            case MARKET_ACTIVITY:
                return lifetimeSold;
            default:
                return 0f;
        }
    }

    /** Total housing capacity of the active houses (Task 18 condition). */
    public float getHousingCapacity() {
        float housing = 0f;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0 && profile.getHousing() > 0f) {
                housing += profile.getHousing() * activeCount;
            }
        }
        return housing;
    }

    /** Advances the stage by one when the next level's conditions are met. */
    private void updateDevelopment() {
        if (isMaxDevelopment()) return;
        DevelopmentLevel next = DevelopmentCatalog.get(developmentLevel + 1);
        if (next.isFullyMet(this)) {
            developmentLevel = Math.min(DevelopmentCatalog.maxLevel(), developmentLevel + 1);
        }
    }

    /** The settlement's current development stage, 1 = Small Settlement … 6 = Major Economic Center. */
    public int getDevelopmentLevel() { return developmentLevel; }

    /** i18n key of the current stage's name, e.g. {@code development.village}. */
    public String getDevelopmentNameKey() {
        return DevelopmentCatalog.get(developmentLevel).getNameKey();
    }

    /** True once the settlement reached the final stage. */
    public boolean isMaxDevelopment() { return developmentLevel >= DevelopmentCatalog.maxLevel(); }

    /** Cumulative units produced since the simulation started. */
    public float getLifetimeProduced() { return lifetimeProduced; }

    /** Cumulative units sold to the market since the simulation started. */
    public float getLifetimeSold() { return lifetimeSold; }

    /** Cumulative coin revenue since the simulation started. */
    public float getLifetimeRevenue() { return lifetimeRevenue; }

    /**
     * Overall progress toward the next stage, 0..1 — the average of the eight
     * condition ratios (1 once the final stage is reached).
     */
    public float getDevelopmentProgress() {
        if (isMaxDevelopment()) return 1f;
        DevelopmentLevel next = DevelopmentCatalog.get(developmentLevel + 1);
        float total = 0f;
        int counted = 0;
        for (DevelopmentCondition condition : DevelopmentCondition.values()) {
            float required = next.required(condition);
            if (required > 0f) {
                total += Math.min(1f, currentValue(condition) / required);
                counted++;
            }
        }
        return counted == 0 ? 1f : clamp(total / counted, 0f, 1f);
    }

    /** Every condition's standing toward the next stage, empty at the final stage. */
    public List<DevelopmentConditionStatus> getDevelopmentConditions() {
        List<DevelopmentConditionStatus> result = new ArrayList<>();
        if (isMaxDevelopment()) return result;
        DevelopmentLevel next = DevelopmentCatalog.get(developmentLevel + 1);
        for (DevelopmentCondition condition : DevelopmentCondition.values()) {
            result.add(new DevelopmentConditionStatus(condition,
                currentValue(condition), next.required(condition)));
        }
        return result;
    }

    /**
     * Test/sandbox hook: places the settlement at a given stage directly.
     * Clamped to the valid range 1..6. Package-private on purpose — the live
     * game always advances through the catalog's measurable conditions.
     */
    void setDevelopmentLevel(int level) {
        developmentLevel = Math.max(1, Math.min(DevelopmentCatalog.maxLevel(), level));
    }

    // ── Task 10 building operating costs ─────────────────────────────────

    /**
     * One tick's full operating-cost calculation. The ledger values the
     * in-kind energy/materials used by production on top of the coin costs,
     * so the player can inspect exactly how a tick's revenue is spent.
     */
    public CostLedger getCostLedger() {
        return new CostLedger(revenue, wageCosts, energyCosts, materialsCosts,
            maintenanceCosts, storageCosts, haulingCosts);
    }

    /** Maintenance of the active buildings (paid in coins). */
    public float getMaintenanceCosts() { return maintenanceCosts; }

    /** Coal burnt by production, valued at the current coal price. */
    public float getEnergyCosts() { return energyCosts; }

    /** Inputs consumed by production, valued at their market prices. */
    public float getMaterialsCosts() { return materialsCosts; }

    /**
     * Full economic cost of one tick: wages + energy + materials +
     * maintenance + transport + hauling. Unlike
     * {@link #getOperatingCosts()} it also values the in-kind resources used
     * by production.
     */
    public float getTotalOperatingCosts() {
        return wageCosts + energyCosts + materialsCosts + maintenanceCosts + storageCosts + haulingCosts;
    }

    /** Revenue minus the full economic cost of the tick. */
    public float getNetIncome() {
        return revenue - getTotalOperatingCosts();
    }

    /** Maintenance one active instance of the type pays per tick. */
    public float getBuildingMaintenanceCost(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getOperatingCost() * getActiveCount(typeId);
    }

    /** Wages the type's staffed workers earn this tick. */
    public float getBuildingWageCost(String typeId) {
        return getBuildingEmployedWorkers(typeId) * averageWage;
    }

    /**
     * Recurring coin cost of one building type per tick: its maintenance
     * plus the wages of its staffed workers (energy and materials are
     * per-batch and reported by the settlement ledger).
     */
    public float getBuildingRecurringCosts(String typeId) {
        return getBuildingMaintenanceCost(typeId) + getBuildingWageCost(typeId);
    }
    public float getTotalProfit() { return totalProfit; }
    public float getTotalInvestment() { return totalInvestment; }
    public float getInvestedThisTick() { return investedThisTick; }
    public float getWorkforceDemand() { return workforceDemand; }
    public float getWorkforceAssigned() { return workforceAssigned; }
    public int getTickCount() { return tickCount; }

    // ── Task 9 workforce / employment read-outs ──────────────────────────

    /** Citizens of working age (population × participation rate). */
    public float getWorkingPopulation() { return workingPopulation; }

    /** Workers that could be hired today (= the working population). */
    public float getAvailableWorkers() { return workingPopulation; }

    /** Workers actually hired by active buildings this tick. */
    public float getEmployedWorkers() { return employedWorkers; }

    /** Working-age citizens without a job. */
    public float getUnemployedWorkers() {
        return Math.max(0f, workingPopulation - employedWorkers);
    }

    /** Workers all active buildings ask for (spec: "required workers"). */
    public float getRequiredWorkers() { return workforceDemand; }

    /**
     * Average wage one worker earns per tick. Drifts toward a target that
     * rises with labor scarcity: the more fully the working population is
     * employed, the higher the wage goes.
     */
    public float getAverageWage() { return averageWage; }

    /** Coins paid to the staff this tick (employed × average wage). */
    public float getWageCosts() { return wageCosts; }

    /**
     * Settlement-wide staffing efficiency: how much of the required workforce
     * the working population can cover (e.g. 30 available vs 40 required is
     * 75%, per the Task 9 spec example).
     */
    public float getProductionEfficiency() {
        return workforceDemand <= 0f ? 1f : Math.min(1f, workingPopulation / workforceDemand);
    }

    /** Workers all active instances of a type ask for. */
    public float getBuildingRequiredWorkers(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getWorkforce() * getActiveCount(typeId);
    }

    /** Workers actually hired by the type's instances this tick. */
    public float getBuildingEmployedWorkers(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        if (profile == null) return 0f;
        return profile.getWorkforce() * staffedByType.getOrDefault(typeId, 0);
    }

    /** Staffing efficiency of a single building type (1f = fully staffed). */
    public float getBuildingProductionEfficiency(String typeId) {
        float required = getBuildingRequiredWorkers(typeId);
        if (required <= 0f) return 1f;
        return Math.min(1f, getBuildingEmployedWorkers(typeId) / required);
    }

    public int getPlacedCount(String typeId) {
        return placedCounts.getOrDefault(typeId, 0);
    }

    /** Buildings of the type that have finished construction. */
    public int getActiveCount(String typeId) {
        return Math.max(0, getPlacedCount(typeId) - countSites(typeId));
    }

    public boolean isConstructing(String typeId) {
        return countSites(typeId) > 0;
    }

    public int getConstructionSiteCount() {
        return sites.size();
    }

    // ── Task 8 inventory / warehousing read-outs ─────────────────────────

    /** Total shared storage: base 200 plus active warehouses' capacity, scaled by storage efficiency. */
    public float getStorageCapacity() {
        float storageFactor = TechnologyCatalog.get(techLevel).storageFactor();
        float capacity = BASE_STORAGE_CAPACITY;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0 && profile.isStorage()) {
                capacity += profile.getInventoryCapacity() * activeCount * storageFactor;
            }
        }
        return capacity + investmentStorageBonus;
    }

    public float getStoredInventory(Resource resource) { return settlement.getStored(resource); }
    public float getReservedInventory(Resource resource) { return settlement.getReserved(resource); }
    public float getReserved(Resource resource) { return settlement.getReserved(resource); }
    public float getStoredInventoryTotal() { return settlement.getStoredTotal(); }
    public float getReservedInventoryTotal() { return settlement.getReservedTotal(); }

    /**
     * Shared storage still free this tick: total capacity minus what is stored
     * and reserved (money excluded, it never sits in warehouses).
     */
    public float getAvailableStorage() {
        return settlement.getCapacity() - nonMoneyStored() - nonMoneyReserved();
    }

    public float getBuildingStored(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getStored(resource);
    }

    public float getBuildingReserved(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getReserved(resource);
    }

    public float getBuildingIncoming(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getIncoming(resource);
    }

    public float getBuildingConsumed(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getConsumed(resource);
    }

    public float getBuildingProduced(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getProduced(resource);
    }

    public float getBuildingOutgoing(String typeId, Resource resource) {
        ResourceInventory inv = buildingInventories.get(typeId);
        return inv == null ? 0f : inv.getOutgoing(resource);
    }

    public float getWarehouseCapacity(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getInventoryCapacity();
    }

    public float getWarehouseStorageCostPerUnit(String typeId) {
        BuildingEconomy profile = EconomyData.get(typeId);
        return profile == null ? 0f : profile.getStorageCostPerUnit();
    }

    public float getWarehouseActiveCount(String typeId) {
        return getActiveCount(typeId);
    }

    /** Snapshot for persistence. */
    public EconomyState toState() {
        EconomyState state = new EconomyState();
        state.money = settlement.getStored(Resource.COINS);
        state.population = population;
        state.averageWage = averageWage;
        state.techLevel = techLevel;
        state.researchRemainingCost = researchRemainingCost;
        state.researchRemainingTicks = researchRemainingTicks;
        state.activeInvestmentId = activeInvestmentId;
        state.investmentRemainingCost = investmentRemainingCost;
        for (String id : completedInvestments) state.completedInvestments.add(id);
        state.developmentLevel = developmentLevel;
        state.lifetimeProduced = lifetimeProduced;
        state.lifetimeSold = lifetimeSold;
        state.lifetimeRevenue = lifetimeRevenue;
        for (Resource resource : Resource.values()) {
            float amount = settlement.getStored(resource);
            if (amount > 0f) {
                EconomyState.StockState stock = new EconomyState.StockState();
                stock.goodId = resource.getId();
                stock.amount = amount;
                state.inventory.add(stock);
            }
        }
        for (ConstructionSite site : sites) {
            EconomyState.ConstructionState construction = new EconomyState.ConstructionState();
            construction.typeId = site.typeId;
            construction.remainingCost = site.remainingCost;
            construction.remainingTicks = site.remainingTicks;
            state.construction.add(construction);
        }
        return state;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float sum(float[] values) {
        float total = 0f;
        for (float value : values) total += value;
        return total;
    }
}
