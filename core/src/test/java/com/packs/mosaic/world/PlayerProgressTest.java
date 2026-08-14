package com.packs.mosaic.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The star/unlock event: addStars(...) is the only mutation, and every
 * unlock is re-derived from the total rather than stored.
 *
 * NOTE: nothing in the running game constructs a PlayerProgress today —
 * GridPrototypeScreen hard-codes "0 STARS" in the HUD and
 * BuildingSelectionMenu lists BuildingCatalog.getAll() without asking
 * whether a type is unlocked. These tests cover the class in isolation.
 */
@DisplayName("PlayerProgress — star awards and derived unlocks")
class PlayerProgressTest {

    @Test
    void startsAtZeroStars() {
        assertEquals(0, new PlayerProgress().getTotalStars());
    }

    @Test
    void canStartFromASavedTotal() {
        assertEquals(42, new PlayerProgress(42).getTotalStars());
    }

    @Test
    void awardingStarsAccumulates() {
        PlayerProgress progress = new PlayerProgress();
        progress.addStars(3);
        progress.addStars(7);
        assertEquals(10, progress.getTotalStars());
    }

    @Test
    void negativeAndZeroAwardsAreIgnored() {
        PlayerProgress progress = new PlayerProgress(10);
        progress.addStars(-5);
        progress.addStars(0);
        assertEquals(10, progress.getTotalStars(), "stars must never decrease");
    }

    @Test
    void unlockIsInclusiveOfTheThreshold() {
        BuildingType shop = BuildingCatalog.get("shop"); // 25 stars
        assertFalse(new PlayerProgress(24).isUnlocked(shop));
        assertTrue(new PlayerProgress(25).isUnlocked(shop));
        assertTrue(new PlayerProgress(26).isUnlocked(shop));
    }

    @Test
    void freeTypesAreUnlockedFromTheStart() {
        PlayerProgress progress = new PlayerProgress();
        // 6 base free types + 18 map-specific types (each map's own buildings
        // are free; the toolbar filters them per world, not PlayerProgress)
        // + 15 free economic buildings (Task 5/6/7/8 producers, converters and
        // warehouses) + 3 free transport buildings (Task 13).
        assertEquals(42, progress.getUnlockedTypes().size);
        assertTrue(progress.isUnlocked(BuildingCatalog.get("small_house")));
        assertTrue(progress.isUnlocked(BuildingCatalog.get("tree")));
        assertTrue(progress.isUnlocked(BuildingCatalog.get("road_cross")));
        assertTrue(progress.isUnlocked(BuildingCatalog.get("palm")));
        assertTrue(progress.isUnlocked(BuildingCatalog.get("warehouse")));
        assertTrue(progress.isUnlocked(BuildingCatalog.get("truck_depot")));
        assertFalse(progress.isUnlocked(BuildingCatalog.get("bush")));
    }

    @Test
    void unlockedSetGrowsAtEachThreshold() {
        assertEquals(42, new PlayerProgress(0).getUnlockedTypes().size);
        assertEquals(42, new PlayerProgress(9).getUnlockedTypes().size);
        assertEquals(47, new PlayerProgress(10).getUnlockedTypes().size);
        assertEquals(50, new PlayerProgress(25).getUnlockedTypes().size);
        assertEquals(51, new PlayerProgress(50).getUnlockedTypes().size);
    }

    @Test
    void awardingStarsUnlocksWithoutAnyExplicitBookkeeping() {
        PlayerProgress progress = new PlayerProgress();
        BuildingType school = BuildingCatalog.get("school");
        assertFalse(progress.isUnlocked(school));
        progress.addStars(25);
        assertTrue(progress.isUnlocked(school), "unlocks are derived, never stored");
    }

    @Test
    void starsUntilNextUnlockCountsDownToTheNearestThreshold() {
        assertEquals(10, new PlayerProgress(0).starsUntilNextUnlock());
        assertEquals(1, new PlayerProgress(9).starsUntilNextUnlock());
        assertEquals(15, new PlayerProgress(10).starsUntilNextUnlock());
        assertEquals(25, new PlayerProgress(25).starsUntilNextUnlock());
    }

    @Test
    void starsUntilNextUnlockIsMinusOneWhenEverythingIsUnlocked() {
        assertEquals(-1, new PlayerProgress(50).starsUntilNextUnlock());
        assertEquals(-1, new PlayerProgress(9999).starsUntilNextUnlock());
    }
}
