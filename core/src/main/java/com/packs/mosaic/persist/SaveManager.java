package com.packs.mosaic.persist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * Reads and writes a single SaveData snapshot as JSON in local storage.
 * The FileHandle is injectable so unit tests can round-trip against a
 * temp file without a running libGDX application.
 */
public class SaveManager {

    public static final String DEFAULT_SAVE_FILE = "mosaic_save.json";

    private final FileHandle saveFile;
    private final Json json;

    public SaveManager() {
        this(Gdx.files.local(DEFAULT_SAVE_FILE));
    }

    public SaveManager(FileHandle saveFile) {
        this.saveFile = saveFile;
        this.json = new Json();
    }

    /** Writes the snapshot, overwriting any previous save. */
    public void save(SaveData data) {
        if (data == null) return;
        saveFile.writeString(json.toJson(data), false);
    }

    /** Returns the loaded snapshot, or null if there is no save (or it is unreadable). */
    public SaveData load() {
        if (!saveFile.exists()) return null;
        try {
            return json.fromJson(SaveData.class, saveFile.readString("UTF-8"));
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to load save: " + e.getMessage());
            return null;
        }
    }

    public boolean hasSave() {
        return saveFile.exists();
    }

    public void deleteSave() {
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }

    public FileHandle getSaveFile() {
        return saveFile;
    }
}
