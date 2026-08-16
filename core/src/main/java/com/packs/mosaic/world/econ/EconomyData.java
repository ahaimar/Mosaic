package com.packs.mosaic.world.econ;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central, data-driven economic table (Task 5/6/7/8). Every economic fact about a
 * building — production (inputs, output per batch, production time, energy,
 * capacity), workforce, operating cost, housing, storage (shared inventory
 * capacity and per-unit storage fee), market bonus, construction
 * cost/duration, transport (trucks, speed, roads, delivery staging) and
 * energy (grid power produced and consumed) — lives here, keyed by building
 * id. Individual building types (BuildingType) know nothing about the
 * economy; the simulation reads only this table.
 *
 * Resource facts (base price, storage limit, category, unit) live on the
 * resources themselves ({@link Resource}); this table only wires resources
 * to buildings.
 *
 * Buildings without an entry are economically neutral (nothing produced,
 * nothing consumed, no upkeep).
 */
public final class EconomyData {

    private static final Map<String, BuildingEconomy> BY_ID = new LinkedHashMap<>();
    private static final List<BuildingEconomy> ALL = new ArrayList<>();

    static {
        // ── Producers (raw materials) ─────────────────────────────────────
        // Free starter nature: low yield, no construction, cheap upkeep.
        producer("tree", 1f, 0.05f, 0f, 0f, Resource.WOOD, 0.5f);
        producer("rock", 1f, 0.05f, 0f, 0f, Resource.STONE, 0.5f);
        producer("garden", 1f, 0.05f, 0f, 0f, Resource.FOOD, 0.5f);
        // Paid economic buildings: high yield, constructed over time.
        producer("lumber_hut", 1f, 0.2f, 25f, 3f, Resource.WOOD, 1f);
        producer("stone_mine", 1f, 0.2f, 30f, 3f, Resource.STONE, 1f);
        producer("iron_mine", 1f, 0.25f, 35f, 4f, Resource.IRON, 1f);
        producer("coal_mine", 1f, 0.25f, 35f, 4f, Resource.COAL, 1f);
        producer("farm", 1f, 0.2f, 25f, 3f, Resource.FOOD, 1f);
        producer("dairy", 1f, 0.25f, 30f, 3f, Resource.MILK, 1f);
        producer("coop", 1f, 0.2f, 25f, 3f, Resource.EGGS, 1f);
        producer("ranch", 1f, 0.25f, 30f, 3f, Resource.WOOL, 1f);

        // ── Converters: consume inputs + energy, produce refined resources.
        // Task 7 production-batch fields: output per batch, production time in
        // ticks, energy (COAL) per batch, and capacity in batches per tick.
        // Task 14: every converter also draws grid energy (energyConsumed),
        // so a factory without power runs at reduced efficiency.
        converter("workshop", single(Resource.WOOD, 1f), Resource.TOOLS, 1f,
            1f, 0f, 1f, 1f, 0.3f, 35f, 4f, 1f);
        converter("smelter", single(Resource.IRON, 1f), Resource.STEEL, 1f,
            2f, 1f, 1f, 1f, 0.4f, 40f, 4f, 2f);
        converter("carpentry", twoInputs(Resource.WOOD, 1f, Resource.STEEL, 1f), Resource.FURNITURE, 1f,
            1f, 0f, 1f, 1f, 0.4f, 40f, 4f, 2f);
        // The Task 7 chain: iron → steel → components → finished product.
        converter("machine_factory", single(Resource.STEEL, 1f), Resource.TOOLS, 1f,
            2f, 1f, 1f, 1f, 0.4f, 45f, 5f, 3f);
        converter("assembly_factory", single(Resource.TOOLS, 1f), Resource.FURNITURE, 1f,
            3f, 1f, 1f, 1f, 0.5f, 50f, 5f, 3f);

        // ── Market buildings: raise the price their goods sell for ─────────
        market("shop", 1f, 0.8f, 0.5f, 40f, 3f, 1f);
        market("market_street", 1f, 1.0f, 0.75f, 60f, 4f, 1f);
        market("grand_market", 2f, 1.5f, 1.0f, 80f, 5f, 2f);

        // ── Infrastructure: storage capacity ──────────────────────────────
        // central_station: legacy per-resource bonus (30) plus a large shared
        // inventory capacity with no storage fee.
        storage("central_station", 1f, 1.0f, 30f, 100f, 0f, 60f, 5f, 2f);

        // Task 8 warehouses: no per-resource bonus, but they add their
        // inventoryCapacity to the settlement's shared storage and charge a
        // storage fee per stored unit per tick.
        warehouse("warehouse", 1f, 0.3f, 100f, 0.02f, 60f, 4f, 1f);
        warehouse("large_warehouse", 1f, 0.4f, 250f, 0.015f, 80f, 5f, 2f);

        // ── Transport & logistics (Task 13) ────────────────────────────────
        // truck_depot: the settlement delivery fleet. Each active depot adds
        // trucks and a fleet-wide bonus on every truck's capacity and speed.
        transport("truck_depot", 1f, 0.2f, 3f, 0.5f, 0.5f, 40f, 4f, 2f);
        // Roads: every active road shortens the travel time of delivery trips.
        road("dirt_road", 0.2f, 0.05f, 10f, 2f);
        road("cobbled_road", 0.5f, 0.1f, 15f, 2f);

        // ── Energy production (Task 14) ────────────────────────────────────
        // The four power plants feed the settlement's energy network; every
        // factory, shop, warehouse, infrastructure and research facility that
        // draws power slows down when the grid runs short.
        energyProducer("generator", 1f, 0.2f, 3f, 20f, 2f);
        energyProducer("power_plant", 2f, 0.5f, 8f, 60f, 5f);
        energyProducer("solar_plant", 1f, 0.15f, 5f, 50f, 4f);
        energyProducer("advanced_power_plant", 3f, 0.8f, 15f, 90f, 7f);

        // ── Research facilities (Task 14) ──────────────────────────────────
        // The university is a research facility: it draws grid power like any
        // other consumer, but its output is research (not a resource), so it
        // only pays wages and maintenance.
        register(new BuildingEconomy("university", null, null, 1f, 0.3f, 0f, 0f, 0f, 60f, 5f,
            1f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 2f));

        // ── Housing: population capacity ───────────────────────────────────
        house("small_house", 2f);
        house("large_house", 4f);
        house("cozy_cottage", 3f);
        house("cabin", 3f);
        house("igloo", 2f);
        house("alpine_hut", 2f);
        house("tiki_hut", 2f);
        house("lifeguard_hut", 2f);
        house("ski_lodge", 3f);
        house("small_town", 8f);
        house("village_center", 4f);
        house("grand_town_hall", 6f);
        house("megalopolis_tower", 10f);
    }

