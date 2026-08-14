package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Data-only description of one special-building unlock. Two flavours:
 * DISCOVERY (a goal you grow into, e.g. "plant 20 flowers") and RECIPE (a
 * combination of objects that must be present at once, e.g. "house +
 * garden + fence"). Every requirement must be met for the unlock to fire;
 * the reward is a hidden BuildingType that becomes placeable and stays
 * unlocked permanently once DiscoveryManager records it. Instances are
 * immutable and registered once in UnlockCatalog.
 */
public class UnlockDefinition {

    public enum Kind { DISCOVERY, RECIPE }

    private final String id;
    private final Kind kind;
    private final String rewardBuildingId;
    private final Array<UnlockRequirement> requirements;

    public UnlockDefinition(String id, Kind kind, String rewardBuildingId,
                            Array<UnlockRequirement> requirements) {
        this.id = id;
        this.kind = kind;
        this.rewardBuildingId = rewardBuildingId;
        this.requirements = new Array<>(requirements);
    }

    /** True when every requirement is met by the current placed counts. */
    public boolean isMet(ObjectMap<String, Integer> counts) {
        for (UnlockRequirement requirement : requirements) {
            if (!requirement.isMet(counts)) return false;
        }
        return true;
    }

    /** Overall progress, the average of its requirements, in [0, 1]. */
    public float getProgress(ObjectMap<String, Integer> counts) {
        if (requirements.size == 0) return 1f;
        float total = 0f;
        for (UnlockRequirement requirement : requirements) {
            total += requirement.getProgress(counts);
        }
        return total / requirements.size;
    }

    public String getId() { return id; }
    public Kind getKind() { return kind; }
    public String getRewardBuildingId() { return rewardBuildingId; }
    public Array<UnlockRequirement> getRequirements() { return new Array<>(requirements); }
}
