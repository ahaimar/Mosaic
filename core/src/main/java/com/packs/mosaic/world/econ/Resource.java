package com.packs.mosaic.world.econ;

/**
 * The unified resource system (Task 6). Every resource is a first-class
 * quantity in the economic simulation: id, display name, category, base
 * price, unit and its own storage limit. The categories mirror the spec
 * (raw materials, components, energy, food, consumer goods, industrial
 * goods, finished products, money).
 *
 * Money itself is a resource (COINS): the simulation's treasury is just the
 * COINS quantity in its inventory, so revenue, operating costs and
 * construction investment are all ordinary resource flows.
 */
public enum Resource {

    // ── Raw materials ────────────────────────────────────────────────────
    WOOD("wood", "good.wood", Category.RAW_MATERIALS, 1.0f, "logs", 60f, 0.02f),
    STONE("stone", "good.stone", Category.RAW_MATERIALS, 1.2f, "blocks", 60f, 0.02f),
    IRON("iron", "good.iron", Category.RAW_MATERIALS, 1.5f, "ore", 60f, 0.015f),
    WOOL("wool", "good.wool", Category.RAW_MATERIALS, 2.0f, "bales", 40f, 0.05f),

    // ── Energy ───────────────────────────────────────────────────────────
    COAL("coal", "good.coal", Category.ENERGY, 1.4f, "coal", 60f, 0.02f),

    // ── Components ───────────────────────────────────────────────────────
    TOOLS("tools", "good.tools", Category.COMPONENTS, 2.5f, "tools", 40f, 0.06f),

    // ── Food ─────────────────────────────────────────────────────────────
    // The preference mirrors FOOD_PER_PERSON: meals every citizen wants to
    // eat each tick (the actual demand the population exerts on the market).
    FOOD("food", "good.food", Category.FOOD, 1.5f, "meals", 40f, 0.25f),

    // ── Consumer goods ───────────────────────────────────────────────────
    MILK("milk", "good.milk", Category.CONSUMER_GOODS, 2.2f, "litres", 30f, 0.07f),
    EGGS("eggs", "good.eggs", Category.CONSUMER_GOODS, 1.8f, "eggs", 30f, 0.06f),

    // ── Industrial goods ─────────────────────────────────────────────────
    STEEL("steel", "good.steel", Category.INDUSTRIAL_GOODS, 3.0f, "bars", 40f, 0.02f),

    // ── Finished products ────────────────────────────────────────────────
    FURNITURE("furniture", "good.furniture", Category.FINISHED_PRODUCTS, 4.0f, "pieces", 30f, 0.08f),

    // ── Money ────────────────────────────────────────────────────────────
    COINS("coins", "good.coins", Category.MONEY, 1.0f, "coins", 1000000f, 0f);

    /** The eight spec resource categories. */
    public enum Category {
        RAW_MATERIALS("raw_materials"),
        COMPONENTS("components"),
        ENERGY("energy"),
        FOOD("food"),
        CONSUMER_GOODS("consumer_goods"),
        INDUSTRIAL_GOODS("industrial_goods"),
        FINISHED_PRODUCTS("finished_products"),
        MONEY("money");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public String getNameKey() { return "category." + id; }
    }

    private final String id;
    private final String nameKey;
    private final Category category;
    private final float basePrice;
    private final String unit;
    private final float storageLimit;
    private final float consumerPreference;

    Resource(String id, String nameKey, Category category, float basePrice,
             String unit, float storageLimit, float consumerPreference) {
        this.id = id;
        this.nameKey = nameKey;
        this.category = category;
        this.basePrice = basePrice;
        this.unit = unit;
        this.storageLimit = storageLimit;
        this.consumerPreference = consumerPreference;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public Category getCategory() { return category; }
    /** Base market price in coins per unit. */
    public float getBasePrice() { return basePrice; }
    /** Display unit (data string, e.g. "logs", "litres", "coins"). */
    public String getUnit() { return unit; }
    /** Storage cap for this resource before surplus is exported. */
    public float getStorageLimit() { return storageLimit; }
    /**
     * How much a citizen wants this good each tick, at the base price in a
     * basic settlement. Part of the consumer demand driving the market.
     */
    public float getConsumerPreference() { return consumerPreference; }

    public boolean isMoney() {
        return category == Category.MONEY;
    }

    /**
     * A good the population buys on the market: components, consumer goods and
     * finished products. Food is eaten directly (never a market transaction),
     * and raw materials, energy and industrial inputs are not consumer goods —
     * they are fed into production or exported as surplus.
     */
    public boolean isConsumerGood() {
        return category == Category.COMPONENTS
            || category == Category.CONSUMER_GOODS
            || category == Category.FINISHED_PRODUCTS;
    }

    /** Resolves a resource id, or null if no such resource exists. */
    public static Resource byId(String id) {
        for (Resource resource : values()) {
            if (resource.getId().equals(id)) return resource;
        }
        return null;
    }
}
