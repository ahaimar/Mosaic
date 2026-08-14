package com.packs.mosaic.world.econ;

/**
 * A per-resource inventory ledger (Task 8 — Inventory and Warehousing). One
 * ledger backs both the settlement's shared stockpile and each producing
 * building's own stockpile. For every {@link Resource} it tracks the six
 * spec quantities:
 *
 * <ul>
 *   <li><b>Incoming</b> — units that entered the ledger (cumulative).</li>
 *   <li><b>Stored</b> — units currently on hand.</li>
 *   <li><b>Reserved</b> — units spoken for by an in-flight production batch.</li>
 *   <li><b>Consumed</b> — units used up (cumulative).</li>
 *   <li><b>Produced</b> — units created here (cumulative).</li>
 *   <li><b>Outgoing</b> — units shipped/exported out (cumulative).</li>
 * </ul>
 *
 * <p>The ledger also carries a storage {@code capacity} (the settlement's
 * capacity is the base storage plus every active warehouse's contribution)
 * and a {@code storageCostPerUnit} used to compute warehousing costs. A
 * reservation marks stock as spoken-for without removing it from the stored
 * quantity; {@link #fulfillReservation} turns a reservation into real
 * consumption at the moment a batch completes.
 */
public final class ResourceInventory {

    private final float[] stored = new float[Resource.values().length];
    private final float[] reserved = new float[Resource.values().length];
    private final float[] incoming = new float[Resource.values().length];
    private final float[] consumed = new float[Resource.values().length];
    private final float[] produced = new float[Resource.values().length];
    private final float[] outgoing = new float[Resource.values().length];
    private float capacity;
    private final float storageCostPerUnit;

    public ResourceInventory(float capacity, float storageCostPerUnit) {
        this.capacity = Math.max(0f, capacity);
        this.storageCostPerUnit = Math.max(0f, storageCostPerUnit);
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /** Marks stock as spoken-for by an in-flight batch (stored is untouched). */
    public void reserve(Resource resource, float amount) {
        reserved[resource.ordinal()] += amount;
    }

    /** Returns reserved stock to the free pool. */
    public void releaseReservation(Resource resource, float amount) {
        reserved[resource.ordinal()] = Math.max(0f, reserved[resource.ordinal()] - amount);
    }

    /**
     * Completes a reservation: the reserved stock is really consumed (stored
     * drops and the consumed counter rises). Used when a batch finishes.
     */
    public void fulfillReservation(Resource resource, float amount) {
        stored[resource.ordinal()] -= amount;
        releaseReservation(resource, amount);
        consumed[resource.ordinal()] += amount;
    }

    /** Records units that arrived to be processed (cumulative counter only). */
    public void markIncoming(Resource resource, float amount) {
        incoming[resource.ordinal()] += amount;
    }

    /** Records units actually consumed (cumulative counter only). */
    public void markConsumed(Resource resource, float amount) {
        consumed[resource.ordinal()] += amount;
    }

    /** Receives units from outside: stored grows, incoming counts them. */
    public void receive(Resource resource, float amount) {
        stored[resource.ordinal()] += amount;
        incoming[resource.ordinal()] += amount;
    }

    /** Receives units produced elsewhere: stored and both counters grow. */
    public void receiveProduced(Resource resource, float amount) {
        stored[resource.ordinal()] += amount;
        incoming[resource.ordinal()] += amount;
        produced[resource.ordinal()] += amount;
    }

    /** Places freshly produced units into the ledger. */
    public void produceInto(Resource resource, float amount) {
        stored[resource.ordinal()] += amount;
        produced[resource.ordinal()] += amount;
    }

    /** Ships units out of the ledger (never more than are stored). */
    public void shipOut(Resource resource, float amount) {
        float actual = Math.min(Math.max(0f, amount), stored[resource.ordinal()]);
        stored[resource.ordinal()] -= actual;
        outgoing[resource.ordinal()] += actual;
    }

    /** Consumes units on hand (e.g. food eaten, operating costs paid). */
    public void consume(Resource resource, float amount) {
        stored[resource.ordinal()] -= amount;
        consumed[resource.ordinal()] += amount;
    }

    public void setStored(Resource resource, float amount) {
        stored[resource.ordinal()] = Math.max(0f, amount);
    }

    public void setCapacity(float capacity) {
        this.capacity = Math.max(0f, capacity);
    }

    // ── Read-outs ─────────────────────────────────────────────────────────

    public float getStored(Resource resource) { return stored[resource.ordinal()]; }
    public float getReserved(Resource resource) { return reserved[resource.ordinal()]; }
    /** Free-on-hand stock, i.e. stored minus whatever is reserved. */
    public float getAvailable(Resource resource) {
        return stored[resource.ordinal()] - reserved[resource.ordinal()];
    }
    public float getIncoming(Resource resource) { return incoming[resource.ordinal()]; }
    public float getConsumed(Resource resource) { return consumed[resource.ordinal()]; }
    public float getProduced(Resource resource) { return produced[resource.ordinal()]; }
    public float getOutgoing(Resource resource) { return outgoing[resource.ordinal()]; }
    public float getCapacity() { return capacity; }
    public float getStorageCostPerUnit() { return storageCostPerUnit; }

    /** Total units of every resource on hand (money included). */
    public float getStoredTotal() {
        return sum(stored);
    }

    /** Total reserved units of every resource (money included). */
    public float getReservedTotal() {
        return sum(reserved);
    }

    /** Capacity still free: total capacity minus stored minus reserved. */
    public float getAvailableCapacity() {
        return capacity - getStoredTotal() - getReservedTotal();
    }

    private static float sum(float[] values) {
        float total = 0f;
        for (float value : values) total += value;
        return total;
    }
}
