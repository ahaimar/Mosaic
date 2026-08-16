package com.packs.mosaic.world.econ;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 15 — Technology and Productivity. The settlement progresses through
 * the five tiers Manual → Semi-Automated → Automated → Robotic → Advanced
 * Technology by funding research projects (coins over time, like
 * construction). Each unlocked tier carries cumulative bonuses for production
 * speed, worker productivity, resource efficiency, energy efficiency, storage
 * efficiency, product quality and transportation efficiency — every factor is
 * 1 at Manual, so the baseline stays untouched and the tier's bonuses show up
 * as measurable, long-term gains everywhere they should.
 */
class TechnologyTest {

    private static final float EPS = 0.001f;

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

    @Test
    void manualIsTheNeutralBaseline() {
        EconomySimulation sim = new EconomySimulation();
        assertEquals(1, sim.getTechLevel());
        assertEquals("tech.manual", sim.getTechnologyNameKey());
        assertEquals("manual", sim.getTechnology().getId());
        assertFalse(sim.isMaxTechnology());
        assertFalse(sim.isResearching());
        assertEquals(0f, sim.getResearchProgress(), EPS);
        assertEquals(0f, sim.getResearchInvestedThisTick(), EPS);
        assertEquals(1f, sim.getProductivityFactor(), EPS);
        assertEquals(1f, sim.getProductionSpeedFactor(), EPS);
        assertEquals(1f, sim.getWorkerProductivityFactor(), EPS);
        assertEquals(1f, sim.getResourceEfficiencyFactor(), EPS);
        assertEquals(1f, sim.getEnergyEfficiencyFactor(), EPS);
        assertEquals(1f, sim.getStorageEfficiencyFactor(), EPS);
        assertEquals(1f, sim.getProductQualityFactor(), EPS);
        assertEquals(1f, sim.getTransportEfficiencyFactor(), EPS);
    }

    @Test
    void catalogCoversTheFiveTiersInOrder() {
        assertEquals(5, TechnologyCatalog.all().size());
        assertEquals(5, TechnologyCatalog.maxLevel());
        String[] ids = {"manual", "semi", "automated", "robotic", "advanced"};
        for (int level = 1; level <= 5; level++) {
            Technology tier = TechnologyCatalog.get(level);
            assertEquals(level, tier.getLevel());
            assertEquals(ids[level - 1], tier.getId());
            assertEquals("tech." + ids[level - 1], tier.getNameKey());
        }
        assertEquals(2, TechnologyCatalog.get("semi").getLevel());
        assertEquals(1, TechnologyCatalog.get("unknown_id").getLevel(), "unknown ids fall back to Manual");
        assertEquals(5, TechnologyCatalog.get(99).getLevel(), "lookups clamp to the highest tier");
    }

    @Test
    void factorGettersMatchTheTierCumulativeBonuses() {
        for (int level = 1; level <= 5; level++) {
            EconomySimulation sim = new EconomySimulation();
            sim.setTechnologyLevel(level);
            Technology tier = TechnologyCatalog.get(level);
            assertEquals(tier.throughputFactor(), sim.getProductivityFactor(), EPS, "tier " + level);
            assertEquals(tier.speedFactor(), sim.getProductionSpeedFactor(), EPS, "tier " + level);
            assertEquals(tier.productivityFactor(), sim.getWorkerProductivityFactor(), EPS, "tier " + level);
            assertEquals(tier.resourceFactor(), sim.getResourceEfficiencyFactor(), EPS, "tier " + level);
            assertEquals(tier.energyFactor(), sim.getEnergyEfficiencyFactor(), EPS, "tier " + level);
            assertEquals(tier.storageFactor(), sim.getStorageEfficiencyFactor(), EPS, "tier " + level);
            assertEquals(tier.qualityFactor(), sim.getProductQualityFactor(), EPS, "tier " + level);
            assertEquals(tier.transportFactor(), sim.getTransportEfficiencyFactor(), EPS, "tier " + level);
        }
        EconomySimulation advanced = new EconomySimulation();
        advanced.setTechnologyLevel(5);
        assertEquals(3f, advanced.getProductivityFactor(), EPS, "speed × productivity at Advanced");
        assertEquals(0.8f, advanced.getResourceEfficiencyFactor(), EPS);
        assertEquals(0.8f, advanced.getEnergyEfficiencyFactor(), EPS);
        assertEquals(1.4f, advanced.getStorageEfficiencyFactor(), EPS);
        assertEquals(1.2f, advanced.getProductQualityFactor(), EPS);
        assertEquals(1.4f, advanced.getTransportEfficiencyFactor(), EPS);
    }

