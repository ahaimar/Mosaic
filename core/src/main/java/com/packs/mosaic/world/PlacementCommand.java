package com.packs.mosaic.world;

/**
 * Immutable record of a single successful place/delete action, stored on
 * the undo/redo stack (CommandHistory). A PLACE command captures the type
 * and origin cell plus the rotation applied at placement time so a redo
 * reproduces the building exactly; a DELETE command captures the removed
 * object's own type/origin/rotation so an undo can restore it.
 */
public class PlacementCommand {

    public enum Kind {
        PLACE, DELETE
    }

    private final Kind kind;
    private final BuildingType type;
    private final int col;
    private final int row;
    private final int rotationDegrees;

    private PlacementCommand(Kind kind, BuildingType type, int col, int row, int rotationDegrees) {
        this.kind = kind;
        this.type = type;
        this.col = col;
        this.row = row;
        this.rotationDegrees = rotationDegrees;
    }

    public static PlacementCommand place(BuildingType type, int col, int row, int rotationDegrees) {
        return new PlacementCommand(Kind.PLACE, type, col, row, rotationDegrees);
    }

    public static PlacementCommand delete(BuildingType type, int col, int row, int rotationDegrees) {
        return new PlacementCommand(Kind.DELETE, type, col, row, rotationDegrees);
    }

    public Kind getKind() { return kind; }
    public BuildingType getType() { return type; }
    public int getCol() { return col; }
    public int getRow() { return row; }
    public int getRotationDegrees() { return rotationDegrees; }
}
