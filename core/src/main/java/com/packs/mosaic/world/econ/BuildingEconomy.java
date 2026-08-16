package com.packs.mosaic.world.econ;

import java.util.EnumMap;
import java.util.Map;

/**
 * The economic profile of one building type (Task 5, extended by Task 7).
 * This is a plain data record — the *economic results* of a building are
 * never hardcoded into the building itself (BuildingType stays purely
 * visual/placement). Instead every number that matters to the simulation
 * lives here, in one central data table ({@link EconomyData}).
 *
 * Task 7 (production chain logic) adds the full production-batch fields a
 * factory defines: inputs consumed per batch, production time (ticks per
 * batch), output per batch, workers required, energy required (COAL per
 * batch), operating cost and production capacity (max batches per tick).
 *
 * Task 8 (inventory and warehousing) adds the storage fields: a factory
 * keeps its own small stockpile (inventoryCapacity per staffed worker), a
 * warehouse contributes its inventoryCapacity to the settlement's total
 * storage and charges storageCostPerUnit per stored unit per tick.
 *
 * Task 13 (transportation and logistics) adds the transport fields: a truck
 * depot provides vehicles (trucksProvided) plus fleet-wide capacity and speed
 * bonuses, a road shortens every trip (roadBonus), and a warehouse raises the
 * dispatch budget (stagingBonus) so more delivery trips can be started each
 * tick. A building type without a profile is economically neutral: it
 * produces, consumes and costs nothing.
 *
 * Task 14 (energy system) adds the grid fields: a power producer generates
 * energy for the settlement's network (energyProduced per tick per instance),
 * and a consumer draws from it (energyConsumed per tick per instance). When
 * the grid is short the producing consumers run at reduced efficiency.
 */
public final class BuildingEconomy {

    private final String buildingId;
    private final Map<Resource, Float> inputs;
    private final Map<Resource, Float> outputs;
    private final float workforce;
    private final float operatingCost;
    private final float housing;
    private final float storageBonus;
    private final float marketBonus;
    private final float constructionCost;
    private final float constructionTicks;

    // ── Task 7 production-batch fields ───────────────────────────────────
    private final float productionTime;
    private final float energyRequired;
    private final float productionCapacity;

    // ── Task 8 inventory/warehousing fields ──────────────────────────────
    private final float inventoryCapacity;
    private final float storageCostPerUnit;

    // ── Task 13 transport/logistics fields ───────────────────────────────
    private final float trucksProvided;
    private final float truckCapacityBonus;
    private final float truckSpeedBonus;
    private final float roadBonus;
    private final float stagingBonus;

    // ── Task 14 energy network fields ────────────────────────────────────
    private final float energyProduced;
    private final float energyConsumed;

    public BuildingEconomy(String buildingId, Map<Resource, Float> inputs, Map<Resource, Float> outputs,
                           float workforce, float operatingCost, float housing,
                           float storageBonus, float marketBonus,
                           float constructionCost, float constructionTicks) {
        this(buildingId, inputs, outputs, workforce, operatingCost, housing,
            storageBonus, marketBonus, constructionCost, constructionTicks,
            1f, 0f, 1f, 0f, 0f);
    }

    public BuildingEconomy(String buildingId, Map<Resource, Float> inputs, Map<Resource, Float> outputs,
                           float workforce, float operatingCost, float housing,
                           float storageBonus, float marketBonus,
                           float constructionCost, float constructionTicks,
                           float productionTime, float energyRequired, float productionCapacity) {
        this(buildingId, inputs, outputs, workforce, operatingCost, housing,
            storageBonus, marketBonus, constructionCost, constructionTicks,
            productionTime, energyRequired, productionCapacity, 0f, 0f);
    }

    public BuildingEconomy(String buildingId, Map<Resource, Float> inputs, Map<Resource, Float> outputs,
                           float workforce, float operatingCost, float housing,
                           float storageBonus, float marketBonus,
                           float constructionCost, float constructionTicks,
                           float productionTime, float energyRequired, float productionCapacity,
                           float inventoryCapacity, float storageCostPerUnit) {
        this(buildingId, inputs, outputs, workforce, operatingCost, housing,
            storageBonus, marketBonus, constructionCost, constructionTicks,
            productionTime, energyRequired, productionCapacity,
            inventoryCapacity, storageCostPerUnit,
            0f, 0f, 0f, 0f, 0f);
    }

    public BuildingEconomy(String buildingId, Map<Resource, Float> inputs, Map<Resource, Float> outputs,
                           float workforce, float operatingCost, float housing,
                           float storageBonus, float marketBonus,
                           float constructionCost, float constructionTicks,
                           float productionTime, float energyRequired, float productionCapacity,
                           float inventoryCapacity, float storageCostPerUnit,
                           float trucksProvided, float truckCapacityBonus, float truckSpeedBonus,
                           float roadBonus, float stagingBonus) {
        this(buildingId, inputs, outputs, workforce, operatingCost, housing,
            storageBonus, marketBonus, constructionCost, constructionTicks,
            productionTime, energyRequired, productionCapacity,
            inventoryCapacity, storageCostPerUnit,
            trucksProvided, truckCapacityBonus, truckSpeedBonus, roadBonus, stagingBonus,
            0f, 0f);
    }

