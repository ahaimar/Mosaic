package com.packs.mosaic.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Owns the "currently selected building to place" state, draws the
 * cursor-following preview (transparent, red/green tint for
 * valid/invalid), and performs place/delete on tap. GridInputController
 * forwards taps here instead of handling placement itself, keeping
 * camera control and placement logic separate.
 */
public class BuildingPlacementController {

    private final VillageGrid grid;
    private BuildingType selectedType;
    private int rotationDegrees;

    private int hoverCol = -1;
    private int hoverRow = -1;

    public BuildingPlacementController(VillageGrid grid) {
        this.grid = grid;
    }

    /** Called from the toolbar (Phase 3) or, for now, directly for testing. */
    public void selectType(BuildingType type) {
        this.selectedType = type;
        this.rotationDegrees = 0;
    }

    public void clearSelection() {
        this.selectedType = null;
    }

    public boolean hasSelection() {
        return selectedType != null;
    }

    public void rotateSelection() {
        rotationDegrees = (rotationDegrees + 90) % 360;
    }

    /** Call every frame with the current hovered cell (from world-space cursor position). */
    public void updateHover(int col, int row) {
        this.hoverCol = col;
        this.hoverRow = row;
    }

    public boolean isPlacementValid(int col, int row) {
        if (selectedType == null) return false;
        for (int dc = 0; dc < selectedType.getWidthCells(); dc++) {
            for (int dr = 0; dr < selectedType.getHeightCells(); dr++) {
                if (!grid.isCellFree(col + dc, row + dr)) return false;
            }
        }
        return true;
    }

    /** Left-click / tap handler — places the selected type at (col, row) if valid. */
    public boolean tryPlace(int col, int row) {
        if (!isPlacementValid(col, row)) return false;

        BuildingObject object = new BuildingObject(selectedType, col, row);
        object.rotate90(); // will be called (rotationDegrees/90) times below
        for (int i = 1; i < rotationDegrees / 90; i++) object.rotate90();

        for (int dc = 0; dc < selectedType.getWidthCells(); dc++) {
            for (int dr = 0; dr < selectedType.getHeightCells(); dr++) {
                grid.setOccupant(col + dc, row + dr, object);
            }
        }
        return true;
    }

    /** Right-click / long-press handler — removes whatever occupies (col, row), if anything. */
    public boolean tryDelete(int col, int row) {
        VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
        if (occupant == null) return false;

        // Multi-cell buildings occupy several cells pointing at the same
        // object — clear every cell across the whole grid that matches it.
        for (int c = 0; c < grid.getCols(); c++) {
            for (int r = 0; r < grid.getRows(); r++) {
                if (grid.getOccupant(c, r) == occupant) {
                    grid.clearCell(c, r);
                }
            }
        }
        return true;
    }

    /** Draws the translucent preview at the current hover cell. Call between shapeRenderer.begin/end (Filled). */
    public void drawPreview(ShapeRenderer shapeRenderer) {
        if (selectedType == null || hoverCol < 0 || hoverRow < 0) return;

        boolean valid = isPlacementValid(hoverCol, hoverRow);
        Color tint = valid ? new Color(0.3f, 1f, 0.3f, 0.5f) : new Color(1f, 0.3f, 0.3f, 0.5f);
        shapeRenderer.setColor(tint);

        float cellSize = grid.getCellSize();
        float x = grid.cellToWorldX(hoverCol);
        float y = grid.cellToWorldY(hoverRow);
        float w = selectedType.getWidthCells() * cellSize;
        float h = selectedType.getHeightCells() * cellSize;
        shapeRenderer.rect(x, y, w, h);
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }
}
