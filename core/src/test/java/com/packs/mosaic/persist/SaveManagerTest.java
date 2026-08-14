package com.packs.mosaic.persist;

import com.badlogic.gdx.files.FileHandle;
import com.packs.mosaic.support.HeadlessGdx;
import com.packs.mosaic.world.econ.EconomyState;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SaveManager — JSON save/load round-trip")
class SaveManagerTest {

    private File file;
    private SaveManager manager;

    @BeforeEach
    void setUp() throws Exception {
        HeadlessGdx.install();
        file = File.createTempFile("mosaic_save", ".json");
        file.delete(); // start from a clean, non-existent file
        manager = new SaveManager(new FileHandle(file));
    }

    @AfterEach
    void tearDown() {
        file.delete();
    }

    private SaveData sampleData() {
        SaveData data = new SaveData();
        data.totalStars = 30;
        data.currentChallengeId = "challenge_2";
        data.placedObjects.add(new SaveData.PlacedObject("small_house", 2, 3, 0));
        data.placedObjects.add(new SaveData.PlacedObject("large_house", 5, 5, 90));
        return data;
    }

    @Test
    void noSaveFileMeansNoSave() {
        assertFalse(manager.hasSave());
        assertNull(manager.load());
    }

    @Test
    void saveThenLoadRoundTripsAllFields() {
        manager.save(sampleData());
        assertTrue(manager.hasSave());

        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertEquals(30, loaded.totalStars);
        assertEquals("challenge_2", loaded.currentChallengeId);
        assertEquals(2, loaded.placedObjects.size);

        SaveData.PlacedObject first = loaded.placedObjects.first();
        assertEquals("small_house", first.typeId);
        assertEquals(2, first.col);
        assertEquals(3, first.row);
        assertEquals(0, first.rotationDegrees);

        SaveData.PlacedObject second = loaded.placedObjects.get(1);
        assertEquals("large_house", second.typeId);
        assertEquals(90, second.rotationDegrees);
    }

    @Test
    void discoveredUnlocksRoundTrip() {
        SaveData data = sampleData();
        data.discoveredUnlocks.add("discovery_flower_palace");
        data.discoveredUnlocks.add("recipe_cozy_cottage");
        manager.save(data);

        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertEquals(2, loaded.discoveredUnlocks.size);
        assertTrue(loaded.discoveredUnlocks.contains("discovery_flower_palace", false));
        assertTrue(loaded.discoveredUnlocks.contains("recipe_cozy_cottage", false));
    }

    @Test
    void perMapSnapshotsRoundTrip() {
        SaveData data = sampleData();
        SaveData.MapSaveData beach = new SaveData.MapSaveData();
        beach.mapId = "beach";
        beach.placedObjects.add(new SaveData.PlacedObject("palm", 4, 2, 0));
        SaveData.MapSaveData snowland = new SaveData.MapSaveData();
        snowland.mapId = "snowland";
        snowland.placedObjects.add(new SaveData.PlacedObject("igloo", 1, 1, 90));
        snowland.placedObjects.add(new SaveData.PlacedObject("snowman", 3, 1, 0));
        data.maps.add(beach);
        data.maps.add(snowland);
        manager.save(data);

        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertEquals(2, loaded.maps.size);
        assertEquals("beach", loaded.maps.get(0).mapId);
        assertEquals(1, loaded.maps.get(0).placedObjects.size);
        assertEquals("palm", loaded.maps.get(0).placedObjects.first().typeId);
        assertEquals("snowland", loaded.maps.get(1).mapId);
        assertEquals(2, loaded.maps.get(1).placedObjects.size);
        assertEquals("igloo", loaded.maps.get(1).placedObjects.first().typeId);
        assertEquals(90, loaded.maps.get(1).placedObjects.first().rotationDegrees);
    }

    @Test
    void seasonRoundTrips() {
        SaveData data = sampleData();
        data.season = "winter";
        manager.save(data);

        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertEquals("winter", loaded.season);
    }

    @Test
    void economyStateRoundTrips() {
        SaveData data = sampleData();
        EconomyState economy = new EconomyState();
        economy.money = 123.5f;
        economy.population = 7f;
        EconomyState.StockState wood = new EconomyState.StockState();
        wood.goodId = "wood";
        wood.amount = 42f;
        economy.inventory.add(wood);
        EconomyState.ConstructionState site = new EconomyState.ConstructionState();
        site.typeId = "farm";
        site.remainingCost = 10f;
        site.remainingTicks = 2f;
        economy.construction.add(site);
        data.economy = economy;
        manager.save(data);

        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertNotNull(loaded.economy);
        assertEquals(123.5f, loaded.economy.money, 0.001f);
        assertEquals(7f, loaded.economy.population, 0.001f);
        assertEquals(1, loaded.economy.inventory.size);
        assertEquals("wood", loaded.economy.inventory.first().goodId);
        assertEquals(42f, loaded.economy.inventory.first().amount, 0.001f);
        assertEquals(1, loaded.economy.construction.size);
        assertEquals("farm", loaded.economy.construction.first().typeId);
        assertEquals(10f, loaded.economy.construction.first().remainingCost, 0.001f);
        assertEquals(2f, loaded.economy.construction.first().remainingTicks, 0.001f);
    }

    @Test
    void savingAgainOverwrites() {
        manager.save(sampleData());
        SaveData fresh = new SaveData();
        fresh.totalStars = 5;
        manager.save(fresh);

        SaveData loaded = manager.load();
        assertEquals(5, loaded.totalStars);
        assertEquals(0, loaded.placedObjects.size);
    }

    @Test
    void emptySaveRoundTrips() {
        manager.save(new SaveData());
        SaveData loaded = manager.load();
        assertNotNull(loaded);
        assertEquals(0, loaded.totalStars);
        assertNull(loaded.currentChallengeId);
        assertEquals(0, loaded.placedObjects.size);
    }

    @Test
    void savingNullIsANoOp() throws Exception {
        manager.save(null);
        assertFalse(manager.hasSave());
    }

    @Test
    void corruptFileLoadsAsNull() throws Exception {
        Files.writeString(file.toPath(), "{ not valid json !!");
        assertNull(manager.load());
    }

    @Test
    void deleteSaveRemovesTheFile() {
        manager.save(sampleData());
        assertTrue(manager.hasSave());

        manager.deleteSave();
        assertFalse(manager.hasSave());
        assertNull(manager.load());
    }
}
