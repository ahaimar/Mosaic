package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildingPlacementController — delete mode")
class BuildingPlacementControllerDeleteModeTest {

    private VillageGrid grid;
    private BuildingPlacementController controller;
    private final Array<String> placedEvents = new Array<>();
    private final Array<String> deletedEvents = new Array<>();
    private final Array<String> blockedEvents = new Array<>();

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(10, 10, 64f);
        controller = new BuildingPlacementController(grid);
        controller.addListener(new BuildingPlacementController.PlacementListener() {
            @Override
            public void onPlaced(BuildingType type, int col, int row) {
                placedEvents.add(col + "," + row);
            }

            @Override
            public void onDeleted(int col, int row) {
                deletedEvents.add(col + "," + row);
            }

            @Override
            public void onDeleteBlocked(int col, int row) {
                blockedEvents.add(col + "," + row);
            }
        });
    }

    @Test
    void tapsRouteToDeleteWhileDeleteModeIsOn() {
        controller.selectType(BuildingCatalog.get("tree"));
        assertTrue(controller.tryPlace(2, 2));
        placedEvents.clear();

        controller.setDeleteMode(true);
        assertTrue(controller.isDeleteMode());
        assertTrue(controller.tryPlace(2, 2), "tap should act as delete in delete mode");

        assertTrue(grid.isCellFree(2, 2));
        assertEquals(1, deletedEvents.size);
        assertTrue(placedEvents.isEmpty(), "delete-mode tap must not record a placement");
    }

    @Test
    void deleteModeTapOnEmptyCellFiresDeleteBlockedAndPlacesNothing() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.setDeleteMode(true);

        assertFalse(controller.tryPlace(2, 2));
        assertTrue(grid.isCellFree(2, 2));
        assertEquals(1, blockedEvents.size);
        assertTrue(placedEvents.isEmpty());
        assertFalse(controller.canUndo(), "blocked delete must not pollute the history");
    }

    @Test
    void placementIsInvalidWhileDeleteModeIsOn() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.setDeleteMode(true);
        assertFalse(controller.isPlacementValid(2, 2));

        controller.setDeleteMode(false);
        assertTrue(controller.isPlacementValid(2, 2));
    }

    @Test
    void exitingDeleteModeRestoresNormalPlacement() {
        controller.selectType(BuildingCatalog.get("tree"));
        controller.setDeleteMode(true);
        controller.setDeleteMode(false);
        assertFalse(controller.isDeleteMode());

        assertTrue(controller.tryPlace(1, 1));
        assertFalse(grid.isCellFree(1, 1));
    }

    @Test
    void deleteModeDeleteIsUndoable() {
        controller.selectType(BuildingCatalog.get("small_house"));
        assertTrue(controller.tryPlace(2, 2));

        controller.setDeleteMode(true);
        assertTrue(controller.tryPlace(2, 2));
        assertTrue(grid.isCellFree(2, 2));

        controller.undo();
        assertFalse(grid.isCellFree(2, 2), "undo must restore the building removed via delete mode");
    }
}
