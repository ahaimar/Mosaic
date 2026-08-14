package com.packs.mosaic.world;

import com.badlogic.gdx.utils.Array;

/**
 * Undo/redo stack for placement actions, capped at the most recent
 * MAX_ACTIONS entries (oldest are dropped once the cap is exceeded, per
 * the spec's "last 20 actions"). undo()/redo() only move a command
 * between the two stacks — they do not touch the grid themselves; the
 * caller (BuildingPlacementController) applies the inverse/forward effect.
 */
public class CommandHistory {

    public static final int MAX_ACTIONS = 20;

    private final Array<PlacementCommand> undoStack = new Array<>();
    private final Array<PlacementCommand> redoStack = new Array<>();

    /** Records a performed action and clears the redo branch (a new action diverges from history). */
    public void push(PlacementCommand command) {
        if (command == null) return;
        undoStack.add(command);
        if (undoStack.size > MAX_ACTIONS) {
            undoStack.removeIndex(0);
        }
        redoStack.clear();
    }

    public boolean canUndo() {
        return undoStack.size > 0;
    }

    public boolean canRedo() {
        return redoStack.size > 0;
    }

    /** Pops the most recent action to revert, or null if there is nothing to undo. */
    public PlacementCommand undo() {
        if (undoStack.size == 0) return null;
        PlacementCommand command = undoStack.pop();
        redoStack.add(command);
        return command;
    }

    /** Pops the most recently undone action to re-apply, or null if there is nothing to redo. */
    public PlacementCommand redo() {
        if (redoStack.size == 0) return null;
        PlacementCommand command = redoStack.pop();
        undoStack.add(command);
        return command;
    }

    public int getUndoCount() { return undoStack.size; }
    public int getRedoCount() { return redoStack.size; }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
