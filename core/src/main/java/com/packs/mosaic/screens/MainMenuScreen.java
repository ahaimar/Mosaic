package com.packs.mosaic.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.packs.mosaic.Main;
import com.packs.mosaic.components.Widgets;

public final class MainMenuScreen extends BaseScreen {

    private final Main game;


    public MainMenuScreen(Main game) {
        super(game.getSkin());
        this.game = game;
    }

    @Override
    protected void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = Widgets.title(skin, "titel");
        Label tagline = new Label(
            "Every mind is a piece. Every game reveals where it belongs.", skin);

        TextButton playButton = Widgets.button(skin, "Play", () ->
            Gdx.app.log("Mosaic", "Play pressed — match flow screen not built yet"));



        TextButton exitButton = Widgets.button(skin, "Exit", "reset", Gdx.app::exit);

        root.add(title).padBottom(10f).row();
        root.add(tagline).padBottom(60f).row();
        root.add(playButton).width(280f).height(60f).padBottom(16f).row();
        root.add(exitButton).width(280f).height(60f).row();
    }
}
