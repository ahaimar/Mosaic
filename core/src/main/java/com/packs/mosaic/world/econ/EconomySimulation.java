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
 * <p>Flow of a single tick:
 *
 * <pre>
 *   Construction → Workforce (hire + wages) → Production (reserve + batches)
 *                  ↓
 *     Routes (dispatch + arrivals) → Ship-out → Inventory
 *                  ↓
 *          Consumption → Market → Sales → Revenue
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

        // Active buildings = placed buildings whose construction has finished.
        TreeMap<String, Integer> active = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            int activeCount = entry.getValue() - countSites(entry.getKey());
            if (activeCount > 0) active.put(entry.getKey(), activeCount);
        }

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
        workingPopulation = population * WORKFORCE_PARTICIPATION;
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
        // building's inventory capacity.
        settlement.setCapacity(settlementCapacityFor(active));

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

            ResourceInventory inv = buildingInventory(entry.getKey(), profile, staffable);

            int slots = batchSlots.getOrDefault(entry.getKey(), 0);
            while (slots < staffable) {
                // Storage full → production limited: never start a batch when
                // the factory's own stockpile cannot hold the output.
                if (inv.getCapacity() > 0f && inv.getStoredTotal() >= inv.getCapacity()) {
                    break;
                }
                boolean canReserve = true;
                for (Resource input : Resource.values()) {
                    float need = profile.getInput(input);
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
                    float need = profile.getInput(input);
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
            progress += slots * profile.getProductionCapacity();
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
                    inv.produceInto(output, outputRate);
                    produced[output.ordinal()] += outputRate;
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
                    float capacity = trucksOnRoute * truckCapacity;
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
            revenue += volume * price[resource.ordinal()] * marketMultiplier;
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

    private float settlementCapacityFor(Map<String, Integer> active) {
        float capacity = BASE_STORAGE_CAPACITY;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            if (profile != null && profile.isStorage()) {
                capacity += profile.getInventoryCapacity() * entry.getValue();
            }
        }
        return capacity;
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

    private ResourceInventory buildingInventory(String typeId, BuildingEconomy profile, int staffable) {
        ResourceInventory inv = buildingInventories.get(typeId);
        if (inv == null) {
            inv = new ResourceInventory(0f, 0f);
            buildingInventories.put(typeId, inv);
        }
        inv.setCapacity(profile.getInventoryCapacity() * staffable);
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

    /** Units one trip of the route can carry today (assigned trucks × capacity). */
    public float getRouteCapacity(String sourceTypeId, Resource resource) {
        DeliveryRoute route = findRoute(sourceTypeId, resource);
        if (route == null) return 0f;
        return route.effectiveTrucks(fleetTrucks()) * truckCapacity();
    }

    /** Number of delivery routes currently serving the settlement. */
    public int getActiveRouteCount() { return routes.size(); }

    /** Units of every routed good currently travelling to the settlement. */
    public float getTotalInTransit() {
        float total = 0f;
        for (DeliveryRoute route : routes) total += route.inTransit();
        return total;
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

    /** Total shared storage: base 200 plus active warehouses' capacity. */
    public float getStorageCapacity() {
        float capacity = BASE_STORAGE_CAPACITY;
        for (Map.Entry<String, Integer> entry : placedCounts.entrySet()) {
            BuildingEconomy profile = EconomyData.get(entry.getKey());
            int activeCount = getActiveCount(entry.getKey());
            if (profile != null && activeCount > 0 && profile.isStorage()) {
                capacity += profile.getInventoryCapacity() * activeCount;
            }
        }
        return capacity;
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
}
