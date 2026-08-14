package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildingPlacementController — placement validity edge cases")
class BuildingPlacementControllerValidityTest {

    private static final int COLS = 10;
    private static final int ROWS = 10;

    private VillageGrid grid;
    private BuildingPlacementController controller;
    private final Array<Boolean> blockedEvents = new Array<>();

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(COLS, ROWS, 64f);
        controller = new BuildingPlacementController(grid);
        controller.addListener(new BuildingPlacementController.PlacementListener() {
            @Override
            public void onPlacementBlocked(BuildingType type, int col, int row) {
                blockedEvents.add(true);
            }
        });
    }

    @Test
    void noSelectionIsInvalid() {
        assertFalse(controller.isPlacementValid(0, 0));
        assertFalse(controller.tryPlace(0, 0));
        assertTrue(grid.isCellFree(0, 0));
    }

    @Test
    void deleteModeInvalidatesPlacement() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.setDeleteMode(true);
        assertFalse(controller.isPlacementValid(0, 0));
        assertFalse(controller.tryPlace(0, 0), "delete mode must never place");
        assertTrue(grid.isCellFree(0, 0));
    }

    @Test
    void footprintOverflowingTheRightEdgeIsRejected() {
        controller.selectType(BuildingCatalog.get("large_house")); // 2 cells wide
        assertFalse(controller.isPlacementValid(COLS - 1, 0), "right edge would overflow");
        assertFalse(controller.tryPlace(COLS - 1, 0));
        assertTrue(grid.isCellFree(COLS - 1, 0), "blocked placement must not write to the grid");

        assertTrue(controller.isPlacementValid(COLS - 2, 0), "last fitting column must be valid");
        assertTrue(controller.tryPlace(COLS - 2, 0));
        assertFalse(grid.isCellFree(COLS - 2, 0));
    }

    @Test
    void footprintOverflowingTheTopEdgeIsRejected() {
        controller.selectType(BuildingCatalog.get("school")); // 2x2
        assertFalse(controller.isPlacementValid(COLS - 2, ROWS - 1), "top edge would overflow");
        assertFalse(controller.tryPlace(COLS - 2, ROWS - 1));
        assertTrue(grid.isCellFree(COLS - 2, ROWS - 1));

        assertTrue(controller.isPlacementValid(COLS - 2, ROWS - 2), "last fitting row must be valid");
        assertTrue(controller.tryPlace(COLS - 2, ROWS - 2));
        assertFalse(grid.isCellFree(COLS - 2, ROWS - 2));
        assertFalse(grid.isCellFree(COLS - 1, ROWS - 1));
    }

    @Test
    void negativeOriginsAreRejected() {
        controller.selectType(BuildingCatalog.get("tree"));
        assertFalse(controller.isPlacementValid(-1, 0));
        assertFalse(controller.isPlacementValid(0, -1));
        assertFalse(controller.tryPlace(-1, 0));
        assertTrue(grid.isCellFree(0, 0));
    }

    @Test
    void blockedEdgePlacementIsNotRecordedInHistory() {
        controller.selectType(BuildingCatalog.get("large_house"));
        controller.tryPlace(COLS - 1, 0);
        assertFalse(controller.canUndo(), "a blocked placement must not be undoable");
        assertEquals(1, blockedEvents.size, "blocked listeners must still be notified");
    }

    @Test
    void validPlacementAtEdgeIsRecordedAndUndoable() {
        controller.selectType(BuildingCatalog.get("large_house"));
        assertTrue(controller.tryPlace(COLS - 2, 0));
        assertTrue(controller.canUndo());
        controller.undo();
        assertTrue(grid.isCellFree(COLS - 2, 0));
    }
}
