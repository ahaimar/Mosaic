package com.packs.mosaic.world.econ;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 16 — Investment Decisions. The settlement can fund one investment
 * project at a time (capacity expansion, storage expansion, research
 * endowment, worker education, export agreement), each presented with its
 * cost, expected benefit, payback period, risk and long-term effect. An
 * investment pays off as a permanent bonus once fully funded, can never be
 * taken twice, and competes with construction and research for the same
 * treasury — so the player has to plan rather than buy everything at once.
 */
class InvestmentTest {

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
    void catalogPresentsOneInvestmentPerCategoryWithPlanningData() {
        List<Investment> all = InvestmentCatalog.all();
        assertEquals(5, all.size(), "one investment per planning category");
        Set<InvestmentCategory> categories = new HashSet<>();
        for (Investment investment : all) {
            assertNotNull(investment.getId());
            assertEquals("investment." + investment.getId(), investment.getNameKey());
            assertEquals("investment." + investment.getId() + ".effect", investment.getEffectKey());
            assertTrue(investment.getCost() > 0f, "every investment costs something");
            assertTrue(investment.getBenefitPerTick() > 0f, "every investment pays a benefit");
            assertTrue(investment.getRisk() >= 0f && investment.getRisk() < 1f, "risk is a discount, not a loss");
            assertTrue(investment.getEffectMagnitude() > 0f, "every investment grants a long-term effect");
            float expected = investment.getBenefitPerTick() * (1f - investment.getRisk());
            assertEquals(expected, investment.getExpectedBenefit(), EPS, "risk scales the expected benefit");
            assertEquals((float) Math.ceil(investment.getCost() / expected),
                (float) investment.getPaybackPeriod(), EPS, "payback = cost ÷ expected benefit, rounded up");
            categories.add(investment.getCategory());
        }
        assertEquals(5, categories.size(), "one investment per category");
        assertNotNull(InvestmentCatalog.get("capacity_expansion"));
        assertEquals(InvestmentCatalog.get(null), null, "unknown ids resolve to null");
    }

    @Test
    void riskyDealsRepayLaterThanSafeOnes() {
        Investment safe = InvestmentCatalog.get("capacity_expansion");
        Investment risky = InvestmentCatalog.get("export_agreement");
        assertTrue(risky.getRisk() > safe.getRisk());
        assertTrue(risky.getExpectedBenefit() < risky.getBenefitPerTick(), "risk discounts the benefit");
        assertEquals(371, safe.getPaybackPeriod());
        assertEquals(391, risky.getPaybackPeriod(), "the risky deal pays back strictly later");
    }

    @Test
    void investmentFundsOverTimeAndCompletesWhenPaid() {
        EconomySimulation sim = new EconomySimulation(state(8f, 500f, Resource.FOOD, 300f));
        assertFalse(sim.isInvesting());
        assertTrue(sim.startInvestment("capacity_expansion"));
        assertTrue(sim.isInvesting());
        assertEquals("capacity_expansion", sim.getActiveInvestmentId());
        assertEquals(400f, sim.getInvestmentRemainingCost(), EPS);
        assertEquals(0f, sim.getInvestmentProgress(), EPS);

        for (int i = 0; i < 66; i++) sim.tick();
        assertTrue(sim.isInvesting(), "400 coins at 6 per tick take 67 ticks");
        assertEquals(400f - 66f * 6f, sim.getInvestmentRemainingCost(), EPS);
        assertEquals(6f, sim.getInvestmentInvestedThisTick(), EPS);
        assertEquals(66f * 6f / 400f, sim.getInvestmentProgress(), EPS);

        sim.tick();
        assertFalse(sim.isInvesting(), "the investment completes once fully paid");
        assertTrue(sim.isInvestmentCompleted("capacity_expansion"));
        assertEquals(0f, sim.getInvestmentRemainingCost(), EPS);
        assertEquals(0f, sim.getInvestmentProgress(), EPS);
        assertEquals(0.1f, sim.getInvestmentProductionBonus(), EPS, "capacity +10% is applied");
        assertFalse(sim.startInvestment("capacity_expansion"), "an investment can only be taken once");
    }

