package com.packs.mosaic.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 3 — the multi-world system: MapCatalog, per-map building restrictions
 * and independent per-map building spaces.
 */
@DisplayName("MapCatalog — the worlds the player can build in")
class MapCatalogTest {

    @Test
    void registersAllSevenWorlds() {
        assertEquals(7, MapCatalog.getAll().size);
    }

    @Test
    void meadowIsPreservedAsTheDefaultMap() {
        GameMap meadow = MapCatalog.getMeadow();
        assertNotNull(meadow);
        assertEquals("meadow", meadow.getId());
        assertEquals(MapCatalog.MEADOW_ID, meadow.getId());
        assertSame(meadow, MapCatalog.get("meadow"));
    }

    @Test
    void idsAreUniqueAndAllMapsAreLookedUp() {
        Set<String> ids = new HashSet<>();
        for (GameMap map : MapCatalog.getAll()) {
            assertTrue(ids.add(map.getId()), "duplicate id: " + map.getId());
            assertSame(map, MapCatalog.get(map.getId()));
        }
    }

    @Test
    void everyNewMapIsListed() {
        Set<String> ids = new HashSet<>();
        for (GameMap map : MapCatalog.getAll()) ids.add(map.getId());
        for (String expected : new String[] {"meadow", "forest", "beach", "mountain",
            "snowland", "volcano", "island"}) {
            assertTrue(ids.contains(expected), "missing map: " + expected);
        }
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(MapCatalog.get("mars"));
    }

    @Test
    void everyMapHasNameDescriptionThemeAndEffect() {
        for (GameMap map : MapCatalog.getAll()) {
            assertNotNull(map.getNameKey(), map.getId() + " name key");
            assertNotNull(map.getDescriptionKey(), map.getId() + " description key");
            assertNotNull(map.getTerrain(), map.getId() + " terrain");
            assertNotNull(map.getEffect(), map.getId() + " effect");
            assertNotNull(map.getGroundColor(), map.getId() + " ground colour");
            assertNotNull(map.getClearColor(), map.getId() + " clear colour");
            assertNotNull(map.getGridLineColor(), map.getId() + " grid line colour");
            assertTrue(map.getNameKey().startsWith("map."), map.getId() + " name key prefix");
        }
    }

    @Test
    void eachMapOffersItsOwnSignatureBuildings() {
        assertNotNull(BuildingCatalog.get("pine"));
        assertTrue(BuildingCatalog.get("pine").isAvailableOn("forest"));
        assertTrue(BuildingCatalog.get("palm").isAvailableOn("beach"));
        assertTrue(BuildingCatalog.get("snowman").isAvailableOn("snowland"));
        assertTrue(BuildingCatalog.get("lava_rock").isAvailableOn("volcano"));
        assertTrue(BuildingCatalog.get("igloo").isAvailableOn("snowland"));
        assertTrue(BuildingCatalog.get("tiki_hut").isAvailableOn("island"));
    }

    @Test
    void mapBuildingsDoNotLeakIntoOtherWorlds() {
        assertFalse(BuildingCatalog.get("palm").isAvailableOn("forest"));
        assertFalse(BuildingCatalog.get("pine").isAvailableOn("beach"));
        assertFalse(BuildingCatalog.get("snowman").isAvailableOn("volcano"));
        assertFalse(BuildingCatalog.get("coconut_palm").isAvailableOn("snowland"));
        assertFalse(BuildingCatalog.get("summit_rock").isAvailableOn("island"));
    }

    @Test
    void baseTypesAreAvailableOnEveryMap() {
        assertTrue(BuildingCatalog.get("small_house").isAvailableOn("meadow"));
        assertTrue(BuildingCatalog.get("small_house").isAvailableOn("volcano"));
        assertTrue(BuildingCatalog.get("tree").isAvailableOn("beach"));
        assertTrue(BuildingCatalog.get("road_straight").isAvailableOn("island"));
        assertTrue(BuildingCatalog.get("tree").isAvailableOn(null), "null map id means everywhere");
    }

    @Test
    void everyMapSpecificBuildingBelongsToARealMap() {
        Set<String> mapIds = new HashSet<>();
        for (GameMap map : MapCatalog.getAll()) mapIds.add(map.getId());
        for (BuildingType type : BuildingCatalog.getAll()) {
            if (type.getMapIds() == null) continue;
            for (String mapId : type.getMapIds()) {
                assertTrue(mapIds.contains(mapId), type.getId() + " references unknown map " + mapId);
            }
        }
    }

    @Test
    void availableBuildingsFilterPerMap() {
        Set<String> forestIds = new HashSet<>();
        for (BuildingType type : MapCatalog.get("forest").getAvailableBuildings()) {
            forestIds.add(type.getId());
        }
        assertTrue(forestIds.contains("pine"), "forest includes its own pine");
        assertTrue(forestIds.contains("small_house"), "forest includes base types");
        assertFalse(forestIds.contains("palm"), "forest excludes beach's palm");
    }
}
