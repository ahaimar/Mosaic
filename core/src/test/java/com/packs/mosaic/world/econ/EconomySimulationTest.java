package com.packs.mosaic.world.econ;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 5 — Economic Simulation Core, extended by the Task 6 resource system.
 * The simulation is fully headless and deterministic: it has no randomness
 * and no graphics, so every step of the production → market → revenue →
 * costs/profit → investment chain can be verified directly. Task 6 turns
 * money into an ordinary resource (COINS), gives every resource its own
 * category, base price, unit and storage limit, and adds the raw/industrial/
 * finished goods chain (iron/coal → steel → furniture).
 */
@DisplayName("EconomySimulation — deterministic tick engine + resource system")
class EconomySimulationTest {

    private static final float EPS = 0.001f;

    @Test
    void dataTablePresentsEveryResourceAndBuildingProfile() {
        for (Resource resource : Resource.values()) {
            assertTrue(resource.getBasePrice() > 0f, resource.getId() + " has a base price");
        }
        for (String producerId : new String[]{"tree", "rock", "garden", "lumber_hut", "stone_mine",
            "iron_mine", "coal_mine", "farm", "dairy", "coop", "ranch",
            "machine_factory", "assembly_factory"}) {
            BuildingEconomy profile = EconomyData.get(producerId);
            assertNotNull(profile, producerId + " has a profile");
            assertTrue(profile.isProducer(), producerId + " produces");
        }
        BuildingEconomy workshop = EconomyData.get("workshop");
        assertEquals(1f, workshop.getInput(Resource.WOOD), EPS);
        assertEquals(1f, workshop.getOutput(Resource.TOOLS), EPS);
        assertEquals(1f, workshop.getProductionTime(), EPS);
        assertEquals(0f, workshop.getEnergyRequired(), EPS);
        assertEquals(1f, workshop.getProductionCapacity(), EPS);
        BuildingEconomy smelter = EconomyData.get("smelter");
        assertEquals(1f, smelter.getInput(Resource.IRON), EPS);
        assertEquals(0f, smelter.getInput(Resource.COAL), EPS,
            "coal is energy for the smelter, not a raw input");
        assertEquals(1f, smelter.getEnergyRequired(), EPS);
        assertEquals(2f, smelter.getProductionTime(), EPS);
        assertEquals(1f, smelter.getOutput(Resource.STEEL), EPS);
        BuildingEconomy machine = EconomyData.get("machine_factory");
        assertEquals(1f, machine.getInput(Resource.STEEL), EPS);
        assertEquals(1f, machine.getEnergyRequired(), EPS);
        assertEquals(2f, machine.getProductionTime(), EPS);
        assertEquals(1f, machine.getOutput(Resource.TOOLS), EPS);
        BuildingEconomy assembly = EconomyData.get("assembly_factory");
        assertEquals(1f, assembly.getInput(Resource.TOOLS), EPS);
        assertEquals(1f, assembly.getEnergyRequired(), EPS);
        assertEquals(3f, assembly.getProductionTime(), EPS);
        assertEquals(1f, assembly.getOutput(Resource.FURNITURE), EPS);
        BuildingEconomy carpentry = EconomyData.get("carpentry");
        assertEquals(1f, carpentry.getInput(Resource.WOOD), EPS);
        assertEquals(1f, carpentry.getInput(Resource.STEEL), EPS);
        assertEquals(1f, carpentry.getOutput(Resource.FURNITURE), EPS);
        assertEquals(0.5f, EconomyData.get("shop").getMarketBonus(), EPS);
        assertEquals(1f, EconomyData.get("grand_market").getMarketBonus(), EPS);
        assertEquals(30f, EconomyData.get("central_station").getStorageBonus(), EPS);
        assertEquals(2f, EconomyData.get("small_house").getHousing(), EPS);
        assertEquals(10f, EconomyData.get("megalopolis_tower").getHousing(), EPS);
        assertTrue(EconomyData.get("lumber_hut").hasConstruction());
        assertFalse(EconomyData.get("tree").hasConstruction());
        assertNull(EconomyData.get("road_straight"), "neutral types have no profile");
    }

    @Test
    void everyResourceCarriesTheFullSpecData() {
        for (Resource resource : Resource.values()) {
            assertNotNull(resource.getId());
            assertFalse(resource.getId().isEmpty(), "id");
            assertNotNull(resource.getNameKey());
            assertFalse(resource.getNameKey().isEmpty(), "name");
            assertNotNull(resource.getCategory(), "category");
            assertTrue(resource.getBasePrice() > 0f, "base_price");
            assertNotNull(resource.getUnit());
            assertFalse(resource.getUnit().isEmpty(), "unit");
            assertTrue(resource.getStorageLimit() > 0f, "storage_limit");
            assertTrue(resource.getConsumerPreference() >= 0f, "consumer_preference");
        }
        assertEquals(0f, Resource.COINS.getConsumerPreference(), EPS, "money is not a consumer good");
        assertEquals(8, Resource.Category.values().length, "the eight spec categories");
        assertEquals(Resource.Category.RAW_MATERIALS, Resource.WOOD.getCategory());
        assertEquals(Resource.Category.COMPONENTS, Resource.TOOLS.getCategory());
        assertEquals(Resource.Category.ENERGY, Resource.COAL.getCategory());
        assertEquals(Resource.Category.FOOD, Resource.FOOD.getCategory());
        assertEquals(Resource.Category.CONSUMER_GOODS, Resource.MILK.getCategory());
        assertEquals(Resource.Category.INDUSTRIAL_GOODS, Resource.STEEL.getCategory());
        assertEquals(Resource.Category.FINISHED_PRODUCTS, Resource.FURNITURE.getCategory());
        assertEquals(Resource.Category.MONEY, Resource.COINS.getCategory());
        assertEquals(Resource.COINS, Resource.byId("coins"));
        assertNull(Resource.byId("does_not_exist"));
    }

    @Test
    void moneyIsARealResource() {
        EconomySimulation sim = new EconomySimulation();
        assertEquals(sim.getMoney(), sim.getInventory(Resource.COINS), EPS);
        assertEquals(Resource.COINS.getBasePrice(), sim.getPrice(Resource.COINS), EPS);
        assertTrue(Resource.COINS.isMoney());
        assertEquals(100f, sim.getMoney(), EPS, "fresh simulation starts with the coin grant");
    }

    @Test
    void moneyIsNeverExportedOrPriced() {
        // Wood starts exactly at its 60-unit cap, so the very first tick makes
        // a surplus that must sell — and the sale revenue accrues to the COINS
        // treasury while COINS itself is never exported.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.WOOD, 60f));
        sim.addBuilding("lumber_hut", true);
        float before = sim.getMoney();
        sim.tick();
        assertEquals(0f, sim.getSold(Resource.COINS), EPS, "coins are never exported");
        assertEquals(Resource.COINS.getBasePrice(), sim.getPrice(Resource.COINS), EPS,
            "the money price never drifts on the market");
        assertEquals(before + sim.getRevenue() - sim.getOperatingCosts(), sim.getMoney(), EPS,
            "sales revenue flows straight into the treasury");
        assertTrue(sim.getRevenue() > 0f, "the surplus wood actually sold");
    }