    public BuildingEconomy(String buildingId, Map<Resource, Float> inputs, Map<Resource, Float> outputs,
                           float workforce, float operatingCost, float housing,
                           float storageBonus, float marketBonus,
                           float constructionCost, float constructionTicks,
                           float productionTime, float energyRequired, float productionCapacity,
                           float inventoryCapacity, float storageCostPerUnit,
                           float trucksProvided, float truckCapacityBonus, float truckSpeedBonus,
                           float roadBonus, float stagingBonus,
                           float energyProduced, float energyConsumed) {
        this.buildingId = buildingId;
        this.inputs = new EnumMap<>(Resource.class);
        if (inputs != null) this.inputs.putAll(inputs);
        this.outputs = new EnumMap<>(Resource.class);
        if (outputs != null) this.outputs.putAll(outputs);
        this.workforce = workforce;
        this.operatingCost = operatingCost;
        this.housing = housing;
        this.storageBonus = storageBonus;
        this.marketBonus = marketBonus;
        this.constructionCost = constructionCost;
        this.constructionTicks = constructionTicks;
        this.productionTime = Math.max(1f, productionTime);
        this.energyRequired = Math.max(0f, energyRequired);
        this.productionCapacity = Math.max(0f, productionCapacity);
        this.inventoryCapacity = Math.max(0f, inventoryCapacity);
        this.storageCostPerUnit = Math.max(0f, storageCostPerUnit);
        this.trucksProvided = Math.max(0f, trucksProvided);
        this.truckCapacityBonus = Math.max(0f, truckCapacityBonus);
        this.truckSpeedBonus = Math.max(0f, truckSpeedBonus);
        this.roadBonus = Math.max(0f, roadBonus);
        this.stagingBonus = Math.max(0f, stagingBonus);
        this.energyProduced = Math.max(0f, energyProduced);
        this.energyConsumed = Math.max(0f, energyConsumed);
    }

    public String getBuildingId() { return buildingId; }

    /** Units of a resource this building consumes per batch (0 if none). */
    public float getInput(Resource resource) {
        Float value = inputs.get(resource);
        return value == null ? 0f : value;
    }

    /** Units of a resource this building produces per batch (0 if none). */
    public float getOutput(Resource resource) {
        Float value = outputs.get(resource);
        return value == null ? 0f : value;
    }

    public boolean isProducer() {
        return !outputs.isEmpty();
    }

    public boolean isConsumer() {
        return !inputs.isEmpty() || energyRequired > 0f || energyConsumed > 0f;
    }

    /** Workers required to run one instance of this building. */
    public float getWorkforce() { return workforce; }

    /** Coins spent every tick per active instance (maintenance). */
    public float getOperatingCost() { return operatingCost; }

    /** Population slots one instance provides. */
    public float getHousing() { return housing; }

    /** Extra storage capacity (in units of any resource) one instance provides. */
    public float getStorageBonus() { return storageBonus; }

    /** Extra multiplier on export prices (0 = none; 0.5 = +50% sale value). */
    public float getMarketBonus() { return marketBonus; }

    /** Total coins that must be invested before a new instance is built. */
    public float getConstructionCost() { return constructionCost; }

    /** Ticks a fully-funded construction site needs to finish. */
    public float getConstructionTicks() { return constructionTicks; }

    public boolean hasConstruction() {
        return constructionCost > 0f;
    }

    // ── Task 7 production batch ──────────────────────────────────────────

    /** Ticks required to complete one production batch. At least 1. */
    public float getProductionTime() { return productionTime; }

    /**
     * Energy (COAL) units consumed per completed batch. A building whose
     * energy requirement is not covered cannot produce.
     */
    public float getEnergyRequired() { return energyRequired; }

    /** Maximum batches one instance can complete per tick (throughput). */
    public float getProductionCapacity() { return productionCapacity; }

    // ── Task 8 inventory/warehousing ─────────────────────────────────────

    /**
     * Units of storage one staffed worker of this building provides. For a
     * factory it sizes the on-site output stockpile; for a warehouse it is
     * the capacity contributed to the settlement's shared storage.
     */
    public float getInventoryCapacity() { return inventoryCapacity; }

    /** Coins charged per stored non-money unit per tick for this building. */
    public float getStorageCostPerUnit() { return storageCostPerUnit; }

    /**
     * A storage building extends the settlement's shared capacity: a
     * warehouse, or any profile that still carries the legacy per-resource
     * storage bonus (e.g. the central station).
     */
    public boolean isStorage() {
        return !isProducer() && (storageBonus > 0f || inventoryCapacity > 0f);
    }

    // ── Task 13 transport / logistics ────────────────────────────────────

    /** Delivery trucks one active truck depot contributes to the fleet. */
    public float getTrucksProvided() { return trucksProvided; }

    /** Fractional bonus on every truck's per-trip capacity (0 = none). */
    public float getTruckCapacityBonus() { return truckCapacityBonus; }

    /** Fractional bonus on every truck's travel speed (0 = none). */
    public float getTruckSpeedBonus() { return truckSpeedBonus; }

    /** Fractional cut of every delivery trip's travel time (0 = none). */
    public float getRoadBonus() { return roadBonus; }

    /** Extra delivery trips one active warehouse can stage per tick. */
    public float getStagingBonus() { return stagingBonus; }

    // ── Task 14 energy network ───────────────────────────────────────────

    /** Grid energy one active instance feeds the settlement network per tick. */
    public float getEnergyProduced() { return energyProduced; }

    /** Grid energy one active instance draws from the network per tick. */
    public float getEnergyConsumed() { return energyConsumed; }

    /** A building type that feeds the settlement's energy network. */
    public boolean isEnergyProducer() {
        return energyProduced > 0f;
    }

    /** A building type that draws from the settlement's energy network. */
    public boolean isEnergyConsumer() {
        return energyConsumed > 0f;
    }
}
