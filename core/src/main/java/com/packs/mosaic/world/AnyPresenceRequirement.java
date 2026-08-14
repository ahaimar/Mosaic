package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Requires at least one of the listed BuildingTypes to be present — the
 * "or" form of a recipe ingredient ("House" accepts a small or a large
 * house). Progress is 0 until satisfied, 1 after.
 */
public class AnyPresenceRequirement implements UnlockRequirement {

    private final Array<String> typeIds;
    private final String labelKey;

    public AnyPresenceRequirement(Array<String> typeIds, String labelKey) {
        this.typeIds = new Array<>(typeIds);
        this.labelKey = labelKey;
    }

    public Array<String> getTypeIds() {
        return new Array<>(typeIds);
    }

    @Override
    public boolean isMet(ObjectMap<String, Integer> counts) {
        for (String typeId : typeIds) {
            if (counts.get(typeId, 0) >= 1) return true;
        }
        return false;
    }

    @Override
    public float getProgress(ObjectMap<String, Integer> counts) {
        return isMet(counts) ? 1f : 0f;
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