    @Test
    void researchRequiresBothCoinsAndTime() {
        // No buildings: money only leaves via research funding (4/tick). The
        // 200-coin project funds over 50 ticks, then needs 10 more ticks to
        // finish, unlocking Semi-Automated at tick 60.
        EconomySimulation sim = new EconomySimulation(state(3f, 500f, Resource.FOOD, 100f));
        assertTrue(sim.startResearch());
        assertTrue(sim.isResearching());
        assertEquals(200f, sim.getResearchRemainingCost(), EPS);
        assertEquals(10f, sim.getResearchRemainingTicks(), EPS);
        for (int i = 0; i < 25; i++) sim.tick();
        assertEquals(100f, sim.getResearchRemainingCost(), EPS, "four coins per tick invested");
        assertEquals(10f, sim.getResearchRemainingTicks(), EPS, "the price is not yet paid");
        assertEquals(1, sim.getTechLevel());
        for (int i = 0; i < 25; i++) sim.tick();
        assertEquals(0f, sim.getResearchRemainingCost(), EPS, "the full price is paid at tick 50");
        for (int i = 0; i < 10; i++) sim.tick();
        assertEquals(2, sim.getTechLevel(), "Semi-Automated unlocks once the ticks elapse");
        assertEquals("tech.semi", sim.getTechnologyNameKey());
        assertFalse(sim.isResearching());
        assertEquals(0f, sim.getResearchRemainingCost(), EPS);
        assertEquals(0f, sim.getResearchRemainingTicks(), EPS);
    }

    @Test
    void researchProgressMeasuresCoinEquivalentWork() {
        EconomySimulation sim = new EconomySimulation(state(3f, 500f, Resource.FOOD, 100f));
        assertEquals(0f, sim.getResearchProgress(), EPS, "idle means no progress");
        sim.startResearch();
        for (int i = 0; i < 25; i++) sim.tick();
        float expected = 100f / (200f + 10f * 4f);
        assertEquals(expected, sim.getResearchProgress(), EPS, "100 coins of 240 total");
        EconomySimulation maxed = new EconomySimulation();
        maxed.setTechnologyLevel(5);
        assertEquals(1f, maxed.getResearchProgress(), EPS, "maxed out is done");
    }

    @Test
    void researchPausesWithoutTreasuryFunds() {
        // Only 100 coins to start and nothing to sell (no goods seeded): after
        // 25 ticks the treasury is empty and half the project is still unpaid,
        // so the level never advances.
        EconomySimulation sim = new EconomySimulation(state(3f, 100f));
        sim.startResearch();
        for (int i = 0; i < 100; i++) sim.tick();
        assertEquals(1, sim.getTechLevel(), "no tier unlocked without the coins");
        assertTrue(sim.isResearching(), "the unpaid project stays open");
        assertEquals(100f, sim.getResearchRemainingCost(), EPS);
        assertEquals(10f, sim.getResearchRemainingTicks(), EPS);
        assertEquals(0f, sim.getResearchInvestedThisTick(), EPS, "nothing invested once broke");
    }

    @Test
    void cannotStartResearchTwiceOrBeyondMax() {
        EconomySimulation sim = new EconomySimulation();
        assertTrue(sim.startResearch());
        assertFalse(sim.startResearch(), "one project at a time");
        sim.setTechnologyLevel(99);
        assertTrue(sim.isMaxTechnology(), "clamped to the top tier");
        assertFalse(sim.startResearch(), "nothing to research at max");
    }

    @Test
    void productionSpeedFinishesBatchesFaster() {
        // A 2-tick smelter: Manual completes its first batch at tick 2 while
        // Advanced Technology doubles the batch progress and completes at
        // tick 1, producing twice as much steel over the same two ticks.
        float manualSteel = producedSteel(1);
        float advancedSteel = producedSteel(5);
        assertEquals(1f, manualSteel, EPS, "manual: one batch in two ticks");
        assertEquals(2f, advancedSteel, EPS, "advanced: one batch every tick");
        assertEquals(2f, advancedSteel / manualSteel, EPS, "production speed × productivity");
    }

