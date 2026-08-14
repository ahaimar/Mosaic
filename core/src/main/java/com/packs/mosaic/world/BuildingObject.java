package com.packs.mosaic.world;

/**
 * A BuildingType placed at a specific grid location. Implements
 * GridOccupant so VillageGrid can store it directly. Rotation is
 * stored here (not on BuildingType, since it's per-instance) even
 * though Phase 1 rendering ignores it for now — the preview/rotate
 * controls (Phase 1 spec item) will read/write this field.
 */
public class BuildingObject implements VillageGrid.GridOccupant {

    private final BuildingType type;
    private final int originCol;
    private final int originRow;
    private int rotationDegrees; // 0, 90, 180, 270

    public BuildingObject(BuildingType type, int originCol, int originRow) {
        this(type, originCol, originRow, 0);
    }

    /** Full constructor with rotation, for save/load and undo/redo restoration. */
    public BuildingObject(BuildingType type, int originCol, int originRow, int rotationDegrees) {
        this.type = type;
        this.originCol = originCol;
        this.originRow = originRow;
        this.rotationDegrees = (rotationDegrees / 90 % 4) * 90; // snap to 0/90/180/270
    }

    public BuildingType getType() { return type; }
    public int getOriginCol() { return originCol; }
    public int getOriginRow() { return originRow; }

    public int getRotationDegrees() { return rotationDegrees; }

    public void rotate90() {
        rotationDegrees = (rotationDegrees + 90) % 360;
    }
}
