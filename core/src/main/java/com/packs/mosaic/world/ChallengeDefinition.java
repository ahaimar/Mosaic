package com.packs.mosaic.world;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Data-only description of one challenge: how many of each BuildingType
 * (by id) must be present on the grid, plus the star reward for
 * completing it. Does not read the grid itself — ChallengeManager builds
 * placed counts and asks {@link #isSatisfiedBy}. Instances are immutable
 * and registered once in ChallengeCatalog.
 */
public class ChallengeDefinition {

    private final String id;
    private final String title;
    private final String description;
    private final int starReward;
    private final ObjectMap<String, Integer> requiredCounts;

    public ChallengeDefinition(String id, String title, String description,
                               int starReward, ObjectMap<String, Integer> requiredCounts) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.starReward = starReward;
        this.requiredCounts = new ObjectMap<>(requiredCounts);
    }

    /** True when every required type is present at or above its required count. */
    public boolean isSatisfiedBy(ObjectMap<String, Integer> placedCounts) {
        for (ObjectMap.Entry<String, Integer> entry : requiredCounts) {
            Integer have = placedCounts.get(entry.key);
            if (have == null || have < entry.value) return false;
        }
        return true;
    }

    /** Number of this type required (0 if not part of this challenge). */
    public int getRequiredCount(String typeId) {
        Integer count = requiredCounts.get(typeId);
        return count == null ? 0 : count;
    }

    public ObjectMap<String, Integer> getRequiredCounts() {
        return new ObjectMap<>(requiredCounts);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getStarReward() { return starReward; }
}
