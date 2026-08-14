package com.packs.mosaic.persist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.Locale;

/**
 * Thin wrapper over libGDX Preferences for settings that live outside the
 * save file: music/SFX volumes and the active language. Read at startup
 * by Main and written by the Settings screen.
 */
public final class GameSettings {

    private static final String NAME = "mosaic-settings";
    private static final String KEY_MUSIC = "musicVolume";
    private static final String KEY_SFX = "sfxVolume";
    private static final String KEY_LOCALE = "locale";

    private static final float DEFAULT_MUSIC = 0.7f;
    private static final float DEFAULT_SFX = 1f;

    private GameSettings() {
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(NAME);
    }

    public static float getMusicVolume() {
        return clamp(prefs().getFloat(KEY_MUSIC, DEFAULT_MUSIC));
    }

    public static void setMusicVolume(float volume) {
        prefs().putFloat(KEY_MUSIC, clamp(volume)).flush();
    }

    public static float getSfxVolume() {
        return clamp(prefs().getFloat(KEY_SFX, DEFAULT_SFX));
    }

    public static void setSfxVolume(float volume) {
        prefs().putFloat(KEY_SFX, clamp(volume)).flush();
    }

    /** Stored locale, or the system default the first time the game runs. */
    public static Locale getLocale() {
        String tag = prefs().getString(KEY_LOCALE, "");
        return tag.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(tag);
    }

    public static void setLocale(Locale locale) {
        prefs().putString(KEY_LOCALE, locale.toLanguageTag()).flush();
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
