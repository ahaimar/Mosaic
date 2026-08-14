package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Requires the combined count of several BuildingTypes to reach a target
 * (e.g. "build 10 houses" counts small and large houses together). The
 * label key is shared by the whole group since no single building names it.
 */
public class SumCountRequirement implements UnlockRequirement {

    private final Array<String> typeIds;
    private final int needed;
    private final String labelKey;

    public SumCountRequirement(Array<String> typeIds, int needed, String labelKey) {
        this.typeIds = new Array<>(typeIds);
        this.needed = needed;
        this.labelKey = labelKey;
    }

    public Array<String> getTypeIds() {
        return new Array<>(typeIds);
    }

    public int getNeeded() {
        return needed;
    }

    @Override
    public boolean isMet(ObjectMap<String, Integer> counts) {
        return sum(counts) >= needed;
    }

    @Override
    public float getProgress(ObjectMap<String, Integer> counts) {
        return Math.min(1f, (float) sum(counts) / needed);
    }

    @Override
    public String getLabelKey() {
        return labelKey;
    }

    @Override
    public String getProgressText(ObjectMap<String, Integer> counts) {
        return sum(counts) + "/" + needed;
    }

    private int sum(ObjectMap<String, Integer> counts) {
        int total = 0;
        for (String typeId : typeIds) {
            total += counts.get(typeId, 0);
        }
        return total;
    }
}
