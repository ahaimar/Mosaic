package com.packs.mosaic.world.econ;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 18 — Economic Development Levels. The settlement advances through six
 * stages (Small Settlement → Village → Town → Industrial Town → City → Major
 * Economic Center), each gated by eight measurable conditions — population,
 * cumulative production, employment rate, active infrastructure, cumulative
 * revenue, technology tier, housing capacity and cumulative market activity.
 * Money alone never unlocks a stage; the level rises automatically when every
 * threshold of the next stage is met.
 */
class DevelopmentTest {

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
    void catalogCoversTheSixStagesInOrder() {
        assertEquals(6, DevelopmentCatalog.all().size());
        assertEquals(6, DevelopmentCatalog.maxLevel());
        String[] ids = {"small_settlement", "village", "town",
            "industrial_town", "city", "major_economic_center"};
        for (int level = 1; level <= 6; level++) {
            DevelopmentLevel stage = DevelopmentCatalog.get(level);
            assertEquals(level, stage.getLevel());
            assertEquals(ids[level - 1], stage.getId());
            assertEquals("development." + ids[level - 1], stage.getNameKey());
            assertNotNull(stage);
        }
        assertEquals(2, DevelopmentCatalog.get("village").getLevel());
        assertEquals(6, DevelopmentCatalog.get(99).getLevel(), "lookups clamp to the highest stage");
        assertEquals(1, DevelopmentCatalog.get(0).getLevel(), "lookups clamp to the first stage");
        assertEquals(null, DevelopmentCatalog.get("unknown_id"), "unknown ids resolve to null");
    }

    @Test
    void smallSettlementHasNoRequirements() {
        DevelopmentLevel stage = DevelopmentCatalog.get(1);
        assertEquals(1, stage.getLevel());
        assertEquals("small_settlement", stage.getId());
        for (DevelopmentCondition condition : DevelopmentCondition.values()) {
            assertEquals(0f, stage.required(condition), EPS, "the starting stage blocks nothing");
        }
    }

    @Test
    void thresholdsAreMonotonic() {
        for (DevelopmentCondition condition : DevelopmentCondition.values()) {
            for (int level = 1; level < DevelopmentCatalog.maxLevel(); level++) {
                assertTrue(DevelopmentCatalog.get(level + 1).required(condition)
                        >= DevelopmentCatalog.get(level).required(condition),
                    condition.getId() + " grows monotonically toward level " + (level + 1));
            }
        }
    }

    @Test
    void moneyAloneNeverAdvancesDevelopment() {
        EconomySimulation sim = new EconomySimulation(state(3f, 100000f, Resource.FOOD, 300f));
        for (int i = 0; i < 5; i++) sim.tick();
        assertEquals(1, sim.getDevelopmentLevel(), "a rich but empty settlement stays a small settlement");
        assertFalse(sim.isMaxDevelopment());
        assertTrue(sim.getDevelopmentProgress() < 1f);
    }

    @Test
    void conditionsAreReadFromRealMetrics() {
        EconomySimulation sim = new EconomySimulation(state(10f, 500f,
            Resource.WOOD, 20f, Resource.STONE, 10f, Resource.FOOD, 100f,
            Resource.FURNITURE, 50f));
        sim.addBuilding("lumber_hut", true);
        sim.addBuilding("small_house", true);
        assertEquals(10f, sim.currentValue(DevelopmentCondition.POPULATION), EPS);
        assertEquals(2f, sim.currentValue(DevelopmentCondition.INFRASTRUCTURE), EPS,
            "lumber hut + house, both active");
        assertEquals(2f, sim.currentValue(DevelopmentCondition.HOUSING), EPS,
            "one small house holds two citizens");
        assertEquals(1f, sim.currentValue(DevelopmentCondition.TECHNOLOGY), EPS);
        assertEquals(0f, sim.currentValue(DevelopmentCondition.REVENUE), EPS,
            "nothing sold before the first tick");

        sim.tick();
        assertTrue(sim.currentValue(DevelopmentCondition.PRODUCTION) > 0f,
            "the lumber hut produced wood");
        assertTrue(sim.currentValue(DevelopmentCondition.EMPLOYMENT) > 0f,
            "the lumber hut hired a worker");
        assertTrue(sim.currentValue(DevelopmentCondition.REVENUE) > 0f,
            "furniture sales brought coins");
        assertTrue(sim.currentValue(DevelopmentCondition.MARKET_ACTIVITY) > 0f,
            "furniture sold on the market");
        assertEquals(1, sim.getDevelopmentLevel(), "too small to reach the village stage yet");
    }

