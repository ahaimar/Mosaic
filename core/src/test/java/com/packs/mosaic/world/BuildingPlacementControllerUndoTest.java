package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildingPlacementController — undo/redo of place and delete")
class BuildingPlacementControllerUndoTest {

    private VillageGrid grid;
    private BuildingPlacementController controller;
    private final Array<BuildingType> placedEvents = new Array<>();
    private final Array<String> deletedEvents = new Array<>();

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(10, 10, 64f);
        controller = new BuildingPlacementController(grid);
        controller.addListener(new BuildingPlacementController.PlacementListener() {
            @Override
            public void onPlaced(BuildingType type, int col, int row) {
                placedEvents.add(type);
            }

            @Override
            public void onDeleted(int col, int row) {
                deletedEvents.add(col + "," + row);
            }
        });
    }

    @Test
    void undoAfterPlaceRemovesTheBuilding() {
        controller.selectType(BuildingCatalog.get("small_house"));
        assertTrue(controller.tryPlace(2, 2));
        assertFalse(grid.isCellFree(2, 2));
        assertTrue(controller.canUndo());

        controller.undo();
        assertTrue(grid.isCellFree(2, 2));
        assertTrue(controller.canRedo());
    }

    @Test
    void redoReappliesTheBuilding() {
        controller.selectType(BuildingCatalog.get("small_house"));
        controller.tryPlace(2, 2);
        controller.undo();
        controller.redo();
        assertFalse(grid.isCellFree(2, 2));
        BuildingObject object = (BuildingObject) grid.getOccupant(2, 2);
        assertEquals("small_house", object.getType().getId());
    }

    @Test
    void rotationSurvivesUndoRedo() {
        controller.selectType(BuildingCatalog.get("large_house"));
        controller.rotateSelection();
        controller.rotateSelection(); // 180°
        controller.tryPlace(2, 2);
        controller.undo();
        controller.redo();
        BuildingObject object = (BuildingObject) grid.getOccupant(2, 2);
        assertEquals(180, object.getRotationDegrees());
    }

    @Test
    void undoAfterDeleteRestoresTheBuildingAtItsOrigin() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.tryPlace(1, 1);
        controller.selectType(BuildingCatalog.get("flower"));
        controller.tryPlace(3, 1);

        assertTrue(controller.tryDelete(1, 1));
        assertTrue(grid.isCellFree(1, 1));

        controller.undo();
        BuildingObject restored = (BuildingObject) grid.getOccupant(1, 1);
        assertEquals("tree", restored.getType().getId());
        assertEquals(1, restored.getOriginCol());
        assertEquals(1, restored.getOriginRow());
    }

    @Test
    void undoDeleteTappedOnNonOriginCellRestoresTheWholeBuilding() {
        controller.selectType(BuildingCatalog.get("large_house"));
        assertTrue(controller.tryPlace(2, 3)); // occupies (2,3) and (3,3)
        assertTrue(controller.tryDelete(3, 3)); // tap the non-origin cell
        assertTrue(grid.isCellFree(2, 3));
        assertTrue(grid.isCellFree(3, 3));

        controller.undo();
        assertFalse(grid.isCellFree(2, 3));
        assertFalse(grid.isCellFree(3, 3));
        assertSame(grid.getOccupant(2, 3), grid.getOccupant(3, 3),
            "both cells must reference the same restored object");
    }

    @Test
    void undoRedoEmitTheSameEventsAsDirectActions() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.tryPlace(1, 1);
        placedEvents.clear();

        controller.undo();
        assertEquals(1, deletedEvents.size);

        controller.redo();
        assertEquals(1, placedEvents.size);
        assertEquals("tree", placedEvents.first().getId());
    }

    @Test
    void undoWithEmptyHistoryIsANoOp() {
        controller.undo();
        controller.redo();
        assertTrue(placedEvents.isEmpty());
        assertTrue(deletedEvents.isEmpty());
    }

    @Test
    void blockedPlaceIsNotRecorded() {
        controller.selectType(BuildingCatalog.get("small_house"));
        assertTrue(controller.tryPlace(2, 2));
        controller.selectType(BuildingCatalog.get("large_house"));
        assertFalse(controller.tryPlace(2, 2), "must overlap the placed house");
        assertFalse(controller.canRedo());

        controller.undo(); // only the first, valid placement is undoable
        assertTrue(grid.isCellFree(2, 2));
        assertFalse(controller.canUndo());
    }

    @Test
    void newActionAfterUndoClearsRedo() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.tryPlace(1, 1);
        controller.tryPlace(3, 1);
        controller.undo();
        assertTrue(controller.canRedo());

        controller.tryPlace(5, 1);
        assertFalse(controller.canRedo());
    }

    @Test
    void undoAcrossMoreThanTwentyActionsKeepsOnlyTheNewest() {
        controller.selectType(BuildingCatalog.get("tree"));
        for (int i = 0; i < 25; i++) {
            assertTrue(controller.tryPlace(i % 10, i / 10));
        }
        assertEquals(25, countOccupied(grid));

        for (int i = 0; i < CommandHistory.MAX_ACTIONS; i++) {
            controller.undo();
        }
        assertFalse(controller.canUndo());
        assertEquals(25 - CommandHistory.MAX_ACTIONS, countOccupied(grid),
            "only the five oldest placements may remain");

        controller.undo(); // must be a harmless no-op past the cap
        assertEquals(25 - CommandHistory.MAX_ACTIONS, countOccupied(grid));
    }

    @Test
    void redoAcrossMoreThanTwentyActionsRestoresEverything() {
        controller.selectType(BuildingCatalog.get("tree"));
        for (int i = 0; i < 25; i++) {
            controller.tryPlace(i % 10, i / 10);
        }
        for (int i = 0; i < CommandHistory.MAX_ACTIONS; i++) {
            controller.undo();
        }

        for (int i = 0; i < CommandHistory.MAX_ACTIONS; i++) {
            controller.redo();
        }
        assertFalse(controller.canRedo());
        assertEquals(25, countOccupied(grid));

        controller.redo(); // must be a harmless no-op past the cap
        assertEquals(25, countOccupied(grid));
    }

    private static int countOccupied(VillageGrid grid) {
        int count = 0;
        for (int col = 0; col < grid.getCols(); col++) {
            for (int row = 0; row < grid.getRows(); row++) {
                if (!grid.isCellFree(col, row)) count++;
            }
        }
        return count;
    }
}