    @Test
    void investmentIsFundedFromTheTreasuryAndPausesWhenBroke() {
        // FOOD 40 is exactly the storage limit, so nothing overflows and sells:
        // the treasury holds only the 12 coins and stays empty after funding.
        EconomySimulation sim = new EconomySimulation(state(8f, 12f, Resource.FOOD, 40f));
        assertTrue(sim.startInvestment("storage_expansion"));
        for (int i = 0; i < 2; i++) sim.tick();
        assertTrue(sim.isInvesting());
        assertEquals(300f - 12f, sim.getInvestmentRemainingCost(), EPS,
            "only 12 coins were on hand, the rest waits for the treasury to refill");
        for (int i = 0; i < 5; i++) sim.tick();
        assertEquals(300f - 12f, sim.getInvestmentRemainingCost(), EPS,
            "an empty treasury stalls the investment");
        assertTrue(sim.isInvesting(), "the project stays active, not cancelled");
    }

    @Test
    void onlyOneInvestmentAtATimeAndEachOnce() {
        EconomySimulation sim = new EconomySimulation(state(8f, 600f, Resource.FOOD, 300f));
        assertFalse(sim.startInvestment(null));
        assertFalse(sim.startInvestment("mystery_deal"), "unknown ids are rejected");
        assertFalse(sim.canStartInvestment("mystery_deal"));

        assertTrue(sim.startInvestment("capacity_expansion"));
        assertFalse(sim.startInvestment("storage_expansion"), "one investment at a time");
        assertFalse(sim.canStartInvestment("storage_expansion"));
        assertFalse(sim.canStartInvestment("capacity_expansion"), "already active");

        for (int i = 0; i < 67; i++) sim.tick();
        assertTrue(sim.isInvestmentCompleted("capacity_expansion"));
        assertFalse(sim.canStartInvestment("capacity_expansion"), "already completed");

        assertTrue(sim.startInvestment("storage_expansion"), "a different investment can follow");
        assertEquals("storage_expansion", sim.getActiveInvestmentId());
    }

    @Test
    void productionInvestmentRaisesOutputPerBatch() {
        EconomyState base = state(8f, 500f, Resource.WOOD, 30f, Resource.STONE, 10f, Resource.FOOD, 100f);
        EconomySimulation control = new EconomySimulation(base);
        control.addBuilding("workshop", true);

        EconomyState boosted = state(8f, 500f, Resource.WOOD, 30f, Resource.STONE, 10f, Resource.FOOD, 100f);
        boosted.completedInvestments.add("capacity_expansion");
        EconomySimulation invested = new EconomySimulation(boosted);
        invested.addBuilding("workshop", true);

        float controlProduced = 0f;
        float investedProduced = 0f;
        for (int i = 0; i < 10; i++) {
            control.tick();
            invested.tick();
            controlProduced += control.getProduced(Resource.TOOLS);
            investedProduced += invested.getProduced(Resource.TOOLS);
        }
        assertEquals(10f, controlProduced, EPS);
        assertEquals(11f, investedProduced, EPS, "+10% output per batch");
    }

    @Test
    void storageInvestmentAddsFlatCapacity() {
        EconomyState base = state(8f, 500f, Resource.FOOD, 100f);
        EconomySimulation control = new EconomySimulation(base);
        control.addBuilding("warehouse", true);

        EconomyState boosted = state(8f, 500f, Resource.FOOD, 100f);
        boosted.completedInvestments.add("storage_expansion");
        EconomySimulation invested = new EconomySimulation(boosted);
        invested.addBuilding("warehouse", true);

        assertEquals(300f, control.getStorageCapacity(), EPS, "base 200 + one 100-capacity warehouse");
        assertEquals(400f, invested.getStorageCapacity(), EPS, "the +100 flat bonus stacks with warehouses");
        assertEquals(100f, invested.getInvestmentStorageBonus(), EPS);
    }

