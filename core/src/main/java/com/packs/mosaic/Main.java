package com.packs.mosaic;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.packs.mosaic.audio.AudioManager;
import com.packs.mosaic.components.SkinFactory;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.persist.GameSettings;
import com.packs.mosaic.screens.MainMenuScreen;

public class Main extends Game {

    private SpriteBatch batch;
    private Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = SkinFactory.createSkin();

        // Apply persisted preferences: language and volumes (audio generates
        // its placeholder WAVs lazily on first play, so this is safe anywhere).
        LocalizationManager.init(GameSettings.getLocale());
        AudioManager.getInstance().setSfxVolume(GameSettings.getSfxVolume());
        AudioManager.getInstance().init();

        // Start at the main menu: New Game enters the grid, Continue loads a save.
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // delegates to the active Screen's render()
    }

    @Override
    public void resize(int width, int height) {
        if (getScreen() != null) {
            getScreen().resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        AudioManager.getInstance().dispose();
        batch.dispose();
        skin.dispose();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Skin getSkin() {
        return skin;
    }
}