    @Test
    void allConditionsMetAdvanceToTheNextStage() {
        EconomyState state = state(20f, 1000f, Resource.FOOD, 300f);
        state.lifetimeProduced = 100f;
        state.lifetimeSold = 50f;
        state.lifetimeRevenue = 300f;
        EconomySimulation sim = new EconomySimulation(state);
        sim.setTechnologyLevel(2);
        for (int i = 0; i < 8; i++) sim.addBuilding("workshop", true);
        for (int i = 0; i < 3; i++) sim.addBuilding("small_house", true);
        assertEquals(1, sim.getDevelopmentLevel());
        assertTrue(sim.currentValue(DevelopmentCondition.INFRASTRUCTURE) >= 5f);
        assertTrue(sim.currentValue(DevelopmentCondition.HOUSING) >= 3f);

        sim.tick();
        assertEquals(0.8f, sim.currentValue(DevelopmentCondition.EMPLOYMENT), EPS,
            "8 of 10 working-age citizens are employed");
        assertEquals(2, sim.getDevelopmentLevel(), "every village threshold is met");
        assertEquals("development.village", sim.getDevelopmentNameKey());
    }

    @Test
    void developmentAdvancesOneStagePerTick() {
        EconomyState state = state(20f, 1000f, Resource.FOOD, 300f);
        state.lifetimeProduced = 100f;
        state.lifetimeSold = 50f;
        state.lifetimeRevenue = 300f;
        EconomySimulation sim = new EconomySimulation(state);
        sim.setTechnologyLevel(2);
        for (int i = 0; i < 8; i++) sim.addBuilding("workshop", true);
        for (int i = 0; i < 3; i++) sim.addBuilding("small_house", true);
        sim.tick();
        assertEquals(2, sim.getDevelopmentLevel());
        sim.tick();
        assertEquals(2, sim.getDevelopmentLevel(),
            "one stage per tick at most — population 20 is far from town's 50");
    }

    @Test
    void developmentReachedTheFinalStageStops() {
        EconomySimulation sim = new EconomySimulation(state(8f, 500f, Resource.FOOD, 100f));
        sim.setDevelopmentLevel(6);
        assertTrue(sim.isMaxDevelopment());
        assertEquals(1f, sim.getDevelopmentProgress(), EPS);
        assertTrue(sim.getDevelopmentConditions().isEmpty(), "no thresholds remain at the final stage");
        sim.tick();
        assertEquals(6, sim.getDevelopmentLevel(), "the final stage never reverts");
    }

    @Test
    void conditionStatusReportsTheNextThresholds() {
        EconomySimulation sim = new EconomySimulation(state(20f, 1000f, Resource.FOOD, 300f));
        List<EconomySimulation.DevelopmentConditionStatus> statuses = sim.getDevelopmentConditions();
        assertEquals(8, statuses.size());
        DevelopmentLevel next = DevelopmentCatalog.get(2);
        for (EconomySimulation.DevelopmentConditionStatus status : statuses) {
            assertEquals(next.required(status.condition), status.required, EPS);
            assertEquals(sim.currentValue(status.condition), status.current, EPS);
            assertEquals(status.current >= status.required, status.met);
        }
    }

    @Test
    void employmentConditionHandlesEmptyWorkforce() {
        EconomySimulation sim = new EconomySimulation(state(0f, 500f, Resource.FOOD, 10f));
        assertEquals(0f, sim.currentValue(DevelopmentCondition.EMPLOYMENT), EPS,
            "an empty workforce yields no employment ratio");
    }

    @Test
    void saveAndRestoreKeepStageAndLifetimeMetrics() {
        EconomySimulation sim = new EconomySimulation(state(8f, 500f,
            Resource.FOOD, 100f, Resource.FURNITURE, 50f));
        sim.setDevelopmentLevel(4);
        for (int i = 0; i < 3; i++) sim.tick();
        EconomyState saved = sim.toState();
        assertEquals(4, saved.developmentLevel);
        assertTrue(saved.lifetimeRevenue > 0f, "lifetime metrics are saved");

        EconomySimulation restored = new EconomySimulation(saved);
        assertEquals(4, restored.getDevelopmentLevel());
        assertEquals("development.industrial_town", restored.getDevelopmentNameKey());
        assertEquals(saved.lifetimeProduced, restored.getLifetimeProduced(), EPS);
        assertEquals(saved.lifetimeSold, restored.getLifetimeSold(), EPS);
        assertEquals(saved.lifetimeRevenue, restored.getLifetimeRevenue(), EPS);

        EconomyState corrupt = saved;
        corrupt.developmentLevel = 99;
        EconomySimulation clamped = new EconomySimulation(corrupt);
        assertEquals(1, clamped.getDevelopmentLevel(), "invalid stages fall back to Small Settlement");
    }
}
