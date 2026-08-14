package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ChallengeManager — active challenge, scoring and advancement")
class ChallengeManagerTest {

    private static final int COLS = 20;
    private static final int ROWS = 12;

    private VillageGrid grid;
    private PlayerProgress progress;
    private ChallengeManager manager;

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(COLS, ROWS, 64f);
        progress = new PlayerProgress();
        manager = new ChallengeManager(grid, progress);
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
    void startsOnTheFirstChallenge() {
        assertEquals("challenge_1", manager.getCurrentChallenge().getId());
    }

    @Test
    void unknownStartingChallengeFallsBackToFirst() {
        ChallengeManager custom = new ChallengeManager(grid, progress, "does_not_exist");
        assertEquals("challenge_1", custom.getCurrentChallenge().getId());
    }

    @Test
    void partialRequirementsAwardNothing() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        manager.checkCompletion();
        assertEquals(0, progress.getTotalStars());
        assertEquals("challenge_1", manager.getCurrentChallenge().getId());
    }

    @Test
    void completingAChallengeAwardsStarsAndAdvances() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        manager.checkCompletion();
        assertEquals(5, progress.getTotalStars());
        assertEquals("challenge_2", manager.getCurrentChallenge().getId());
    }

    @Test
    void listenerHearsCompletion() {
        Array<ChallengeDefinition> completed = new Array<>();
        manager.addListener(new ChallengeManager.ChallengeListener() {
            @Override
            public void onChallengeCompleted(ChallengeDefinition challenge) {
                completed.add(challenge);
            }
        });
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        manager.checkCompletion();
        assertEquals(1, completed.size);
        assertEquals("challenge_1", completed.first().getId());
    }

    @Test
    void multiCellBuildingsCountOnce() {
        manager.setChallenge("challenge_2");
        place("large_house", 0, 0); // 2x1 — one building, two occupied cells
        place("tree", 3, 0);
        place("tree", 5, 0);
        place("road_straight", 7, 0);
        place("road_straight", 9, 0);
        manager.checkCompletion();
        assertEquals(10, progress.getTotalStars());
        assertEquals("challenge_3", manager.getCurrentChallenge().getId());
    }

    @Test
    void getCurrentCountsCountsMultiCellBuildingsOnce() {
        place("large_house", 0, 0);
        assertEquals(1, manager.getCurrentCounts().get("large_house", 0));
        assertEquals(0, manager.getCurrentCounts().get("tree", 0));
    }

    @Test
    void completingAllChallengesAwardsEverythingAndEndsNull() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        place("large_house", 6, 0);
        place("tree", 9, 0);
        place("road_straight", 11, 0);
        place("road_straight", 13, 0);
        place("small_house", 15, 0);
        place("small_house", 17, 0);
        place("street_lamp", 0, 3);
        place("street_lamp", 2, 3);
        place("flower", 4, 3);
        place("flower", 6, 3);
        manager.checkCompletion();
        assertEquals(5 + 10 + 15, progress.getTotalStars());
        assertNull(manager.getCurrentChallenge());
    }

    @Test
    void checkCompletionWithNoChallengeLeftIsHarmless() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        place("large_house", 6, 0);
        place("tree", 9, 0);
        place("road_straight", 11, 0);
        place("road_straight", 13, 0);
        place("small_house", 15, 0);
        place("small_house", 17, 0);
        place("street_lamp", 0, 3);
        place("street_lamp", 2, 3);
        place("flower", 4, 3);
        place("flower", 6, 3);
        manager.checkCompletion();
        manager.checkCompletion();
        assertEquals(30, progress.getTotalStars());
    }

    @Test
    void completedChallengesAreNotRevokedByLaterDeletion() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        manager.checkCompletion();
        assertEquals(5, progress.getTotalStars());
        assertEquals("challenge_2", manager.getCurrentChallenge().getId());

        grid.clearCell(0, 0);
        manager.checkCompletion();
        assertEquals(5, progress.getTotalStars(), "awarded stars must never decrease");
        assertEquals("challenge_2", manager.getCurrentChallenge().getId());
    }

    @Test
    void deletingBeforeCompletionKeepsTheChallengeUnmet() {
        place("small_house", 0, 0);
        place("tree", 2, 0);
        place("tree", 4, 0);
        grid.clearCell(4, 0); // one tree removed before checkCompletion is ever called
        manager.checkCompletion();
        assertEquals(0, progress.getTotalStars());
        assertEquals("challenge_1", manager.getCurrentChallenge().getId());
        assertFalse(manager.isCurrentCompleted());
    }

    @Test
    void setChallengeJumpsToANamedChallenge() {
        manager.setChallenge("challenge_3");
        assertEquals("challenge_3", manager.getCurrentChallenge().getId());
        assertFalse(manager.isCurrentCompleted(), "empty grid must not satisfy challenge_3");
    }
}