    private static float producedSteel(int techLevel) {
        EconomySimulation sim = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 10f, Resource.STONE, 10f, Resource.IRON, 30f,
            Resource.COAL, 30f, Resource.FOOD, 100f));
        sim.setTechnologyLevel(techLevel);
        sim.addBuilding("smelter", true);
        float steel = 0f;
        for (int i = 0; i < 2; i++) {
            sim.tick();
            steel += sim.getProduced(Resource.STEEL);
        }
        return steel;
    }

    @Test
    void resourceEfficiencyConsumesFewerInputs() {
        // One staffed workshop makes one tool per tick at any tier (the batch
        // slot caps the rate), but Advanced Technology needs only 0.8 wood per
        // batch: ten tools cost 10 wood at Manual and 8 at Advanced.
        float manualWood = woodConsumedPerTool(1);
        float advancedWood = woodConsumedPerTool(5);
        assertEquals(1f, manualWood, EPS, "manual uses one wood per tool");
        assertEquals(0.8f, advancedWood, EPS, "advanced wastes 20% less wood");
        assertTrue(advancedWood < manualWood, "resource efficiency really saves inputs");
    }

    private static float woodConsumedPerTool(int techLevel) {
        EconomySimulation sim = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 30f, Resource.STONE, 10f, Resource.FOOD, 100f));
        sim.setTechnologyLevel(techLevel);
        sim.addBuilding("workshop", true);
        float wood = 0f;
        float tools = 0f;
        for (int i = 0; i < 10; i++) {
            sim.tick();
            wood += sim.getConsumed(Resource.WOOD);
            tools += sim.getProduced(Resource.TOOLS);
        }
        assertEquals(10f, tools, EPS, "slot-bounded rate is one tool per tick");
        return wood / tools;
    }

    @Test
    void energyEfficiencyReducesGridDraw() {
        EconomySimulation manual = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 30f, Resource.STONE, 10f, Resource.FOOD, 100f));
        manual.addBuilding("workshop", true);
        manual.tick();
        EconomySimulation advanced = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 30f, Resource.STONE, 10f, Resource.FOOD, 100f));
        advanced.setTechnologyLevel(5);
        advanced.addBuilding("workshop", true);
        advanced.tick();
        assertEquals(1f, manual.getEnergyConsumption(), EPS, "workshop draws one unit");
        assertEquals(0.8f, advanced.getEnergyConsumption(), EPS, "advanced draws 20% less");
    }

    @Test
    void storageEfficiencyScalesWarehouseCapacity() {
        EconomySimulation sim = new EconomySimulation();
        sim.addBuilding("warehouse", true);
        assertEquals(300f, sim.getStorageCapacity(), EPS, "base 200 + one 100-capacity warehouse");
        sim.setTechnologyLevel(5);
        assertEquals(340f, sim.getStorageCapacity(), EPS, "200 + 100 × 1.4 storage efficiency");
        assertEquals(140f, sim.getStorageEfficiencyFactor() * 100f, EPS, "storage factor sanity");
    }

    @Test
    void qualityEfficiencyRaisesSaleRevenue() {
        // No buildings, identical seeds: both markets sell the same volume of
        // furniture at the same price on the first tick, so the whole revenue
        // ratio is the quality factor.
        EconomySimulation manual = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 10f, Resource.STONE, 10f, Resource.FOOD, 40f,
            Resource.FURNITURE, 50f));
        EconomySimulation advanced = new EconomySimulation(state(8f, 500f,
            Resource.WOOD, 10f, Resource.STONE, 10f, Resource.FOOD, 40f,
            Resource.FURNITURE, 50f));
        advanced.setTechnologyLevel(5);
        manual.tick();
        advanced.tick();
        assertEquals(manual.getSold(Resource.FURNITURE), advanced.getSold(Resource.FURNITURE), EPS,
            "identical sales volume");
        assertEquals(manual.getPrice(Resource.FURNITURE), advanced.getPrice(Resource.FURNITURE), EPS,
            "identical market price");
        assertTrue(manual.getRevenue() > 0f, "something sold");
        assertEquals(manual.getRevenue() * 1.2f, advanced.getRevenue(), EPS,
            "better quality sells for 20% more");
    }

    @Test
    void transportEfficiencyRaisesRouteCapacity() {
        EconomySimulation sim = new EconomySimulation(state(8f, 100f,
            Resource.WOOD, 40f, Resource.STONE, 30f, Resource.COAL, 30f, Resource.FOOD, 40f));
        sim.addBuilding("iron_mine", true);
        sim.addBuilding("truck_depot", true);
        sim.addRoute("iron_mine", Resource.IRON, 2);
        assertEquals(15f, sim.getRouteCapacity("iron_mine", Resource.IRON), EPS,
            "two assigned trucks × the boosted 7.5 capacity");
        sim.setTechnologyLevel(5);
        assertEquals(21f, sim.getRouteCapacity("iron_mine", Resource.IRON), EPS,
            "the same trip carries 40% more at Advanced Technology");
    }

    @Test
    void saveAndRestoreKeepTierAndResearch() {
        EconomySimulation sim = new EconomySimulation(state(3f, 500f, Resource.FOOD, 100f));
        assertEquals(1, sim.getTechLevel(), "fresh saves restore to Manual");
        sim.setTechnologyLevel(4);
        assertTrue(sim.startResearch(), "robotic → advanced project");
        for (int i = 0; i < 10; i++) sim.tick();
        EconomyState saved = sim.toState();
        assertEquals(4, saved.techLevel);
        assertEquals(2500f - 40f, saved.researchRemainingCost, EPS);
        assertEquals(30f, saved.researchRemainingTicks, EPS);

        EconomySimulation restored = new EconomySimulation(saved);
        assertEquals(4, restored.getTechLevel());
        assertEquals("tech.robotic", restored.getTechnologyNameKey());
        assertEquals(2500f - 40f, restored.getResearchRemainingCost(), EPS);
        assertEquals(30f, restored.getResearchRemainingTicks(), EPS);
        assertTrue(restored.isResearching());
        assertNotNull(restored.getTechnology());

        EconomyState corrupt = saved;
        corrupt.techLevel = 9;
        EconomySimulation clamped = new EconomySimulation(corrupt);
        assertEquals(1, clamped.getTechLevel(), "invalid tiers fall back to Manual");
    }
}
