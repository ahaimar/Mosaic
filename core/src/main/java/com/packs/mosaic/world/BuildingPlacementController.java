package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Owns the "currently selected building to place" state, draws the
 * cursor-following preview (transparent, red/green tint for
 * valid/invalid), and performs place/delete on tap. GridInputController
 * forwards taps here instead of handling placement itself, keeping
 * camera control and placement logic separate.
 *
 * Every successful place/delete is recorded on an internal
 * {@link CommandHistory} (last 20 actions) so undo()/redo() can revert
 * or re-apply it. Undo/redo fire the same onPlaced/onDeleted listener
 * events as direct actions, so UI and challenge checks stay in sync.
 *
 * UI can observe state changes via {@link PlacementListener}.
 */
public class BuildingPlacementController {

    /** Receives UI events for placement/selection changes (toasts, HUD, toolbar state). */
    public interface PlacementListener {
        default void onSelectionChanged(BuildingType type) {}
        default void onPlaced(BuildingType type, int col, int row) {}
        default void onPlacementBlocked(BuildingType type, int col, int row) {}
        default void onDeleted(int col, int row) {}
        default void onDeleteBlocked(int col, int row) {}
    }

    private final VillageGrid grid;
    private final CommandHistory history = new CommandHistory();
    private BuildingType selectedType;
    private int rotationDegrees;
    private boolean deleteMode;

    private int hoverCol = -1;
    private int hoverRow = -1;

    private final Array<PlacementListener> listeners = new Array<>();

    public BuildingPlacementController(VillageGrid grid) {
        this.grid = grid;
    }

    public void addListener(PlacementListener listener) {
        listeners.add(listener);
    }

    /** Called from the toolbar (Phase 3) or, for now, directly for testing. */
    public void selectType(BuildingType type) {
        this.selectedType = type;
        this.rotationDegrees = 0;
        for (PlacementListener l : listeners) l.onSelectionChanged(type);
    }

    public void clearSelection() {
        this.selectedType = null;
        for (PlacementListener l : listeners) l.onSelectionChanged(null);
    }

    /** Toggles delete mode: while active, taps delete buildings instead of placing. */
    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    public boolean hasSelection() {
        return selectedType != null;
    }

    public BuildingType getSelectedType() {
        return selectedType;
    }

    public void rotateSelection() {
        rotationDegrees = (rotationDegrees + 90) % 360;
    }

    /** Call every frame with the current hovered cell (from world-space cursor position). */
    public void updateHover(int col, int row) {
        this.hoverCol = col;
        this.hoverRow = row;
    }

    public int getHoverCol() {
        return hoverCol;
    }

    public int getHoverRow() {
        return hoverRow;
    }

    public boolean isPlacementValid(int col, int row) {
        if (deleteMode) return false;
        if (selectedType == null) return false;
        return canFit(selectedType, col, row);
    }

    /** True if a full type footprint fits free in (col, row) — used for restore checks too. */
    private boolean canFit(BuildingType type, int col, int row) {
        for (int dc = 0; dc < type.getWidthCells(); dc++) {
            for (int dr = 0; dr < type.getHeightCells(); dr++) {
                if (!grid.isCellFree(col + dc, row + dr)) return false;
            }
        }
        return true;
    }

    /** Left-click / tap handler — deletes in delete mode, otherwise places the selected type. */
    public boolean tryPlace(int col, int row) {
        if (deleteMode) {
            return tryDelete(col, row);
        }
        if (!isPlacementValid(col, row)) {
            for (PlacementListener l : listeners) l.onPlacementBlocked(selectedType, col, row);
            return false;
        }

        placeObject(selectedType, col, row, rotationDegrees);
        history.push(PlacementCommand.place(selectedType, col, row, rotationDegrees));
        for (PlacementListener l : listeners) l.onPlaced(selectedType, col, row);
        return true;
    }

    /** Right-click / long-press handler — removes whatever occupies (col, row), if anything. */
    public boolean tryDelete(int col, int row) {
        BuildingObject removed = removeObjectAt(col, row);
        if (removed == null) {
            for (PlacementListener l : listeners) l.onDeleteBlocked(col, row);
            return false;
        }

        history.push(PlacementCommand.delete(
            removed.getType(), removed.getOriginCol(), removed.getOriginRow(), removed.getRotationDegrees()));
        for (PlacementListener l : listeners) l.onDeleted(col, row);
        return true;
    }

    /** Reverts the most recent action. Fires onPlaced/onDeleted like a direct action. */
    public void undo() {
        PlacementCommand command = history.undo();
        if (command == null) return;

        if (command.getKind() == PlacementCommand.Kind.PLACE) {
            removeObjectAt(command.getCol(), command.getRow());
            for (PlacementListener l : listeners) l.onDeleted(command.getCol(), command.getRow());
        } else {
            placeObject(command.getType(), command.getCol(), command.getRow(), command.getRotationDegrees());
            for (PlacementListener l : listeners) l.onPlaced(command.getType(), command.getCol(), command.getRow());
        }
    }

    /** Re-applies the most recently undone action. Fires onPlaced/onDeleted like a direct action. */
    public void redo() {
        PlacementCommand command = history.redo();
        if (command == null) return;

        if (command.getKind() == PlacementCommand.Kind.PLACE) {
            placeObject(command.getType(), command.getCol(), command.getRow(), command.getRotationDegrees());
            for (PlacementListener l : listeners) l.onPlaced(command.getType(), command.getCol(), command.getRow());
        } else {
            removeObjectAt(command.getCol(), command.getRow());
            for (PlacementListener l : listeners) l.onDeleted(command.getCol(), command.getRow());
        }
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }

    /** Places a building without validating or recording it — for save restoration only. */
    public void placeRestored(BuildingType type, int col, int row, int rotationDegrees) {
        placeObject(type, col, row, rotationDegrees);
    }

    /** Places a BuildingObject of the given type with its origin at (col, row). */
    private void placeObject(BuildingType type, int col, int row, int rotationDegrees) {
        BuildingObject object = new BuildingObject(type, col, row, rotationDegrees);
        for (int dc = 0; dc < type.getWidthCells(); dc++) {
            for (int dr = 0; dr < type.getHeightCells(); dr++) {
                grid.setOccupant(col + dc, row + dr, object);
            }
        }
    }

    /**
     * Removes the building occupying (col, row), if any, and returns it.
     * Multi-cell buildings occupy several cells pointing at the same
     * object — every cell across the whole grid that matches it is cleared.
     */
    private BuildingObject removeObjectAt(int col, int row) {
        VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
        if (occupant == null) return null;

        for (int c = 0; c < grid.getCols(); c++) {
            for (int r = 0; r < grid.getRows(); r++) {
                if (grid.getOccupant(c, r) == occupant) {
                    grid.clearCell(c, r);
                }
            }
        }
        return occupant instanceof BuildingObject ? (BuildingObject) occupant : null;
    }

    /** Draws the translucent preview at the current hover cell. Call between shapeRenderer.begin/end (Filled). */
    public void drawPreview(ShapeRenderer shapeRenderer) {
        if (selectedType == null || deleteMode || hoverCol < 0 || hoverRow < 0) return;

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
