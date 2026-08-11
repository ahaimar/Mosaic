package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Disposable;

/**
 * Grid data model: tracks which cells are occupied and by what, and
 * converts between world coordinates and cell coordinates. Pure data
 * + math — no rendering, no input. GridInputController reads this to
 * validate placement; the (future) BuildingManager writes to it.
 */
public class VillageGrid implements Disposable {

    /** Marker interface for anything that can occupy a grid cell. Real
     *  BuildingObject/PlacedObject types will implement this once the
     *  building-object system exists (Phase 1, later class). */
    public interface GridOccupant {
    }

    private final int cols;
    private final int rows;
    private final float cellSize;
    private final GridOccupant[][] cells; // [col][row]

    public VillageGrid(int cols, int rows, float cellSize) {
        this.cols = cols;
        this.rows = rows;
        this.cellSize = cellSize;
        this.cells = new GridOccupant[cols][rows];
    }

    /** True if (col, row) is within the grid bounds. */
    public boolean isInBounds(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    /** True if the cell is empty AND in bounds — the check placement code should use. */
    public boolean isCellFree(int col, int row) {
        return isInBounds(col, row) && cells[col][row] == null;
    }

    public GridOccupant getOccupant(int col, int row) {
        return isInBounds(col, row) ? cells[col][row] : null;
    }

    /** Places an occupant at (col, row). Caller must check isCellFree first. */
    public void setOccupant(int col, int row, GridOccupant occupant) {
        if (!isInBounds(col, row)) {
            throw new IllegalArgumentException("Cell out of bounds: (" + col + ", " + row + ")");
        }
        cells[col][row] = occupant;
    }

    /** Clears whatever occupies (col, row), if anything. */
    public void clearCell(int col, int row) {
        if (isInBounds(col, row)) {
            cells[col][row] = null;
        }
    }

    /** Converts a world-space point to grid cell coordinates (may be out of bounds — check isInBounds). */
    public int worldToCol(float worldX) {
        return (int) Math.floor(worldX / cellSize);
    }

    public int worldToRow(float worldY) {
        return (int) Math.floor(worldY / cellSize);
    }

    /** Bottom-left world-space corner of a cell, for snapping previews/sprites. */
    public float cellToWorldX(int col) {
        return col * cellSize;
    }

    public float cellToWorldY(int row) {
        return row * cellSize;
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
    public float getCellSize() { return cellSize; }

    @Override
    public void dispose() {
        // No GPU/native resources owned here yet — present for symmetry
        // with GridPrototypeScreen's dispose chain and in case cached
        // per-cell render data gets added later.
    }
}