    @Test
    void researchInvestmentDiscountsTheNextProject() {
        EconomyState boosted = state(8f, 500f, Resource.FOOD, 100f);
        boosted.completedInvestments.add("research_endowment");
        EconomySimulation invested = new EconomySimulation(boosted);
        assertTrue(invested.startResearch());
        assertEquals(0.1f, invested.getInvestmentResearchDiscount(), EPS);
        assertEquals(180f, invested.getResearchRemainingCost(), EPS, "semi-automated costs 200, −10%");
        assertEquals(10f, invested.getResearchRemainingTicks(), EPS);
    }

    @Test
    void workforceInvestmentRaisesWorkingPopulation() {
        EconomyState base = state(20f, 500f, Resource.FOOD, 200f);
        EconomySimulation control = new EconomySimulation(base);

        EconomyState boosted = state(20f, 500f, Resource.FOOD, 200f);
        boosted.completedInvestments.add("worker_education");
        EconomySimulation invested = new EconomySimulation(boosted);

        control.tick();
        invested.tick();
        assertEquals(10f, control.getWorkingPopulation(), EPS, "half of 20 works by default");
        assertEquals(11f, invested.getWorkingPopulation(), EPS, "+5% of the population joins the workforce");
    }

    @Test
    void marketInvestmentRaisesSaleRevenue() {
        // No buildings, identical seeds: both markets sell the same volume of
        // furniture at the same price on the first tick, so the whole revenue
        // ratio is the export-agreement bonus.
        EconomyState base = state(8f, 500f, Resource.FOOD, 40f, Resource.FURNITURE, 50f);
        EconomySimulation control = new EconomySimulation(base);

        EconomyState boosted = state(8f, 500f, Resource.FOOD, 40f, Resource.FURNITURE, 50f);
        boosted.completedInvestments.add("export_agreement");
        EconomySimulation invested = new EconomySimulation(boosted);

        control.tick();
        invested.tick();
        assertEquals(control.getSold(Resource.FURNITURE), invested.getSold(Resource.FURNITURE), EPS,
            "identical sales volume");
        assertEquals(control.getPrice(Resource.FURNITURE), invested.getPrice(Resource.FURNITURE), EPS,
            "identical market price");
        assertTrue(control.getRevenue() > 0f, "something sold");
        assertEquals(control.getRevenue() * 1.05f, invested.getRevenue(), EPS,
            "the export agreement lifts every sale by 5%");
    }

    @Test
    void saveAndRestoreKeepCompletedAndActiveInvestments() {
        EconomySimulation sim = new EconomySimulation(state(8f, 1000f, Resource.FOOD, 300f));
        assertTrue(sim.startInvestment("capacity_expansion"));
        for (int i = 0; i < 67; i++) sim.tick();
        assertTrue(sim.startInvestment("worker_education"));
        for (int i = 0; i < 30; i++) sim.tick();

        EconomyState saved = sim.toState();
        assertTrue(saved.completedInvestments.contains("capacity_expansion", false));
        assertEquals("worker_education", saved.activeInvestmentId);
        assertEquals(450f - 30f * 6f, saved.investmentRemainingCost, EPS);

        EconomySimulation restored = new EconomySimulation(saved);
        assertTrue(restored.isInvestmentCompleted("capacity_expansion"));
        assertEquals(0.1f, restored.getInvestmentProductionBonus(), EPS, "bonuses are recomputed from the ids");
        assertTrue(restored.isInvesting());
        assertEquals("worker_education", restored.getActiveInvestmentId());
        assertEquals(450f - 180f, restored.getInvestmentRemainingCost(), EPS);
        assertFalse(restored.canStartInvestment("storage_expansion"),
            "the restored in-progress project still blocks new investments");

        EconomyState corrupt = saved;
        corrupt.completedInvestments.add("mystery_deal");
        corrupt.completedInvestments.add("capacity_expansion");
        corrupt.activeInvestmentId = "mystery_deal";
        corrupt.investmentRemainingCost = 999f;
        EconomySimulation clamped = new EconomySimulation(corrupt);
        assertEquals(0.1f, clamped.getInvestmentProductionBonus(), EPS,
            "duplicate and unknown ids never double an effect");
        assertEquals(0f, clamped.getInvestmentWorkforceBonus(), EPS, "the untouched investment grants nothing");
        assertFalse(clamped.isInvesting(), "an active investment with an unknown id is dropped");
    }
}
