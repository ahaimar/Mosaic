package com.packs.mosaic;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.packs.mosaic.components.SkinFactory;
import com.packs.mosaic.screens.GridPrototypeScreen;

/**
 * Application entry point. Builds the shared SpriteBatch and Skin
 * once (both are expensive and stateless enough to reuse across every
 * screen), then hands off to the first Screen. Individual screens
 * receive the skin via BaseScreen's constructor.
 */
public class Main extends Game {

    private SpriteBatch batch;
    private Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = SkinFactory.createSkin();

        // Phase 1 prototype target: grid + camera + building placement.
        // Swap this for MainMenuScreen once the prototype is validated.
        setScreen(new GridPrototypeScreen(skin));
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