    private EconomyData() {
    }

    public static BuildingEconomy get(String buildingId) {
        return BY_ID.get(buildingId);
    }

    /** Every profile, in registration order. */
    public static List<BuildingEconomy> getAll() {
        return ALL;
    }

    private static void register(BuildingEconomy economy) {
        BY_ID.put(economy.getBuildingId(), economy);
        ALL.add(economy);
    }

    private static void producer(String id, float workforce, float operatingCost,
                                 float constructionCost, float constructionTicks,
                                 Resource output, float rate) {
        register(new BuildingEconomy(id, null, single(output, rate),
            workforce, operatingCost, 0f, 0f, 0f, constructionCost, constructionTicks,
            1f, 0f, 1f, 20f, 0f));
    }

    private static void converter(String id, Map<Resource, Float> inputs,
                                  Resource output, float outputPerBatch,
                                  float productionTime, float energyRequired, float capacity,
                                  float workforce, float operatingCost,
                                  float constructionCost, float constructionTicks,
                                  float energyConsumed) {
        Map<Resource, Float> out = new EnumMap<>(Resource.class);
        out.put(output, outputPerBatch);
        register(new BuildingEconomy(id, inputs, out, workforce, operatingCost, 0f, 0f, 0f,
            constructionCost, constructionTicks, productionTime, energyRequired, capacity,
            20f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, energyConsumed));
    }

    private static void market(String id, float workforce, float operatingCost,
                               float marketBonus, float constructionCost, float constructionTicks,
                               float energyConsumed) {
        register(new BuildingEconomy(id, null, null, workforce, operatingCost, 0f, 0f, marketBonus,
            constructionCost, constructionTicks, 1f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f, 0f, energyConsumed));
    }

    private static void storage(String id, float workforce, float operatingCost,
                                float storageBonus, float inventoryCapacity, float storageCostPerUnit,
                                float constructionCost, float constructionTicks,
                                float energyConsumed) {
        register(new BuildingEconomy(id, null, null, workforce, operatingCost, 0f, storageBonus, 0f,
            constructionCost, constructionTicks, 1f, 0f, 1f, inventoryCapacity, storageCostPerUnit,
            0f, 0f, 0f, 0f, 0f, 0f, energyConsumed));
    }

    private static void warehouse(String id, float workforce, float operatingCost,
                                  float inventoryCapacity, float storageCostPerUnit,
                                  float constructionCost, float constructionTicks,
                                  float energyConsumed) {
        // A warehouse also stages deliveries: each one lets one extra truck
        // trip start per tick (staging bonus).
        register(new BuildingEconomy(id, null, null, workforce, operatingCost, 0f, 0f, 0f,
            constructionCost, constructionTicks, 1f, 0f, 1f, inventoryCapacity, storageCostPerUnit,
            0f, 0f, 0f, 0f, 1f, 0f, energyConsumed));
    }

    private static void transport(String id, float workforce, float operatingCost,
                                  float trucksProvided, float truckCapacityBonus,
                                  float truckSpeedBonus,
                                  float constructionCost, float constructionTicks,
                                  float energyConsumed) {
        register(new BuildingEconomy(id, null, null, workforce, operatingCost, 0f, 0f, 0f,
            constructionCost, constructionTicks, 1f, 0f, 1f, 0f, 0f,
            trucksProvided, truckCapacityBonus, truckSpeedBonus, 0f, 0f, 0f, energyConsumed));
    }

    private static void energyProducer(String id, float workforce, float operatingCost,
                                       float energyProduced,
                                       float constructionCost, float constructionTicks) {
        register(new BuildingEconomy(id, null, null, workforce, operatingCost, 0f, 0f, 0f,
            constructionCost, constructionTicks, 1f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f, energyProduced, 0f));
    }

    private static void road(String id, float roadBonus, float operatingCost,
                             float constructionCost, float constructionTicks) {
        register(new BuildingEconomy(id, null, null, 0f, operatingCost, 0f, 0f, 0f,
            constructionCost, constructionTicks, 1f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, roadBonus, 0f));
    }

    private static void house(String id, float housing) {
        register(new BuildingEconomy(id, null, null, 0f, 0f, housing, 0f, 0f, 0f, 0f));
    }

    private static Map<Resource, Float> single(Resource resource, float rate) {
        Map<Resource, Float> map = new EnumMap<>(Resource.class);
        map.put(resource, rate);
        return map;
    }

    private static Map<Resource, Float> twoInputs(Resource a, float ra, Resource b, float rb) {
        Map<Resource, Float> map = new EnumMap<>(Resource.class);
        map.put(a, ra);
        map.put(b, rb);
        return map;
    }
}
