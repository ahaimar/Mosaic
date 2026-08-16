package com.packs.mosaic.world.econ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.packs.mosaic.world.econ.InvestmentCategory.INFRASTRUCTURE;
import static com.packs.mosaic.world.econ.InvestmentCategory.MARKETS;
import static com.packs.mosaic.world.econ.InvestmentCategory.PRODUCTION;
import static com.packs.mosaic.world.econ.InvestmentCategory.TECHNOLOGY;
import static com.packs.mosaic.world.econ.InvestmentCategory.WORKFORCE;

/**
 * The investment opportunities a settlement can fund (Task 16 — Investment
 * Decisions), one per category. Each is a genuine long-term commitment with a
 * measurable effect and a payback that scales with its risk, so the player has
 * to weigh cost against benefit instead of buying everything at once.
 */
public final class InvestmentCatalog {

    private static final Map<String, Investment> BY_ID = new LinkedHashMap<>();
    private static final List<Investment> ALL = new ArrayList<>();

    static {
        register(new Investment("capacity_expansion", PRODUCTION, 400f, 1.2f, 0.10f, 0.10f));
        register(new Investment("storage_expansion", INFRASTRUCTURE, 300f, 0.9f, 0.05f, 100f));
        register(new Investment("research_endowment", TECHNOLOGY, 350f, 1.0f, 0.05f, 0.10f));
        register(new Investment("worker_education", WORKFORCE, 450f, 1.5f, 0.15f, 0.05f));
        register(new Investment("export_agreement", MARKETS, 500f, 1.6f, 0.20f, 0.05f));
    }

    private InvestmentCatalog() {
    }

    private static void register(Investment investment) {
        BY_ID.put(investment.getId(), investment);
        ALL.add(investment);
    }

    /** The investment with the given id, or null when unknown. */
    public static Investment get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** Every investment, in registration order. */
    public static List<Investment> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The investments of one category (all investments are unique per category). */
    public static List<Investment> byCategory(InvestmentCategory category) {
        List<Investment> result = new ArrayList<>();
        for (Investment investment : ALL) {
            if (investment.getCategory() == category) result.add(investment);
        }
        return result;
    }
}
