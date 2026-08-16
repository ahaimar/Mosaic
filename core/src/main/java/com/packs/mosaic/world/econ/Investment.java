package com.packs.mosaic.world.econ;

/**
 * One investment opportunity (Task 16 — Investment Decisions). Every
 * investment is presented to the player with the five planning attributes the
 * spec requires:
 *
 * <ul>
 *   <li><b>Cost</b> — coins paid from the treasury over time while the
 *       investment is active.</li>
 *   <li><b>Expected benefit</b> — the projected coins the investment earns
 *       per tick once it is complete.</li>
 *   <li><b>Payback period</b> — cost ÷ expected benefit: how long the benefit
 *       takes to repay the investment.</li>
 *   <li><b>Risk</b> — how likely the benefit is to underperform; the expected
 *       benefit and payback period are already scaled by it, so a cheap risky
 *       deal is not automatically better than a safe expensive one.</li>
 *   <li><b>Long-term effect</b> — the permanent mechanical bonus granted when
 *       the investment finishes, applied through the investment's category.</li>
 * </ul>
 *
 * <p>The economy is deliberately deterministic, so risk is not a random roll:
 * it discounts the benefit figures the player plans with. The trade-off a
 * risky option carries is real — a higher-risk investment must offer a bigger
 * benefit to break even.
 */
public final class Investment {

    private final String id;
    private final InvestmentCategory category;
    private final float cost;
    private final float benefitPerTick;
    private final float risk;
    private final float effectMagnitude;

    /**
     * @param id              unique catalog id, also the base of the i18n keys
     * @param category        the effect track this investment pays into
     * @param cost            coins owed while the investment is active
     * @param benefitPerTick  nominal coins the investment is projected to earn
     *                        per tick once complete (before risk)
     * @param risk            0..1; scales the expected benefit and payback
     * @param effectMagnitude the permanent bonus applied on completion:
     *                        fraction for PRODUCTION/TECHNOLOGY/MARKETS,
     *                        flat capacity for INFRASTRUCTURE, share for
     *                        WORKFORCE
     */
    public Investment(String id, InvestmentCategory category, float cost,
                      float benefitPerTick, float risk, float effectMagnitude) {
        this.id = id;
        this.category = category;
        this.cost = Math.max(0f, cost);
        this.benefitPerTick = Math.max(0f, benefitPerTick);
        this.risk = Math.max(0f, Math.min(1f, risk));
        this.effectMagnitude = effectMagnitude;
    }

    public String getId() { return id; }

    public InvestmentCategory getCategory() { return category; }

    /** i18n key for the investment's name, e.g. {@code investment.capacity_expansion}. */
    public String getNameKey() { return "investment." + id; }

    /** i18n key for the investment's long-term effect, e.g. {@code investment.capacity_expansion.effect}. */
    public String getEffectKey() { return "investment." + id + ".effect"; }

    public float getCost() { return cost; }

    public float getBenefitPerTick() { return benefitPerTick; }

    /** 0..1 — how likely the benefit is to underperform. */
    public float getRisk() { return risk; }

    /** The permanent bonus applied when the investment completes. */
    public float getEffectMagnitude() { return effectMagnitude; }

    /** Expected coins per tick after the investment completes (risk-adjusted). */
    public float getExpectedBenefit() { return benefitPerTick * (1f - risk); }

    /** Payback period in ticks: cost ÷ expected benefit, rounded up. */
    public int getPaybackPeriod() {
        float expected = getExpectedBenefit();
        return expected <= 0f ? Integer.MAX_VALUE : (int) Math.ceil(cost / expected);
    }
}
