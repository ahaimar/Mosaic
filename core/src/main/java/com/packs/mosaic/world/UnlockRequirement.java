package com.packs.mosaic.world;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * A single condition a discovery or recipe can ask for, evaluated against
 * per-type placed counts (see {@link GridCounts}). Immutable and reusable:
 * one requirement instance can be shared across unlock definitions. The
 * UI renders progress lines from {@link #getLabelKey()} and
 * {@link #getProgressText(ObjectMap)} so no i18n logic lives in the world
 * layer.
 */
public interface UnlockRequirement {

    /** True when this single requirement is satisfied by the current placed counts. */
    boolean isMet(ObjectMap<String, Integer> counts);

    /** Progress towards this requirement alone, in [0, 1]. */
    float getProgress(ObjectMap<String, Integer> counts);

    /** I18n key naming this requirement (a building id or a shared group label). */
    String getLabelKey();

    /** Short text suffix for a progress line, e.g. "12/20" or "✓". */
    String getProgressText(ObjectMap<String, Integer> counts);
}
