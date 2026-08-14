package com.packs.mosaic.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VillageGrid — cell occupancy and world/cell conversion")
class VillageGridTest {

    private static final int COLS = 20;
    private static final int ROWS = 12;
    private static final float CELL = 64f;

    private VillageGrid grid;
    private VillageGrid.GridOccupant occupant;

    @BeforeEach
    void setUp() {
        grid = new VillageGrid(COLS, ROWS, CELL);
        occupant = new VillageGrid.GridOccupant() {
        };
    }

    @Nested
    @DisplayName("bounds")
    class Bounds {

        @Test
        void insideCornersAreInBounds() {
            assertTrue(grid.isInBounds(0, 0));
            assertTrue(grid.isInBounds(COLS - 1, ROWS - 1));
        }

        @Test
        void justOutsideEveryEdgeIsOutOfBounds() {
            assertFalse(grid.isInBounds(-1, 0));
            assertFalse(grid.isInBounds(0, -1));
            assertFalse(grid.isInBounds(COLS, 0));
            assertFalse(grid.isInBounds(0, ROWS));
        }

        @Test
        void outOfBoundsCellsAreNeverFree() {
            assertFalse(grid.isCellFree(-1, 5));
            assertFalse(grid.isCellFree(COLS, 5));
            assertFalse(grid.isCellFree(5, ROWS));
        }

        @Test
        void getOccupantOutOfBoundsReturnsNullInsteadOfThrowing() {
            assertNull(grid.getOccupant(-1, -1));
            assertNull(grid.getOccupant(COLS, ROWS));
        }

        @Test
        void setOccupantOutOfBoundsThrows() {
            assertThrows(IllegalArgumentException.class, () -> grid.setOccupant(COLS, 0, occupant));
        }

        @Test
        void clearCellOutOfBoundsIsANoOp() {
            grid.clearCell(-3, -3); // must not throw
        }
    }

    @Nested
    @DisplayName("occupancy")
    class Occupancy {

        @Test
        void freshGridIsEmpty() {
            for (int col = 0; col < COLS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    assertTrue(grid.isCellFree(col, row), "cell " + col + "," + row);
                }
            }
        }

        @Test
        void setThenGetReturnsTheSameInstance() {
            grid.setOccupant(3, 4, occupant);
            assertSame(occupant, grid.getOccupant(3, 4));
            assertFalse(grid.isCellFree(3, 4));
        }

        @Test
        void settingOneCellLeavesNeighboursFree() {
            grid.setOccupant(3, 4, occupant);
            assertTrue(grid.isCellFree(2, 4));
            assertTrue(grid.isCellFree(4, 4));
            assertTrue(grid.isCellFree(3, 3));
            assertTrue(grid.isCellFree(3, 5));
        }

        @Test
        void clearCellFreesTheCell() {
            grid.setOccupant(3, 4, occupant);
            grid.clearCell(3, 4);
            assertTrue(grid.isCellFree(3, 4));
            assertNull(grid.getOccupant(3, 4));
        }

        @Test
        void setOccupantOverwritesWithoutComplaining() {
            VillageGrid.GridOccupant other = new VillageGrid.GridOccupant() {
            };
            grid.setOccupant(3, 4, occupant);
            grid.setOccupant(3, 4, other);
            assertSame(other, grid.getOccupant(3, 4),
                "setOccupant is documented as 'caller must check isCellFree first' — it silently overwrites");
        }
    }

    @Nested
    @DisplayName("coordinate conversion")
    class Conversion {

        @Test
        void worldOriginMapsToCellZero() {
            assertEquals(0, grid.worldToCol(0f));
            assertEquals(0, grid.worldToRow(0f));
        }

        @Test
        void anyPointInsideACellMapsToThatCell() {
            assertEquals(5, grid.worldToCol(5 * CELL));          // exact left edge
            assertEquals(5, grid.worldToCol(5 * CELL + CELL / 2)); // centre
            assertEquals(5, grid.worldToCol(6 * CELL - 0.01f));  // just short of the right edge
            assertEquals(6, grid.worldToCol(6 * CELL));          // next cell starts here
        }

        @Test
        void negativeWorldCoordinatesFloorToNegativeCells() {
            // Math.floor (not integer division) matters here: -1 would become 0 with a cast.
            assertEquals(-1, grid.worldToCol(-1f));
            assertEquals(-1, grid.worldToRow(-CELL));
            assertEquals(-2, grid.worldToRow(-CELL - 1f));
        }

        @Test
        void cellToWorldReturnsTheBottomLeftCorner() {
            assertEquals(5 * CELL, grid.cellToWorldX(5));
            assertEquals(7 * CELL, grid.cellToWorldY(7));
        }

        @Test
        void cellToWorldAndBackIsStable() {
            for (int col = 0; col < COLS; col++) {
                assertEquals(col, grid.worldToCol(grid.cellToWorldX(col) + CELL / 2f));
            }
        }
    }

    @Test
    void accessorsReportConstructionValues() {
        assertEquals(COLS, grid.getCols());
        assertEquals(ROWS, grid.getRows());
        assertEquals(CELL, grid.getCellSize());
    }

    @Test
    void disposeIsSafeToCall() {
        grid.setOccupant(1, 1, occupant);
        grid.dispose();
    }
}
