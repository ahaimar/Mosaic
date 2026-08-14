package com.packs.mosaic.screens;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.packs.mosaic.Main;
import com.packs.mosaic.audio.AudioManager;
import com.packs.mosaic.components.LibGdxButton;
import com.packs.mosaic.components.LibGdxModel;
import com.packs.mosaic.components.LibGdxToast;
import com.packs.mosaic.components.Widgets;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.persist.GameSettings;
import com.packs.mosaic.persist.SaveManager;

import java.util.Locale;

/**
 * Settings screen: music/SFX volume sliders, the active language
 * (rebuilds the UI live when it changes), and a two-step confirm to
 * reset all progress (save file + stars). Settings persist through
 * GameSettings and are applied to AudioManager immediately.
 */
public class SettingsScreen extends BaseScreen {

    private static final String[] LANGUAGES = {"English", "Français", "العربية"};
    private static final Locale[] LOCALES = {Locale.ENGLISH, Locale.FRENCH, new Locale("ar")};

    private final Main game;
    private final LocalizationManager.LocaleListener localeListener = this::rebuild;
    private Label musicValueLabel;
    private Label sfxValueLabel;

    public SettingsScreen(Main game) {
        super(game.getSkin());
        this.game = game;
        LocalizationManager.addListener(localeListener);
    }

    @Override
    protected void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table panel = new Table(skin);
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(30f);
        panel.defaults().pad(8f);

        panel.add(Widgets.title(skin, tr("settings.title"))).colspan(3).padBottom(24f).row();

        // ── Music volume ───────────────────────────────────────────────────
        panel.add(Widgets.label(skin, tr("settings.music"))).left().width(240f);
        Slider musicSlider = new Slider(0f, 1f, 0.05f, false, skin);
        musicSlider.setValue(GameSettings.getMusicVolume());
        musicValueLabel = Widgets.smallLabel(skin, percent(GameSettings.getMusicVolume()));
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSettings.setMusicVolume(musicSlider.getValue());
                musicValueLabel.setText(percent(musicSlider.getValue()));
            }
        });
        panel.add(musicSlider).width(360f);
        panel.add(musicValueLabel).width(64f).row();

        // ── SFX volume ─────────────────────────────────────────────────────
        panel.add(Widgets.label(skin, tr("settings.sfx"))).left().width(240f);
        Slider sfxSlider = new Slider(0f, 1f, 0.05f, false, skin);
        sfxSlider.setValue(AudioManager.getInstance().getSfxVolume());
        sfxValueLabel = Widgets.smallLabel(skin, percent(AudioManager.getInstance().getSfxVolume()));
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float volume = sfxSlider.getValue();
                GameSettings.setSfxVolume(volume);
                AudioManager.getInstance().setSfxVolume(volume);
                sfxValueLabel.setText(percent(volume));
            }
        });
        panel.add(sfxSlider).width(360f);
        panel.add(sfxValueLabel).width(64f).row();

        // ── Language ───────────────────────────────────────────────────────
        panel.add(Widgets.label(skin, tr("settings.language"))).left().width(240f);
        SelectBox<String> languageBox = new SelectBox<>(skin);
        languageBox.setItems(LANGUAGES);
        languageBox.setSelectedIndex(localeIndex(GameSettings.getLocale()));
        languageBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Locale selected = LOCALES[languageBox.getSelectedIndex()];
                GameSettings.setLocale(selected);
                LocalizationManager.setLocale(selected);
            }
        });
        panel.add(languageBox).width(360f).colspan(2).row();

        // ── Reset progress ─────────────────────────────────────────────────
        panel.add(Widgets.button(skin, tr("settings.resetProgress"), "reset",
            LibGdxButton.Size.MD, this::confirmReset)).width(300f).colspan(3).padTop(28f).row();

        // ── Back ───────────────────────────────────────────────────────────
        panel.add(Widgets.button(skin, tr("settings.back"), "ghost",
            LibGdxButton.Size.MD, () -> game.setScreen(new MainMenuScreen(game))))
            .width(300f).colspan(3).padTop(12f).row();

        root.add(panel);
    }

    private void confirmReset() {
        LibGdxModel.show(stage, skin,
            tr("settings.resetConfirmTitle"),
            tr("settings.resetConfirmText"),
            tr("settings.resetYes"), this::doReset,
            tr("settings.resetNo"), null);
    }

    private void doReset() {
        new SaveManager().deleteSave();
        LibGdxToast.show(stage, skin, LibGdxToast.Kind.SUCCESS, tr("settings.progressReset"), 2f);
    }

    /** Clears the stage and rebuilds all UI with the new language. */
    private void rebuild(Locale locale) {
        if (stage.getActors().size > 0) {
            stage.clear();
            buildUi();
        }
    }

    @Override
    public void hide() {
        LocalizationManager.removeListener(localeListener);
        super.hide();
    }

    private static int localeIndex(Locale locale) {
        for (int i = 0; i < LOCALES.length; i++) {
            if (LOCALES[i].getLanguage().equals(locale.getLanguage())) return i;
        }
        return 0;
    }

    private static String percent(float value) {
        return Math.round(value * 100f) + "%";
    }

    private static String tr(String key) {
        return LocalizationManager.tr(key);
    }
}
