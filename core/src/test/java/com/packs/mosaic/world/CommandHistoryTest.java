package com.packs.mosaic.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandHistory — bounded undo/redo stack")
class CommandHistoryTest {

    private CommandHistory history;

    @BeforeEach
    void setUp() {
        history = new CommandHistory();
    }

    private PlacementCommand commandAt(int col) {
        return PlacementCommand.place(BuildingCatalog.get("tree"), col, 1, 0);
    }

    @Test
    void startsEmpty() {
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertNull(history.undo());
        assertNull(history.redo());
        assertEquals(0, history.getUndoCount());
        assertEquals(0, history.getRedoCount());
    }

    @Test
    void nullCommandsAreIgnored() {
        history.push(null);
        assertFalse(history.canUndo());
        assertEquals(0, history.getUndoCount());
    }

    @Test
    void undoReturnsCommandsInReverseOrder() {
        history.push(commandAt(1));
        history.push(commandAt(2));
        history.push(commandAt(3));
        assertEquals(3, history.undo().getCol());
        assertEquals(2, history.undo().getCol());
        assertEquals(1, history.undo().getCol());
        assertFalse(history.canUndo());
    }

    @Test
    void redoReplaysInOriginalOrder() {
        history.push(commandAt(1));
        history.push(commandAt(2));
        history.undo();
        history.undo();
        assertEquals(1, history.redo().getCol());
        assertEquals(2, history.redo().getCol());
        assertFalse(history.canRedo());
    }

    @Test
    void pushAfterUndoClearsTheRedoBranch() {
        history.push(commandAt(1));
        history.push(commandAt(2));
        history.undo();
        history.push(commandAt(3));
        assertFalse(history.canRedo(), "a new action diverges from the undone history");
        assertEquals(3, history.undo().getCol());
        assertEquals(1, history.undo().getCol());
    }

    @Test
    void stackIsCappedAtTwentyActions() {
        for (int i = 1; i <= 25; i++) history.push(commandAt(i));
        assertEquals(CommandHistory.MAX_ACTIONS, history.getUndoCount());

        assertEquals(25, history.undo().getCol(), "most recent action is undone first");
        for (int i = 24; i >= 6; i--) {
            assertEquals(i, history.undo().getCol());
        }
        assertFalse(history.canUndo(), "the five oldest actions must have been dropped");
    }

    @Test
    void clearResetsBothStacks() {
        history.push(commandAt(1));
        history.push(commandAt(2));
        history.undo();
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals(0, history.getUndoCount());
        assertEquals(0, history.getRedoCount());
    }
}
