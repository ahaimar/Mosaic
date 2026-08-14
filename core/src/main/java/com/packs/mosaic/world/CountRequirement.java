package com.packs.mosaic.world;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Requires at least {@code needed} instances of one BuildingType to be
 * placed (e.g. "plant 20 flowers"). Progress is the raw have/needed ratio,
 * capped at 1.
 */
public class CountRequirement implements UnlockRequirement {

    private final String typeId;
    private final int needed;

    public CountRequirement(String typeId, int needed) {
        this.typeId = typeId;
        this.needed = needed;
    }

    public String getTypeId() {
        return typeId;
    }

    public int getNeeded() {
        return needed;
    }

    @Override
    public boolean isMet(ObjectMap<String, Integer> counts) {
        return counts.get(typeId, 0) >= needed;
    }

    @Override
    public float getProgress(ObjectMap<String, Integer> counts) {
        return Math.min(1f, (float) counts.get(typeId, 0) / needed);
    }

    @Override
    public String getLabelKey() {
        return "building." + typeId;
    }

    @Override
    public String getProgressText(ObjectMap<String, Integer> counts) {
        return counts.get(typeId, 0) + "/" + needed;
    }
}
