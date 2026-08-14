package com.packs.mosaic.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildingCatalog — the type registry the toolbar and placement read from")
class BuildingCatalogTest {

    @Test
    void registersEveryCatalogType() {
        assertEquals(77, BuildingCatalog.getAll().size);
    }

    @Test
    void lookupByIdReturnsTheSharedInstance() {
        BuildingType house = BuildingCatalog.get("small_house");
        assertNotNull(house);
        assertEquals("Small House", house.getDisplayName());
        assertSame(house, BuildingCatalog.get("small_house"), "types are shared recipes, not copies");
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(BuildingCatalog.get("castle"));
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (BuildingType type : BuildingCatalog.getAll()) {
            assertTrue(ids.add(type.getId()), "duplicate id: " + type.getId());
        }
    }

    @Test
    void categoryFilterPartitionsTheCatalog() {
        assertEquals(41, BuildingCatalog.getByCategory(BuildingType.Category.BUILDING).size);
        assertEquals(22, BuildingCatalog.getByCategory(BuildingType.Category.ENVIRONMENT).size);
        assertEquals(14, BuildingCatalog.getByCategory(BuildingType.Category.INFRASTRUCTURE).size);
    }

    @Test
    void constructionCategoryIsDeclaredButEmpty() {
        // BuildingSelectionMenu iterates Category.values() and skips empty rows,
        // so an unused enum constant is harmless — but it is a gap worth pinning.
        assertEquals(0, BuildingCatalog.getByCategory(BuildingType.Category.CONSTRUCTION).size);
    }

    @Test
    void everyTypeHasSaneFootprintAndUnlockCost() {
        for (BuildingType type : BuildingCatalog.getAll()) {
            assertTrue(type.getWidthCells() >= 1, type.getId() + " width");
            assertTrue(type.getHeightCells() >= 1, type.getId() + " height");
            assertTrue(type.getStarsToUnlock() >= 0, type.getId() + " unlock cost");
            assertNotNull(type.getPlaceholderColor(), type.getId() + " colour");
            assertNotNull(type.getCategory(), type.getId() + " category");
        }
    }

    @Test
    void multiCellTypesAreTheExpectedOnes() {
        Set<String> multiCell = new HashSet<>();
        for (BuildingType type : BuildingCatalog.getAll()) {
            if (type.getWidthCells() > 1 || type.getHeightCells() > 1) multiCell.add(type.getId());
        }
        Set<String> expected = new HashSet<>(java.util.Arrays.asList(
            "large_house", "school", "playground", "bridge_small",
            "flower_palace", "grand_town_hall", "royal_garden", "crystal_quarry",
            "lamp_plaza", "bench_park", "bush_maze", "highway",
            "university", "grand_market", "amusement_park", "grand_bridge",
            "nature_reserve", "central_station", "megalopolis_tower",
            "cozy_cottage", "small_town", "wizard_tower", "park_square",
            "market_street", "village_center",
            "cabin", "alpine_hut", "mountain_lookout", "ski_lodge",
            "magma_spring", "obsidian_tower",
            "lumber_hut", "stone_mine", "farm", "workshop", "dairy", "ranch",
            "iron_mine", "coal_mine", "smelter", "carpentry",
            "machine_factory", "assembly_factory",
            "warehouse", "large_warehouse"));
        assertEquals(expected, multiCell);
    }

    @Test
    void getAllExposesTheLiveBackingArray() {
        // Not a crash today, but callers could corrupt the registry: getAll() hands
        // out the internal Array rather than a copy.
        assertSame(BuildingCatalog.getAll(), BuildingCatalog.getAll());
    }
}
