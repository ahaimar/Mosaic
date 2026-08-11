package com.packs.mosaic.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Common boilerplate shared by every screen: a Stage sized with a
 * FitViewport (keeps layout consistent across desktop window sizes
 * and Android device resolutions), input wiring, background clear,
 * and the shared Skin (built once by Main, not per screen). Concrete
 * screens only implement buildUi() and, if they own extra resources
 * beyond the shared skin, disposeScreen().
 */
public abstract class BaseScreen implements Screen {

    protected final Stage stage;
    protected final Skin skin;

    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;
    private static final float CLEAR_R = 0.12f;
    private static final float CLEAR_G = 0.12f;
    private static final float CLEAR_B = 0.16f;

    protected BaseScreen(Skin skin) {
        Viewport viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        this.stage = new Stage(viewport);
        this.skin = skin;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    /** Build this screen's UI elements and add them to `stage` here. */
    protected abstract void buildUi();

    @Override
    public void render(float delta) {
        ScreenUtils.clear(CLEAR_R, CLEAR_G, CLEAR_B, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        disposeScreen();
    }

    /** Override to dispose any extra resources this screen owns beyond the shared skin/stage. */
    protected void disposeScreen() {
    }
}