    @Test
    void idleSimulationIsStableWithoutBuildings() {
        // Enough food to last the window (40 - 0.75*50 = 2.5 left) but below
        // food's 40-unit storage limit, so nothing is exported and nothing starves.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.FOOD, 40f));
        for (int i = 0; i < 50; i++) {
            sim.tick();
        }
        assertEquals(100f, sim.getMoney(), EPS, "no buildings, no income, no upkeep");
        assertEquals(3f, sim.getPopulation(), EPS, "fed population neither grows nor shrinks");
        assertEquals(2.5f, sim.getInventory(Resource.FOOD), EPS, "food consumed exactly 0.75/tick");
        for (Resource resource : Resource.values()) {
            assertFalse(Float.isNaN(sim.getPrice(resource)));
            assertFalse(Float.isNaN(sim.getInventory(resource)));
        }
    }

    @Test
    void producerCreatesInventory() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("lumber_hut", true);
        sim.tick();
        assertEquals(1f, sim.getProduced(Resource.WOOD), EPS);
        assertEquals(11f, sim.getInventory(Resource.WOOD), EPS);
        assertEquals(1f, sim.getWorkforceDemand(), EPS);
        assertEquals(1f, sim.getWorkforceAssigned(), EPS);
    }

    @Test
    void workforceLimitsProduction() {
        EconomySimulation sim = new EconomySimulation(state(0f, 100f));
        sim.addBuilding("lumber_hut", true);
        sim.tick();
        assertEquals(0f, sim.getProduced(Resource.WOOD), EPS);
        assertEquals(1f, sim.getWorkforceDemand(), EPS);
        assertEquals(0f, sim.getWorkforceAssigned(), EPS);
    }

    @Test
    void populationGrowsWithHousingAndFood() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("small_house", true);
        sim.addBuilding("small_house", true);
        sim.tick();
        assertEquals(3.1f, sim.getPopulation(), EPS);
    }

    @Test
    void starvationShrinksPopulation() {
        EconomySimulation sim = new EconomySimulation(state(3f, 100f));
        sim.tick();
        assertEquals(2.8125f, sim.getPopulation(), EPS);
        assertTrue(sim.getPopulation() < 3f);
    }

    @Test
    void surplusIsExportedForRevenue() {
        // Two farms feed a population of six (working population 3) for the
        // whole window, so the lumber hut keeps its worker and the wood surplus
        // keeps selling. That sale revenue books straight into the treasury:
        // the money identity holds every tick and, even with upkeep plus wages
        // paid, the treasury stays solvent over the whole window.
        EconomySimulation sim = new EconomySimulation(state(6f, 100f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("stone_mine", true);
        sim.addBuilding("farm", true);
        sim.addBuilding("farm", true);
        boolean sawWoodSale = false;
        for (int i = 0; i < 200; i++) {
            float before = sim.getMoney();
            sim.tick();
            if (sim.getSold(Resource.WOOD) > 0f) sawWoodSale = true;
            assertEquals(before + sim.getRevenue() - sim.getOperatingCosts(), sim.getMoney(), EPS,
                "sale revenue books into the treasury");
        }
        assertTrue(sawWoodSale, "the wood surplus was exported");
        assertTrue(sim.getRevenue() > 0f);
        assertTrue(sim.getMoney() > 0f, "surplus sales keep the treasury solvent despite wages");
        assertEquals(0f, sim.getProfit() - (sim.getRevenue() - sim.getOperatingCosts()), EPS);
    }

    @Test
    void pricesRiseWhenDemandExceedsSupply() {
        // Food is seeded so the population does not starve during the window:
        // the workshop keeps its single worker and wood demand stays at 1/tick.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.FOOD, 40f));
        sim.addBuilding("workshop", true);
        for (int i = 0; i < 10; i++) {
            sim.tick();
        }
        assertTrue(sim.getPrice(Resource.WOOD) > 1.2f,
            "scarce wood should get expensive, got " + sim.getPrice(Resource.WOOD));
    }

    @Test
    void pricesFallWithOversupply() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("lumber_hut", true);
        for (int i = 0; i < 15; i++) {
            sim.tick();
        }
        assertTrue(sim.getPrice(Resource.WOOD) < 0.5f,
            "oversupplied wood should get cheap, got " + sim.getPrice(Resource.WOOD));
    }

    @Test
    void eachResourceUsesItsOwnStorageLimit() {
        // A farm keeps a population of six (working population 3) fed and
        // staffed for the whole window. getSold is per-tick: with production
        // of exactly 1/tick, the last tick exports exactly one unit above the
        // resource's own cap.
        EconomySimulation wood = new EconomySimulation(state(6f, 100f, Resource.FOOD, 40f));
        wood.addBuilding("lumber_hut", true);
        wood.addBuilding("farm", true);
        for (int i = 0; i < 70; i++) {
            wood.tick();
        }
        assertEquals(60f, wood.getInventory(Resource.WOOD), EPS, "wood is capped at its own limit");
        assertEquals(1f, wood.getSold(Resource.WOOD), EPS, "one surplus unit exported this tick");

        EconomySimulation wool = new EconomySimulation(state(6f, 100f, Resource.FOOD, 40f));
        wool.addBuilding("ranch", true);
        wool.addBuilding("farm", true);
        for (int i = 0; i < 70; i++) {
            wool.tick();
        }
        assertEquals(40f, wool.getInventory(Resource.WOOL), EPS, "wool is capped at its own limit");
        assertEquals(1f, wool.getSold(Resource.WOOL), EPS, "one surplus unit exported this tick");
    }

    @Test
    void storageBuildingRaisesEveryCapEqually() {
        // Two farms keep a population of eight (working population 4) fed and
        // staffed, so the lumber hut, both farms and the central station all
        // run for the whole window.
        EconomySimulation sim = new EconomySimulation(state(8f, 200f, Resource.FOOD, 100f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("central_station", true);
        sim.addBuilding("farm", true);
        sim.addBuilding("farm", true);
        for (int i = 0; i < 110; i++) {
            sim.tick();
        }
        assertEquals(90f, sim.getInventory(Resource.WOOD), EPS,
            "central_station adds 30 to every resource cap");
        assertTrue(sim.getSold(Resource.WOOD) > 0f);
    }

    @Test
    void productionChainTurnsIronIntoFinishedFurniture() {
        // The Task 7 chain: smelter (iron + energy → steel), machine_factory
        // (steel + energy → tools/components), assembly_factory (tools +
        // energy → furniture). Seeded raw stock keeps the whole line running.
        // The market now buys some tools and furniture along the way (Task 12),
        // so every resource is tracked as produced/consumed/sold and the
        // inventories are checked against those identities.
        EconomySimulation sim = new EconomySimulation(state(6f, 100f,
            Resource.IRON, 30f, Resource.COAL, 30f, Resource.STEEL, 30f, Resource.FOOD, 100f));
        sim.addBuilding("smelter", true);
        sim.addBuilding("machine_factory", true);
        sim.addBuilding("assembly_factory", true);
        float producedSteel = 0f, producedTools = 0f, producedFurniture = 0f;
        float consumedIron = 0f, consumedCoal = 0f, consumedSteel = 0f, consumedTools = 0f;
        float soldTools = 0f, soldFurniture = 0f;
        for (int i = 0; i < 12; i++) {
            sim.tick();
            producedSteel += sim.getProduced(Resource.STEEL);
            producedTools += sim.getProduced(Resource.TOOLS);
            producedFurniture += sim.getProduced(Resource.FURNITURE);
            consumedIron += sim.getConsumed(Resource.IRON);
            consumedCoal += sim.getConsumed(Resource.COAL);
            consumedSteel += sim.getConsumed(Resource.STEEL);
            consumedTools += sim.getConsumed(Resource.TOOLS);
            soldTools += sim.getSold(Resource.TOOLS);
            soldFurniture += sim.getSold(Resource.FURNITURE);
        }
        assertTrue(producedFurniture >= 1f, "the chain turns iron into finished furniture");
        assertEquals(30f - consumedSteel + producedSteel, sim.getInventory(Resource.STEEL), EPS,
            "steel = seeded + produced - consumed by the machine factory");
        assertEquals(30f - consumedIron, sim.getInventory(Resource.IRON), EPS, "iron ore consumed");
        assertEquals(30f - consumedCoal, sim.getInventory(Resource.COAL), EPS, "energy consumed");
        assertEquals(producedTools - consumedTools - soldTools,
            sim.getInventory(Resource.TOOLS), EPS,
            "tools = produced - fed to assembly - sold to the market");
        assertEquals(producedFurniture - soldFurniture,
            sim.getInventory(Resource.FURNITURE), EPS,
            "furniture = produced - sold to the market");
        assertTrue(soldTools > 0f && soldFurniture > 0f, "the town bought from the chain");
    }

    @Test
    void chainBuildingBlocksWhenUpstreamOutputIsMissing() {
        // A machine factory with no steel stock cannot run, even though it has
        // all the energy it wants: "a building cannot produce if required
        // inputs are unavailable".
        EconomySimulation sim = new EconomySimulation(state(6f, 100f, Resource.COAL, 30f));
        sim.addBuilding("machine_factory", true);
        for (int i = 0; i < 6; i++) {
            sim.tick();
        }
        assertEquals(0f, sim.getInventory(Resource.TOOLS), EPS, "nothing produced without steel");
        assertEquals(30f, sim.getInventory(Resource.COAL), EPS, "energy untouched while blocked");
        assertEquals(1f + sim.getConsumerDemand(Resource.STEEL), sim.getDemand(Resource.STEEL), EPS,
            "the factory's need plus the town's appetite keep steel in demand");
    }

    @Test
    void batchTakesProductionTime() {
        // The smelter needs 2 ticks per steel batch.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.IRON, 2f, Resource.COAL, 2f));
        sim.addBuilding("smelter", true);
        sim.tick();
        assertEquals(0f, sim.getInventory(Resource.STEEL), EPS, "batch still in progress");
        assertEquals(2f, sim.getInventory(Resource.IRON), EPS, "inputs consumed only on completion");
        assertEquals(2f, sim.getInventory(Resource.COAL), EPS);
        sim.tick();
        assertEquals(1f, sim.getInventory(Resource.STEEL), EPS, "batch completed");
        assertEquals(1f, sim.getInventory(Resource.IRON), EPS);
        assertEquals(1f, sim.getInventory(Resource.COAL), EPS, "energy consumed with the batch");
        assertEquals(1f, sim.getConsumed(Resource.COAL), EPS);
    }

    @Test
    void finishedGoodsHaveMultipleRecipes() {
        // Carpentry still turns wood + steel into furniture in one tick; the
        // town then buys its furniture on the market (Task 12).
        EconomySimulation sim = new EconomySimulation(state(3f, 100f,
            Resource.WOOD, 5f, Resource.STEEL, 2f));
        sim.addBuilding("carpentry", true);
        sim.tick();
        assertEquals(1f, sim.getProduced(Resource.FURNITURE), EPS);
        assertEquals(sim.getConsumerDemand(Resource.FURNITURE), sim.getSold(Resource.FURNITURE), EPS,
            "the town bought its furniture on the market");
        assertEquals(1f - sim.getConsumerDemand(Resource.FURNITURE),
            sim.getInventory(Resource.FURNITURE), EPS);
        assertEquals(4f, sim.getInventory(Resource.WOOD), EPS);
        assertEquals(1f, sim.getInventory(Resource.STEEL), EPS);
    }

    @Test
    void constructionInvestsMoneyThenCompletes() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("lumber_hut", false);
        for (int i = 0; i < 10; i++) {
            sim.tick();
        }
        assertEquals(1, sim.getActiveCount("lumber_hut"), "construction finishes");
        assertEquals(1, sim.getPlacedCount("lumber_hut"));
        assertEquals(0, sim.getConstructionSiteCount());
        assertFalse(sim.isConstructing("lumber_hut"));
        assertEquals(25f, sim.getTotalInvestment(), EPS);
        // 100 minus the 25 invested, minus two active ticks of upkeep (0.2)
        // and the wages paid to the worker in those two ticks.
        assertEquals(74.6f - 2f * sim.getAverageWage(), sim.getMoney(), 0.01f);
    }

    @Test
    void constructionStallsWithoutMoney() {
        EconomySimulation sim = new EconomySimulation(state(3f, 0f));
        sim.addBuilding("lumber_hut", false);
        for (int i = 0; i < 30; i++) {
            sim.tick();
        }
        assertEquals(1, sim.getConstructionSiteCount(), "site still waiting for funds");
        assertEquals(0, sim.getActiveCount("lumber_hut"));
        assertEquals(0f, sim.getTotalInvestment(), EPS);
    }

    @Test
    void converterConsumesInputsAndProducesTools() {
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.WOOD, 5f));
        sim.addBuilding("workshop", true);
        sim.tick();
        assertEquals(4f, sim.getInventory(Resource.WOOD), EPS);
        assertEquals(1f, sim.getProduced(Resource.TOOLS), EPS);
        assertEquals(sim.getConsumerDemand(Resource.TOOLS), sim.getSold(Resource.TOOLS), EPS,
            "the town bought the workshop's tools on the market");
        assertEquals(1f - sim.getConsumerDemand(Resource.TOOLS),
            sim.getInventory(Resource.TOOLS), EPS, "the unsold tools stay in inventory");
        assertEquals(1f + sim.getConsumerDemand(Resource.WOOD), sim.getDemand(Resource.WOOD), EPS);
    }

    @Test
    void productionIsBlockedWhenInputsAreShort() {
        // A workshop batch needs a full unit of wood: with only half a unit,
        // it produces nothing and keeps the wood untouched. No partial runs.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f, Resource.WOOD, 0.5f));
        sim.addBuilding("workshop", true);
        sim.tick();
        assertEquals(0f, sim.getInventory(Resource.TOOLS), EPS);
        assertEquals(0.5f, sim.getInventory(Resource.WOOD), EPS, "inputs are never partially consumed");
        assertEquals(0f, sim.getConsumed(Resource.WOOD), EPS);
        assertEquals(1f + sim.getConsumerDemand(Resource.WOOD), sim.getDemand(Resource.WOOD), EPS,
            "the blocked input plus the town's appetite stay in demand");
    }

    @Test
    void reconcileBuildingsAddsAndRemoves() {
        EconomySimulation sim = new EconomySimulation();
        Map<String, Integer> counts = new TreeMap<>();
        counts.put("lumber_hut", 1);
        counts.put("small_house", 2);
        sim.reconcileBuildings(counts, true);
        assertEquals(1, sim.getPlacedCount("lumber_hut"));
        assertEquals(2, sim.getPlacedCount("small_house"));
        assertEquals(1, sim.getActiveCount("lumber_hut"), "restored buildings are already built");

        Map<String, Integer> fewer = new TreeMap<>();
        fewer.put("small_house", 1);
        sim.reconcileBuildings(fewer, false);
        assertEquals(0, sim.getPlacedCount("lumber_hut"));
        assertEquals(1, sim.getPlacedCount("small_house"));
    }

    @Test
    void reconcileCreatesConstructionSitesForLivePlacement() {
        EconomySimulation sim = new EconomySimulation();
        Map<String, Integer> counts = new TreeMap<>();
        counts.put("lumber_hut", 1);
        sim.reconcileBuildings(counts, false);
        assertEquals(1, sim.getPlacedCount("lumber_hut"));
        assertEquals(1, sim.getConstructionSiteCount(), "fresh placement enters construction");
        assertEquals(0, sim.getActiveCount("lumber_hut"));
    }

    @Test
    void toStateRoundTripsPersistedFields() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("lumber_hut", true);
        for (int i = 0; i < 5; i++) {
            sim.tick();
        }
        EconomyState state = sim.toState();
        // 100 minus five ticks of upkeep, minus the wages paid to the single
        // lumber worker over those five ticks.
        assertEquals(100f - 5f * 0.2f - 5f * sim.getAverageWage(), state.money, 0.02f);
        assertEquals(3f, state.population, EPS);
        assertEquals(15f, stockOf(state, Resource.WOOD), EPS);
        assertEquals(0, state.construction.size);
        assertEquals(sim.getAverageWage(), state.averageWage, EPS, "the wage survives the snapshot");

        EconomySimulation restored = new EconomySimulation(state);
        restored.addBuilding("lumber_hut", true);
        assertEquals(sim.getMoney(), restored.getMoney(), EPS);
        assertEquals(sim.getPopulation(), restored.getPopulation(), EPS);
        assertEquals(sim.getAverageWage(), restored.getAverageWage(), EPS, "restored wage matches");
        for (Resource resource : Resource.values()) {
            assertEquals(sim.getInventory(resource), restored.getInventory(resource), EPS);
        }
    }

    @Test
    void legacyMoneyFieldSeedsTheCoinsStock() {
        // A save written before the resource system had no COINS stock, only
        // EconomyState.money. Restoring it must seed the treasury.
        EconomyState legacy = new EconomyState();
        legacy.population = 3f;
        legacy.money = 42f;
        EconomySimulation restored = new EconomySimulation(legacy);
        assertEquals(42f, restored.getMoney(), EPS);
        assertEquals(42f, restored.getInventory(Resource.COINS), EPS);
    }

    @Test
    void coinsStockWinsOverLegacyMoneyField() {
        EconomyState state = state(3f, 42f, Resource.COINS, 99f);
        EconomySimulation restored = new EconomySimulation(state);
        assertEquals(99f, restored.getMoney(), EPS, "explicit COINS stock is authoritative");
    }

    @Test
    void constructionSitesSurviveStateSnapshot() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("farm", false);
        sim.tick();
        assertEquals(1, sim.getConstructionSiteCount());
        EconomyState state = sim.toState();
        assertEquals(1, state.construction.size);
        assertEquals("farm", state.construction.first().typeId);
        assertTrue(state.construction.first().remainingCost < 25f, "some funds already invested");

        EconomySimulation restored = new EconomySimulation(state);
        assertEquals(1, restored.getConstructionSiteCount());
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        EconomySimulation first = new EconomySimulation();
        EconomySimulation second = new EconomySimulation();
        for (EconomySimulation sim : new EconomySimulation[]{first, second}) {
            sim.addBuilding("lumber_hut", true);
            sim.addBuilding("workshop", true);
            sim.addBuilding("small_house", true);
            sim.addBuilding("small_house", true);
            sim.addBuilding("farm", true);
            for (int i = 0; i < 50; i++) {
                sim.tick();
            }
        }
        assertEquals(first.getMoney(), second.getMoney(), EPS);
        assertEquals(first.getPopulation(), second.getPopulation(), EPS);
        for (Resource resource : Resource.values()) {
            assertEquals(first.getInventory(resource), second.getInventory(resource), EPS);
            assertEquals(first.getPrice(resource), second.getPrice(resource), EPS);
        }
    }

    @Test
    void updateAccumulatesWholeTicks() {
        EconomySimulation sim = new EconomySimulation();
        sim.update(0.4f);
        assertEquals(0, sim.getTickCount());
        sim.update(0.9f);
        assertEquals(1, sim.getTickCount());
        sim.update(2.0f);
        assertEquals(3, sim.getTickCount());
    }

    // ── Task 8: inventory and warehousing ────────────────────────────────

    @Test
    void settlementStartsWithBaseStorageAndWarehousesExtendIt() {
        EconomySimulation sim = new EconomySimulation();
        assertEquals(200f, sim.getStorageCapacity(), EPS, "base storage before any building");
        sim.addBuilding("warehouse", true);
        sim.addBuilding("large_warehouse", true);
        assertEquals(550f, sim.getStorageCapacity(), EPS, "100 + 250 from the two warehouses");
        assertEquals(1f, sim.getWarehouseActiveCount("warehouse"), EPS);
        assertEquals(100f, sim.getWarehouseCapacity("warehouse"), EPS);
        assertEquals(0.02f, sim.getWarehouseStorageCostPerUnit("warehouse"), EPS);
        assertEquals(250f, sim.getWarehouseCapacity("large_warehouse"), EPS);
        assertEquals(0.015f, sim.getWarehouseStorageCostPerUnit("large_warehouse"), EPS);
    }

    @Test
    void storageCostIsDeductedFromTheTreasury() {
        // No production and no sales: money falls by the warehouse upkeep (0.3),
        // the storage fee of 0.02 per stored non-money unit and the wage of the
        // single worker the warehouse employs. Food is the only inventory that
        // moves (the population eats 0.75/tick).
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("warehouse", true);
        sim.tick();
        float stored = 10f + 10f + 11.25f; // wood + stone + food after one tick
        assertEquals(stored * 0.02f, sim.getStorageCosts(), EPS);
        assertEquals(1f, sim.getEmployedWorkers(), EPS, "the warehouse needs one worker");
        assertEquals(sim.getAverageWage(), sim.getWageCosts(), EPS, "one worker at the average wage");
        assertEquals(100f - 0.3f - stored * 0.02f - sim.getWageCosts(), sim.getMoney(), EPS);
        assertTrue(sim.getOperatingCosts() >= sim.getStorageCosts() + sim.getWageCosts(),
            "storage fee and wages are part of operating costs");
    }

    @Test
    void reservedInputsReduceAvailableStorageUntilTheBatchCompletes() {
        // A 2-tick smelter reserves its iron input plus its coal energy when
        // the batch starts; while in flight both count as reserved stock that
        // reduces the settlement's available storage.
        EconomySimulation sim = new EconomySimulation(state(6f, 100f,
            Resource.WOOD, 10f, Resource.STONE, 10f, Resource.IRON, 30f,
            Resource.COAL, 30f, Resource.FOOD, 100f));
        sim.addBuilding("smelter", true);
        sim.tick();
        assertEquals(1f, sim.getReserved(Resource.IRON), EPS);
        assertEquals(1f, sim.getReserved(Resource.COAL), EPS);
        assertEquals(2f, sim.getReservedInventoryTotal(), EPS);
        // The seeded food surplus is exported the same tick (food caps at 40).
        float storedAfterTick = 10f + 10f + 40f + 30f + 30f;
        assertEquals(200f - storedAfterTick - 2f, sim.getAvailableStorage(), EPS,
            "available storage = capacity - stored - reserved");

        sim.tick();
        assertEquals(0f, sim.getReserved(Resource.IRON), EPS, "batch completed, reservation fulfilled");
        assertEquals(0f, sim.getReserved(Resource.COAL), EPS);
        assertEquals(1f, sim.getBuildingIncoming("smelter", Resource.IRON), EPS);
        assertEquals(1f, sim.getBuildingConsumed("smelter", Resource.IRON), EPS);
        assertEquals(1f, sim.getBuildingConsumed("smelter", Resource.COAL), EPS);
        assertEquals(1f, sim.getBuildingProduced("smelter", Resource.STEEL), EPS);
        assertEquals(1f, sim.getBuildingOutgoing("smelter", Resource.STEEL), EPS);
        assertEquals(0f, sim.getBuildingStored("smelter", Resource.STEEL), EPS,
            "steel is shipped straight into the settlement");
        assertEquals(1f, sim.getInventory(Resource.STEEL), EPS);
    }

    @Test
    void fullStorageStopsProduction() {
        // Wood, stone, iron and wool already overflow the base 200 storage, so
        // the lumber hut can never ship its output; its own 20-unit stockpile
        // fills up and production stops until space frees.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f,
            Resource.WOOD, 60f, Resource.STONE, 60f, Resource.IRON, 60f,
            Resource.WOOL, 40f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        for (int i = 0; i < 100; i++) {
            sim.tick();
        }
        assertEquals(20f, sim.getBuildingStored("lumber_hut", Resource.WOOD), EPS,
            "the on-site stockpile fills to its capacity");
        assertEquals(0f, sim.getProduced(Resource.WOOD), EPS,
            "production stopped while storage is full");
        assertEquals(60f, sim.getInventory(Resource.WOOD), EPS,
            "nothing ever shipped into the overflowing settlement");
    }

    @Test
    void removingAProducerReleasesItsInputReservations() {
        EconomySimulation sim = new EconomySimulation(state(3f, 100f,
            Resource.IRON, 5f, Resource.COAL, 5f));
        sim.addBuilding("smelter", true);
        sim.tick();
        assertEquals(1f, sim.getReserved(Resource.IRON), EPS);
        assertEquals(1f, sim.getReserved(Resource.COAL), EPS);
        sim.removeBuilding("smelter");
        assertEquals(0f, sim.getReserved(Resource.IRON), EPS, "reserved stock is returned");
        assertEquals(0f, sim.getReserved(Resource.COAL), EPS);
        assertEquals(5f, sim.getInventory(Resource.IRON), EPS, "stored stock is untouched by removal");
        assertEquals(0f, sim.getBuildingStored("smelter", Resource.STEEL), EPS, "stockpile discarded");
    }

    // ── Task 9: workforce and employment ─────────────────────────────────

    @Test
    void workingPopulationIsThePartOfWorkingAge() {
        EconomySimulation sim = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        assertEquals(0.05f, sim.getAverageWage(), EPS, "a fresh settlement starts at the base wage");
        sim.tick();
        assertEquals(8f, sim.getPopulation(), EPS);
        assertEquals(4f, sim.getWorkingPopulation(), EPS, "half the population is of working age");
        assertEquals(4f, sim.getAvailableWorkers(), EPS);
        assertEquals(0f, sim.getRequiredWorkers(), EPS, "no buildings, no positions");
        assertEquals(0f, sim.getEmployedWorkers(), EPS);
        assertEquals(4f, sim.getUnemployedWorkers(), EPS);
        assertEquals(1f, sim.getProductionEfficiency(), EPS, "nothing required is trivially 100%");
    }

    @Test
    void workforceAccountingCoversEverySpecFigure() {
        // Four positions and a working population of four: everyone works and
        // every building is staffed.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 40f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("stone_mine", true);
        sim.addBuilding("farm", true);
        sim.addBuilding("farm", true);
        sim.tick();
        assertEquals(4f, sim.getWorkingPopulation(), EPS);
        assertEquals(4f, sim.getRequiredWorkers(), EPS);
        assertEquals(4f, sim.getEmployedWorkers(), EPS, "all four positions filled");
        assertEquals(0f, sim.getUnemployedWorkers(), EPS);
        assertEquals(1f, sim.getProductionEfficiency(), EPS);
        assertEquals(2f, sim.getBuildingRequiredWorkers("farm"), EPS);
        assertEquals(2f, sim.getBuildingEmployedWorkers("farm"), EPS);
        assertEquals(1f, sim.getBuildingEmployedWorkers("lumber_hut"), EPS);
        assertEquals(1f, sim.getBuildingEmployedWorkers("stone_mine"), EPS);
        assertEquals(1f, sim.getBuildingProductionEfficiency("lumber_hut"), EPS);
    }

    @Test
    void laborShortageCapsEmploymentAndEfficiency() {
        // Four positions but a working population of three — the Task 9 spec
        // example: required 4, available 3, so efficiency is 75%. The jobs
        // nobody can fill stay open, and greedy assignment in sorted type
        // order leaves the alphabetically-last building empty.
        EconomySimulation sim = new EconomySimulation(state(6f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 40f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("stone_mine", true);
        sim.addBuilding("farm", true);
        sim.addBuilding("farm", true);
        sim.tick();
        assertEquals(3f, sim.getWorkingPopulation(), EPS);
        assertEquals(4f, sim.getRequiredWorkers(), EPS);
        assertEquals(3f, sim.getEmployedWorkers(), EPS, "only the three workers can be hired");
        assertEquals(0f, sim.getUnemployedWorkers(), EPS);
        assertEquals(0.75f, sim.getProductionEfficiency(), EPS, "3 available / 4 required");
        assertEquals(2f, sim.getBuildingEmployedWorkers("farm"), EPS, "farms staff first");
        assertEquals(1f, sim.getBuildingEmployedWorkers("lumber_hut"), EPS);
        assertEquals(0f, sim.getBuildingEmployedWorkers("stone_mine"), EPS,
            "the stone mine gets nobody");
        assertEquals(0f, sim.getBuildingProductionEfficiency("stone_mine"), EPS);
        assertEquals(1f, sim.getBuildingProductionEfficiency("lumber_hut"), EPS);
    }

    @Test
    void wagesRiseWhenLaborIsScarce() {
        // Identical production, different labor pools: one settlement has
        // exactly enough workers for its two jobs, the other has twice as
        // many. Labor scarcity pushes the wage up while a slack market keeps
        // it near the baseline.
        EconomySimulation tight = new EconomySimulation(state(4f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 40f, Resource.FOOD, 100f));
        tight.addBuilding("lumber_hut", true);
        tight.addBuilding("stone_mine", true);
        EconomySimulation loose = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 40f, Resource.FOOD, 100f));
        loose.addBuilding("lumber_hut", true);
        loose.addBuilding("stone_mine", true);
        for (int i = 0; i < 20; i++) {
            tight.tick();
            loose.tick();
        }
        assertEquals(2f, tight.getEmployedWorkers(), EPS, "the tight market hires everyone");
        assertEquals(0f, tight.getUnemployedWorkers(), EPS);
        assertEquals(2f, loose.getEmployedWorkers(), EPS);
        assertEquals(2f, loose.getUnemployedWorkers(), EPS);
        assertEquals(1f, tight.getProductionEfficiency(), EPS);
        assertTrue(tight.getAverageWage() > loose.getAverageWage(),
            "a fully employed labor market pushes wages above a slack one");
        assertTrue(tight.getAverageWage() > 0.05f, "scarcity pushes the wage above the baseline");
    }

    @Test
    void wagesAreAPaidOperatingCost() {
        // Two lumber workers at the average wage: the treasury pays exactly
        // employed × wage on top of the buildings' 0.2 upkeep each.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 40f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("stone_mine", true);
        sim.tick();
        assertEquals(2f, sim.getEmployedWorkers(), EPS);
        assertEquals(2f * sim.getAverageWage(), sim.getWageCosts(), EPS);
        assertEquals(0.4f + sim.getWageCosts(), sim.getOperatingCosts(), EPS,
            "0.2 upkeep per factory plus the wage bill");
        assertEquals(100f - 0.4f - sim.getWageCosts(), sim.getMoney(), EPS);
    }

    // ── Task 10: building operating costs ────────────────────────────────

    @Test
    void everyProductiveBuildingHasRecurringCosts() {
        // Data-driven: every producer/converter pays maintenance (its operating
        // cost) and hires staff, so a working factory always has recurring
        // costs — never a free lunch.
        int producers = 0;
        for (BuildingEconomy profile : EconomyData.getAll()) {
            if (!profile.isProducer()) continue;
            producers++;
            assertTrue(profile.getOperatingCost() > 0f,
                profile.getBuildingId() + " pays maintenance every tick");
            assertTrue(profile.getWorkforce() > 0f,
                profile.getBuildingId() + " hires workers (and so pays wages)");
        }
        assertTrue(producers >= 10, "the table carries the expected producer set");
    }

    @Test
    void costLedgerBreaksDownEveryCategory() {
        // A lumber hut (wages + maintenance), a smelter (energy + materials +
        // maintenance), a workshop (materials + maintenance) and a warehouse
        // (transport fees) exercise every ledger bucket at once.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.IRON, 30f,
            Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("smelter", true);
        sim.addBuilding("warehouse", true);
        sim.addBuilding("workshop", true);
        for (int i = 0; i < 4; i++) {
            sim.tick();
        }
        assertEquals(1.2f, sim.getMaintenanceCosts(), EPS,
            "0.2 + 0.4 + 0.3 + 0.3 for the four active buildings");
        assertEquals(sim.getEmployedWorkers() * sim.getAverageWage(), sim.getWageCosts(), EPS,
            "wages are the staff bill");
        assertEquals(sim.getConsumed(Resource.COAL) * sim.getPrice(Resource.COAL),
            sim.getEnergyCosts(), EPS, "energy is the coal burnt, valued at market price");
        assertEquals(sim.getConsumed(Resource.WOOD) * sim.getPrice(Resource.WOOD)
                + sim.getConsumed(Resource.IRON) * sim.getPrice(Resource.IRON),
            sim.getMaterialsCosts(), EPS, "materials are the consumed inputs at market price");
        assertEquals(sim.getStorageCosts(), sim.getCostLedger().transport, EPS);
        assertTrue(sim.getConsumed(Resource.COAL) > 0f, "the smelter burnt coal by now");
        assertTrue(sim.getConsumed(Resource.WOOD) > 0f, "the workshop consumed wood");

        EconomySimulation.CostLedger ledger = sim.getCostLedger();
        assertEquals(ledger.wages + ledger.energy + ledger.materials
                + ledger.maintenance + ledger.transport + ledger.hauling, ledger.total, EPS,
            "total is the sum of every bucket");
        assertEquals(sim.getRevenue() - ledger.total, ledger.net, EPS,
            "net is revenue minus the full economic cost");
        assertEquals(ledger.total, sim.getTotalOperatingCosts(), EPS);
        assertEquals(ledger.net, sim.getNetIncome(), EPS);
    }

    @Test
    void energyAndMaterialsAreValuedNotPaid() {
        // The treasury pays wages, maintenance and transport in coins; the
        // energy and materials used by production are in-kind, so the coin
        // outflow (operatingCosts) stays the sum of the paid buckets while the
        // economic ledger also values the in-kind consumption.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.IRON, 30f,
            Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("smelter", true);
        sim.addBuilding("workshop", true);
        sim.tick();
        sim.tick();
        assertEquals(sim.getMaintenanceCosts() + sim.getStorageCosts() + sim.getWageCosts(),
            sim.getOperatingCosts(), EPS, "coin outflow excludes in-kind use");
        assertTrue(sim.getEnergyCosts() > 0f, "the smelter burnt coal");
        assertTrue(sim.getMaterialsCosts() > 0f, "the workshop consumed wood");
        assertTrue(sim.getTotalOperatingCosts() > sim.getOperatingCosts(),
            "the economic ledger values the in-kind consumption too");
        assertEquals(sim.getRevenue() - sim.getOperatingCosts(), sim.getProfit(), EPS,
            "coin profit still matches the treasury delta");
    }

    @Test
    void buildingRecurringCostsAggregateMaintenanceAndWages() {
        EconomySimulation sim = new EconomySimulation(state(3f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.FOOD, 40f));
        sim.addBuilding("lumber_hut", true);
        sim.tick();
        assertEquals(0.2f, sim.getBuildingMaintenanceCost("lumber_hut"), EPS);
        assertEquals(sim.getAverageWage(), sim.getBuildingWageCost("lumber_hut"), EPS,
            "one worker at the average wage");
        assertEquals(0.2f + sim.getAverageWage(), sim.getBuildingRecurringCosts("lumber_hut"), EPS,
            "recurring cost = maintenance + wages");
    }

    @Test
    void unstaffedBuildingsStillPayMaintenance() {
        EconomySimulation sim = new EconomySimulation(state(0f, 100f));
        sim.addBuilding("lumber_hut", true);
        sim.tick();
        assertEquals(0f, sim.getEmployedWorkers(), EPS, "nobody to hire");
        assertEquals(0f, sim.getBuildingWageCost("lumber_hut"), EPS, "no staff, no wages");
        assertEquals(0.2f, sim.getBuildingRecurringCosts("lumber_hut"), EPS,
            "maintenance is still charged on the idle building");
    }

    // ── Task 11 — Supply and demand ──────────────────────────────────────

    @Test
    void consumerDemandScalesWithPopulation() {
        // The population's own appetite is a per-citizen preference: twice the
        // citizens, twice the demand for a good (at the base price).
        EconomySimulation many = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        EconomySimulation few = new EconomySimulation(state(4f, 100f, Resource.FOOD, 40f));
        many.tick();
        few.tick();
        float perCitizen = Resource.TOOLS.getConsumerPreference();
        assertEquals(8f * perCitizen, many.getConsumerDemand(Resource.TOOLS), EPS);
        assertEquals(4f * perCitizen, few.getConsumerDemand(Resource.TOOLS), EPS);
        assertTrue(many.getConsumerDemand(Resource.TOOLS) > few.getConsumerDemand(Resource.TOOLS));
    }

    @Test
    void localDevelopmentRaisesConsumerDemand() {
        // Each active building makes the town want more of everything: a
        // settlement with two houses exerts visibly higher consumer demand.
        EconomySimulation base = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        EconomySimulation developed = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        developed.addBuilding("small_house", true);
        developed.addBuilding("small_house", true);
        base.tick();
        developed.tick();
        assertEquals(1f, base.getDevelopmentFactor(), EPS);
        assertEquals(1.1f, developed.getDevelopmentFactor(), EPS, "two active buildings raise development");
        float appetite = 8f * Resource.TOOLS.getConsumerPreference();
        assertEquals(appetite, base.getConsumerDemand(Resource.TOOLS), EPS);
        assertEquals(appetite * 1.1f, developed.getConsumerDemand(Resource.TOOLS), EPS,
            "development multiplies the town's appetite");
    }

    @Test
    void highPricesCurbConsumerDemand() {
        // A wood shortage pushes the price up; the higher price then damps the
        // population's appetite for wood (price elasticity).
        EconomySimulation sim = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        sim.addBuilding("workshop", true);
        sim.tick();
        assertTrue(sim.getPrice(Resource.WOOD) > 1f, "a wood shortage pushes the price up");
        sim.tick();
        assertTrue(sim.getPriceElasticity(Resource.WOOD) < 1f, "a high price dampens the appetite");
        float baseAppetite = 8f * Resource.WOOD.getConsumerPreference() * sim.getDevelopmentFactor();
        assertTrue(sim.getConsumerDemand(Resource.WOOD) < baseAppetite,
            "demand fell as the price rose");
    }

    @Test
    void consumerPreferencesMakePricesRespondToTaste() {
        // The town loves its finished goods: furniture is demanded far more
        // strongly than a plain industrial input, so its market holds value.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        sim.tick();
        assertTrue(sim.getConsumerDemand(Resource.FURNITURE) > sim.getConsumerDemand(Resource.STEEL),
            "the finished good the town loves outpaces the industrial input");
        assertTrue(sim.getConsumerDemand(Resource.FURNITURE)
                > sim.getConsumerDemand(Resource.WOOD));
    }

    @Test
    void supplyShortageRaisesPrice() {
        // A machine factory starved of steel cannot produce, so steel stays
        // scarce and the market punishes the gap with a rising price.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("machine_factory", true);
        for (int i = 0; i < 8; i++) {
            sim.tick();
        }
        assertEquals(0f, sim.getInventory(Resource.TOOLS), EPS, "blocked factory produced nothing");
        assertTrue(sim.getPrice(Resource.STEEL) > 3f,
            "scarce steel gets expensive, got " + sim.getPrice(Resource.STEEL));
    }

    @Test
    void marketSimulationKeepsFinishedGoodsValuable() {
        // The carpentry shop keeps producing furniture and the town keeps
        // buying it: the town's appetite holds the price well above its price
        // floor even though supply keeps growing — consumer demand keeps
        // finished goods valuable.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STEEL, 20f, Resource.FOOD, 60f));
        sim.addBuilding("carpentry", true);
        float producedTotal = 0f;
        float soldTotal = 0f;
        for (int i = 0; i < 20; i++) {
            sim.tick();
            producedTotal += sim.getProduced(Resource.FURNITURE);
            soldTotal += sim.getSold(Resource.FURNITURE);
        }
        assertEquals(producedTotal - soldTotal, sim.getInventory(Resource.FURNITURE), EPS,
            "furniture = produced minus what the market bought");
        assertTrue(soldTotal > 0f, "the town kept buying furniture");
        assertTrue(sim.getPrice(Resource.FURNITURE) > 1.3f,
            "furniture stays well above its 1.2 floor, got " + sim.getPrice(Resource.FURNITURE));
    }

    @Test
    void pricesRespondGraduallyToImbalance() {
        // The market never jumps: each tick only closes PRICE_DRIFT of the gap
        // toward the balanced target, then drifts onward as the imbalance holds.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f, Resource.FOOD, 40f));
        sim.addBuilding("workshop", true);
        float worstJump = 0f;
        for (int i = 0; i < 8; i++) {
            float before = sim.getPrice(Resource.WOOD);
            sim.tick();
            worstJump = Math.max(worstJump, Math.abs(sim.getPrice(Resource.WOOD) - before));
        }
        assertTrue(worstJump < 0.9f, "no single tick jumps the price, worst jump " + worstJump);
        assertTrue(sim.getPrice(Resource.WOOD) > 1.5f,
            "the market converged toward the high target, got " + sim.getPrice(Resource.WOOD));
    }

    // ── Task 12 — Market and sales logic ─────────────────────────────────

    @Test
    void marketSellsThePopulationItsGoods() {
        // The market clears the smaller of available supply and consumer
        // demand: a carpentry shop makes furniture faster than the town wants
        // it, so exactly the town's appetite sells and the rest stays in
        // inventory, earning the market price for what did sell.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STEEL, 20f, Resource.FOOD, 40f));
        sim.addBuilding("carpentry", true);
        sim.tick();
        float appetite = sim.getConsumerDemand(Resource.FURNITURE);
        assertEquals(Math.min(sim.getMarketSupply(Resource.FURNITURE), appetite),
            sim.getSold(Resource.FURNITURE), EPS, "sales volume = min(supply, consumer demand)");
        assertEquals(1f - appetite, sim.getInventory(Resource.FURNITURE), EPS,
            "the unsold furniture stays in inventory");
        assertEquals(appetite * sim.getPrice(Resource.FURNITURE), sim.getRevenue(), EPS,
            "sales earn the market price for what sold");
    }

    @Test
    void overproductionPilesInventoryAndCutsPrice() {
        // A workshop makes 1 tool per tick but the town only wants a fraction:
        // the unsold surplus accumulates in inventory while the price drifts
        // below its base — "when production exceeds demand: inventory up,
        // price down".
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 60f, Resource.FOOD, 60f));
        sim.addBuilding("workshop", true);
        float afterFirstTick = 0f;
        for (int i = 0; i < 15; i++) {
            sim.tick();
            if (i == 0) afterFirstTick = sim.getInventory(Resource.TOOLS);
        }
        assertTrue(sim.getInventory(Resource.TOOLS) > afterFirstTick,
            "unsold tools keep accumulating, got " + sim.getInventory(Resource.TOOLS));
        assertTrue(sim.getPrice(Resource.TOOLS) < Resource.TOOLS.getBasePrice(),
            "oversupplied tools get cheaper, got " + sim.getPrice(Resource.TOOLS));
    }

    @Test
    void shortageDrainsInventoryAndRaisesPrice() {
        // A workshop starved of wood cannot make new tools, but the town keeps
        // buying: the stored tools drain toward zero while the scarce tools
        // get expensive — "when demand exceeds supply: inventory down, price
        // up".
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.TOOLS, 2f, Resource.FOOD, 60f));
        sim.addBuilding("workshop", true);
        for (int i = 0; i < 10; i++) {
            sim.tick();
        }
        assertTrue(sim.getInventory(Resource.TOOLS) < 2f, "the market drained the tool stock");
        assertTrue(sim.getPrice(Resource.TOOLS) > Resource.TOOLS.getBasePrice(),
            "scarce tools get expensive, got " + sim.getPrice(Resource.TOOLS));
    }

    @Test
    void marketTransparencyExplainsPricesAndSales() {
        // Every tick, the market info exposes why things moved: sales volume
        // is the clearing amount, the price is drifting toward the balanced
        // target, and the demand/supply figures match the simulation's own
        // numbers. The player can inspect exactly this.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 60f, Resource.FOOD, 60f));
        sim.addBuilding("workshop", true);
        for (int i = 0; i < 10; i++) {
            sim.tick();
            EconomySimulation.MarketInfo tools = sim.getMarketInfo(Resource.TOOLS);
            assertEquals(sim.getDemand(Resource.TOOLS), tools.demand, EPS);
            assertEquals(sim.getConsumerDemand(Resource.TOOLS), tools.consumerDemand, EPS);
            assertEquals(sim.getMarketSupply(Resource.TOOLS), tools.supply, EPS);
            assertEquals(sim.getSold(Resource.TOOLS), tools.salesVolume, EPS);
            assertEquals(sim.getPrice(Resource.TOOLS), tools.price, EPS);
            assertEquals(sim.getMarketTargetPrice(Resource.TOOLS), tools.targetPrice, EPS);
            assertEquals(Math.min(sim.getMarketSupply(Resource.TOOLS),
                sim.getConsumerDemand(Resource.TOOLS)), tools.salesVolume, EPS,
                "the reported sales volume is the market clearing amount");
            assertTrue(tools.consumerGood, "tools are sold to the town");
            assertEquals(EconomySimulation.PriceTrend.of(tools.targetPrice, tools.price),
                tools.priceTrend, "the trend is exactly the target-vs-price gap");
        }
    }

    // ── Task 13: transportation and logistics ───────────────────────────

    @Test
    void routeDeliversGoodsAfterTravelTime() {
        // A delivery route replaces the instant ship-out: the truck leaves the
        // loading point, travels six ticks and hands its load to the
        // settlement (the central warehouse). Nothing arrives until then.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        EconomySimulation.DeliveryRoute route = sim.addRoute("iron_mine", Resource.IRON, 2);
        assertEquals(6, sim.getRouteTripTicks("iron_mine", Resource.IRON), EPS,
            "the base trip takes 6 ticks");
        sim.tick();
        assertEquals(1, route.tripsLaunched, "one trip left the loading point");
        assertEquals(1, sim.getRouteTrips("iron_mine", Resource.IRON), "one trip on the road");
        assertTrue(sim.getRouteInTransit("iron_mine", Resource.IRON) > 0f, "the load is travelling");
        assertEquals(7, sim.getRouteArrivalTick("iron_mine", Resource.IRON),
            "the first load arrives on tick 7");
        assertEquals(0f, sim.getInventory(Resource.IRON), EPS, "nothing reached the settlement yet");
        for (int i = 0; i < 5; i++) sim.tick();
        assertEquals(0f, sim.getInventory(Resource.IRON), EPS, "still travelling after six ticks");
        sim.tick();
        assertTrue(sim.getInventory(Resource.IRON) > 0f, "the load arrived on tick 7");
        assertEquals(7, sim.getTickCount());
    }

    @Test
    void routeReplacesInstantShipOut() {
        EconomySimulation instant = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        instant.addBuilding("iron_mine", true);
        instant.tick();
        assertEquals(1f, instant.getInventory(Resource.IRON), EPS,
            "without a route the good ships instantly");

        EconomySimulation routed = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        routed.addBuilding("iron_mine", true);
        routed.addRoute("iron_mine", Resource.IRON, 2);
        routed.tick();
        assertEquals(0f, routed.getInventory(Resource.IRON), EPS,
            "with a route the load stays on the road");
        assertTrue(routed.getRouteInTransit("iron_mine", Resource.IRON) > 0f);
        routed.removeRoute("iron_mine", Resource.IRON);
        routed.tick();
        assertEquals(1f, routed.getInventory(Resource.IRON), EPS,
            "removing the route restores instant ship-out");
    }

    @Test
    void truckDepotExpandsTheFleetAndSpeedsDeliveries() {
        // A truck depot adds vehicles to the base fleet and scales every
        // truck's capacity and speed, so trips carry more and arrive sooner.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        sim.addBuilding("truck_depot", true);
        sim.addRoute("iron_mine", Resource.IRON, 2);
        assertEquals(6, sim.getTruckCount(), "base fleet plus the depot's trucks");
        assertEquals(7.5f, sim.getTruckCapacity(), EPS, "the depot's bonus scales every truck");
        assertEquals(45f, sim.getTransportCapacity(), EPS, "fleet × per-truck capacity");
        assertEquals(15f, sim.getRouteCapacity("iron_mine", Resource.IRON), EPS,
            "two assigned trucks × the boosted capacity");
        assertEquals(4, sim.getRouteTripTicks("iron_mine", Resource.IRON), EPS,
            "the faster fleet shortens the trip");
    }

    @Test
    void roadsShortenDeliveryTime() {
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        sim.addRoute("iron_mine", Resource.IRON, 2);
        assertEquals(1f, sim.getRoadFactor(), EPS);
        assertEquals(6f, sim.getRouteTripTicks("iron_mine", Resource.IRON), EPS,
            "no road: the full six ticks");
        sim.addBuilding("dirt_road", true);
        assertEquals(1.2f, sim.getRoadFactor(), EPS);
        assertEquals(5f, sim.getRouteTripTicks("iron_mine", Resource.IRON), EPS,
            "a dirt road cuts the trip to 6 / 1.2");
        sim.addBuilding("cobbled_road", true);
        assertEquals(1.7f, sim.getRoadFactor(), EPS);
        assertEquals(4f, sim.getRouteTripTicks("iron_mine", Resource.IRON), EPS,
            "6 / 1.7 rounds to four ticks");
    }

    @Test
    void tripCostsFlowIntoTheCostLedgerAndTreasury() {
        // Every dispatched trip pays the hauling fee: it shows up in its own
        // ledger bucket and leaves the treasury like any paid operating cost.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        EconomySimulation.DeliveryRoute route = sim.addRoute("iron_mine", Resource.IRON, 2);
        sim.tick();
        assertEquals(1, route.tripsLaunched, "one trip dispatched");
        assertEquals(0.1f, sim.getHaulingCosts(), EPS, "one trip's fee this tick");
        assertEquals(100f - sim.getMaintenanceCosts() - sim.getWageCosts() - sim.getHaulingCosts(),
            sim.getMoney(), EPS, "maintenance, wages and the trip fee left the treasury");
        sim.tick();
        sim.tick();
        assertEquals(3, route.tripsLaunched, "a trip dispatched every tick");
        assertEquals(sim.getStorageCosts() + sim.getHaulingCosts(), sim.getTransportCosts(), EPS,
            "transportation = warehousing fees + hauling");
        EconomySimulation.CostLedger ledger = sim.getCostLedger();
        assertEquals(sim.getHaulingCosts(), ledger.hauling, EPS,
            "the ledger exposes the hauling bucket");
    }

    @Test
    void warehousesRaiseTheDispatchBudget() {
        // Warehouses are the staging yards of the settlement: each one lets
        // one extra delivery trip start every tick.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        assertEquals(1, sim.getDispatchBudget(), "one trip per tick by default");
        sim.addBuilding("warehouse", true);
        assertEquals(2, sim.getDispatchBudget(), "each warehouse stages one extra trip");
        sim.addBuilding("large_warehouse", true);
        assertEquals(3, sim.getDispatchBudget(), "large warehouses stage too");
    }

    @Test
    void routedGoodsReachTheFactoryDownTheChain() {
        // Mine → Truck → Warehouse → Factory: no iron is seeded, so the
        // smelter can only run on iron the delivery route carries to the
        // settlement (the warehouse staging yard) and then into the factory.
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        sim.addBuilding("smelter", true);
        sim.addBuilding("warehouse", true);
        sim.addRoute("iron_mine", Resource.IRON, 2);
        assertEquals(0f, sim.getConsumed(Resource.IRON), EPS,
            "no iron can be used before the trucks deliver it");
        for (int i = 0; i < 9; i++) sim.tick();
        assertTrue(sim.getConsumed(Resource.IRON) > 0f,
            "the routed iron reached the settlement and the smelter consumed it");
        assertTrue(sim.getInventory(Resource.IRON) > 0f, "arrived iron also sits in the settlement");
    }

    private static EconomyState state(float population, float money, Object... resourceAmounts) {
        EconomyState state = new EconomyState();
        state.population = population;
        state.money = money;
        for (int i = 0; i < resourceAmounts.length; i += 2) {
            EconomyState.StockState stock = new EconomyState.StockState();
            stock.goodId = ((Resource) resourceAmounts[i]).getId();
            stock.amount = (Float) resourceAmounts[i + 1];
            state.inventory.add(stock);
        }
        return state;
    }

    private static float stockOf(EconomyState state, Resource resource) {
        for (EconomyState.StockState stock : state.inventory) {
            if (resource.getId().equals(stock.goodId)) return stock.amount;
        }
        return 0f;
    }
}
