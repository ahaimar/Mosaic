package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DiscoveryManager — discovery and recipe unlocks")
class DiscoveryManagerTest {

    private static final int COLS = 20;
    private static final int ROWS = 12;

    private VillageGrid grid;
    private DiscoveryManager manager;

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(COLS, ROWS, 64f);
        manager = new DiscoveryManager(grid);
    }

    private void place(String typeId, int col, int row) {
        BuildingType type = BuildingCatalog.get(typeId);
        BuildingObject object = new BuildingObject(type, col, row);
        for (int dc = 0; dc < type.getWidthCells(); dc++) {
            for (int dr = 0; dr < type.getHeightCells(); dr++) {
                grid.setOccupant(col + dc, row + dr, object);
            }
        }
    }

    @Test
    void countDiscoveryUnlocksAtTheThreshold() {
        for (int i = 0; i < 20; i++) place("flower", i, 0);
        manager.checkUnlocks();
        assertTrue(manager.isUnlocked("discovery_flower_palace"));
    }

    @Test
    void partialCountDoesNotUnlock() {
        for (int i = 0; i < 19; i++) place("flower", i, 0);
        manager.checkUnlocks();
        assertFalse(manager.isUnlocked("discovery_flower_palace"));
    }

    @Test
    void recipeUnlocksWhenTheWholeCombinationIsPresent() {
        place("small_house", 0, 0);
        place("garden", 2, 0);
        place("fence", 4, 0);
        manager.checkUnlocks();
        assertTrue(manager.isUnlocked("recipe_cozy_cottage"));
    }

    @Test
    void recipeNeedsEveryIngredient() {
        place("small_house", 0, 0);
        place("garden", 2, 0);
        manager.checkUnlocks();
        assertFalse(manager.isUnlocked("recipe_cozy_cottage"));
    }

    @Test
    void houseIngredientAcceptsEitherSize() {
        place("large_house", 0, 0);
        place("garden", 3, 0);
        place("fence", 5, 0);
        manager.checkUnlocks();
        assertTrue(manager.isUnlocked("recipe_cozy_cottage"));
    }

    @Test
    void compoundDiscoveryNeedsEveryPart() {
        for (int i = 0; i < 10; i++) place("flower", i, 0);
        for (int i = 0; i < 5; i++) place("bush", i, 2);
        manager.checkUnlocks();
        assertFalse(manager.isUnlocked("discovery_garden"), "5 of 6 bushes must not unlock the garden");
    }

    @Test
    void unlocksArePermanentEvenAfterDeletion() {
        for (int i = 0; i < 20; i++) place("flower", i, 0);
        manager.checkUnlocks();
        assertTrue(manager.isUnlocked("discovery_flower_palace"));

        grid.clearCell(0, 0);
        manager.checkUnlocks();
        assertTrue(manager.isUnlocked("discovery_flower_palace"),
            "a discovered building must never be revoked");
    }

    @Test
    void listenerHearsEveryNewUnlockOnce() {
        Array<UnlockDefinition> unlocked = new Array<>();
        manager.addListener(new DiscoveryManager.DiscoveryListener() {
            @Override
            public void onUnlocked(UnlockDefinition unlock) {
                unlocked.add(unlock);
            }
        });

        for (int i = 0; i < 20; i++) place("flower", i, 0);
        manager.checkUnlocks();
        assertEquals(2, unlocked.size, "20 flowers unlock the palace and the tower together");
        assertTrue(unlocked.contains(UnlockCatalog.get("discovery_flower_palace"), true));
        assertTrue(unlocked.contains(UnlockCatalog.get("discovery_tower"), true));

        manager.checkUnlocks();
        assertEquals(2, unlocked.size, "repeat checks must not re-fire listeners");
    }

    @Test
    void unlockedRewardsAreExposedForTheToolbar() {
        for (int i = 0; i < 20; i++) place("flower", i, 0);
        manager.checkUnlocks();

        Array<BuildingType> rewards = manager.getRewardUnlockedTypes();
        assertTrue(rewards.contains(BuildingCatalog.get("flower_palace"), true));
        assertFalse(rewards.contains(BuildingCatalog.get("garden"), true),
            "unmet unlocks must not leak into the toolbar");
    }

    @Test
    void restoreUnlockedRebuildsStateFromSave() {
        Array<String> ids = new Array<>();
        ids.add("discovery_flower_palace");
        ids.add("recipe_cozy_cottage");
        ids.add("unknown_id");
        manager.restoreUnlocked(ids);

        assertTrue(manager.isUnlocked("discovery_flower_palace"));
        assertTrue(manager.isUnlocked("recipe_cozy_cottage"));
        assertEquals(2, manager.getUnlocked().size);
        assertEquals(2, manager.getUnlockedIds().size);
    }

    @Test
    void progressReflectsPartialRequirements() {
        for (int i = 0; i < 10; i++) place("flower", i, 0);
        for (int i = 0; i < 3; i++) place("bush", i, 2);

        UnlockDefinition garden = UnlockCatalog.get("discovery_garden");
        assertEquals(0.75f, garden.getProgress(manager.getCurrentCounts()), 0.001f,
            "10/10 flowers (1.0) + 3/6 bushes (0.5) average to 0.75");
        assertFalse(garden.isMet(manager.getCurrentCounts()));
    }

    @Test
    void lockedListExcludesDiscovered() {
        for (int i = 0; i < 20; i++) place("flower", i, 0);
        manager.checkUnlocks();
        assertFalse(manager.getLocked().contains(UnlockCatalog.get("discovery_flower_palace"), true));
        assertTrue(manager.getLocked().contains(UnlockCatalog.get("recipe_cozy_cottage"), true));
    }
}
