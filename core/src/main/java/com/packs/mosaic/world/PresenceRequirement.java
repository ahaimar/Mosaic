package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Requires every listed BuildingType to be present at least once at the
 * same time — a single recipe ingredient ("Garden + Fence" must both be
 * on the grid). Progress is the fraction of listed types already present.
 */
public class PresenceRequirement implements UnlockRequirement {

    private final Array<String> typeIds;
    private final String labelKey;

    public PresenceRequirement(Array<String> typeIds, String labelKey) {
        this.typeIds = new Array<>(typeIds);
        this.labelKey = labelKey;
    }

    public Array<String> getTypeIds() {
        return new Array<>(typeIds);
    }

    @Override
    public boolean isMet(ObjectMap<String, Integer> counts) {
        for (String typeId : typeIds) {
            if (counts.get(typeId, 0) < 1) return false;
        }
        return true;
    }

    @Override
    public float getProgress(ObjectMap<String, Integer> counts) {
        if (typeIds.size == 0) return 1f;
        int present = 0;
        for (String typeId : typeIds) {
            if (counts.get(typeId, 0) >= 1) present++;
        }
        return (float) present / typeIds.size;
    }

    @Override
    public String getLabelKey() {
        return labelKey;
    }

    @Override
    public String getProgressText(ObjectMap<String, Integer> counts) {
        return isMet(counts) ? "\u2713" : "";
    }
}
