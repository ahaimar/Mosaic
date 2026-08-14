package com.packs.mosaic.world;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildingObject / BuildingType — a placed instance and its shared recipe")
class BuildingObjectTest {

    private static final BuildingType LARGE_HOUSE = BuildingCatalog.get("large_house");

    @Test
    void remembersItsTypeAndOrigin() {
        BuildingObject object = new BuildingObject(LARGE_HOUSE, 4, 7);
        assertSame(LARGE_HOUSE, object.getType());
        assertEquals(4, object.getOriginCol());
        assertEquals(7, object.getOriginRow());
    }

    @Test
    void startsUnrotated() {
        assertEquals(0, new BuildingObject(LARGE_HOUSE, 0, 0).getRotationDegrees());
    }

    @Test
    void rotationCyclesThroughAllFourQuartersAndWrapsToZero() {
        BuildingObject object = new BuildingObject(LARGE_HOUSE, 0, 0);
        object.rotate90();
        assertEquals(90, object.getRotationDegrees());
        object.rotate90();
        assertEquals(180, object.getRotationDegrees());
        object.rotate90();
        assertEquals(270, object.getRotationDegrees());
        object.rotate90();
        assertEquals(0, object.getRotationDegrees(), "must wrap, not reach 360");
    }

    @Test
    void rotationIsPerInstanceNotPerType() {
        BuildingObject a = new BuildingObject(LARGE_HOUSE, 0, 0);
        BuildingObject b = new BuildingObject(LARGE_HOUSE, 2, 0);
        a.rotate90();
        assertEquals(90, a.getRotationDegrees());
        assertEquals(0, b.getRotationDegrees());
    }

    @Test
    void isUsableAsAGridOccupant() {
        assertTrue(new BuildingObject(LARGE_HOUSE, 0, 0) instanceof VillageGrid.GridOccupant);
    }

    @Test
    void buildingTypeIsAPlainImmutableRecord() {
        BuildingType type = new BuildingType("test", "Test", BuildingType.Category.BUILDING,
            2, 3, Color.RED, 15);
        assertEquals("test", type.getId());
        assertEquals("Test", type.getDisplayName());
        assertEquals(BuildingType.Category.BUILDING, type.getCategory());
        assertEquals(2, type.getWidthCells());
        assertEquals(3, type.getHeightCells());
        assertEquals(15, type.getStarsToUnlock());
        assertSame(Color.RED, type.getPlaceholderColor());
    }
}
