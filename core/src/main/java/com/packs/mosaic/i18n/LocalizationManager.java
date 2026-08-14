package com.packs.mosaic.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

/**
 * Thin wrapper around libGDX {@link I18NBundle} so the rest of the game
 * asks {@code LocalizationManager.tr(key, ...)} instead of holding a
 * bundle reference. Uses the simple formatter so "{0}"-style placeholders
 * work without MessageFormat's apostrophe escaping.
 *
 * Screens and widgets that show user-facing text register a
 * {@link LocaleListener} and re-render when the player switches language
 * in Settings.
 */
public final class LocalizationManager {

    public static final String BUNDLE_PATH = "i18n/bundle";

    /** Called whenever setLocale() changes the active language. */
    public interface LocaleListener {
        void onLocaleChanged(Locale locale);
    }

    private static final Array<LocaleListener> listeners = new Array<>();
    private static I18NBundle bundle;

    private LocalizationManager() {
    }

    /** Loads the bundle for the given locale, falling back to the base bundle. */
    public static void init(Locale locale) {
        try {
            I18NBundle.setSimpleFormatter(true);
            FileHandle base = Gdx.files.internal(BUNDLE_PATH);
            bundle = locale == null
                ? I18NBundle.createBundle(base, "UTF-8")
                : I18NBundle.createBundle(base, locale, "UTF-8");
        } catch (Exception e) {
            Gdx.app.error("LocalizationManager", "Failed to load bundle: " + e.getMessage());
            bundle = null;
        }
    }

    public static boolean isReady() {
        return bundle != null;
    }

    /** Translated text for a key, formatted with the given args, or the key itself if the bundle is missing. */
    public static String tr(String key, Object... args) {
        if (bundle == null) {
            return key;
        }
        return args.length == 0 ? bundle.get(key) : bundle.format(key, args);
    }

    public static Locale getLocale() {
        return bundle == null ? Locale.getDefault() : bundle.getLocale();
    }

    /** Switches the active language and notifies listeners. No-op if already on that locale. */
    public static void setLocale(Locale locale) {
        if (bundle != null && bundle.getLocale().equals(locale)) {
            return;
        }
        init(locale);
        for (LocaleListener listener : listeners) {
            listener.onLocaleChanged(locale);
        }
    }

    public static void addListener(LocaleListener listener) {
        listeners.add(listener);
    }

    /** Removes a previously registered listener (screens unregister on hide). */
    public static void removeListener(LocaleListener listener) {
        listeners.removeValue(listener, true);
    }
}
