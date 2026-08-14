package com.packs.mosaic.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.packs.mosaic.Main;
import com.packs.mosaic.components.LibGdxButton;
import com.packs.mosaic.components.LibGdxToast;
import com.packs.mosaic.components.Widgets;
import com.packs.mosaic.graphics.VillageBackground;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.persist.SaveManager;

public final class MainMenuScreen extends BaseScreen {

    private final Main game;
    private VillageBackground background;

    public MainMenuScreen(Main game) {
        super(game.getSkin());
        this.game = game;
    }

    @Override
    protected void buildUi() {
        background = new VillageBackground();
        background.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = Widgets.title(skin, "MOSAIC");
        Label tagline = new Label(tr("app.tagline"), skin);

        LibGdxButton newGameButton = Widgets.button(skin, tr("menu.newGame"), () ->
            game.setScreen(new MapSelectScreen(game)));

        LibGdxButton continueButton = Widgets.button(skin, tr("menu.continue"), "secondary", () -> {
            if (new SaveManager().hasSave()) {
                game.setScreen(new MapSelectScreen(game));
            } else {
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.INFO, tr("menu.noSave"), 2f);
            }
        });

        LibGdxButton settingsButton = Widgets.button(skin, tr("menu.settings"), "ghost",
            () -> game.setScreen(new SettingsScreen(game)));

        LibGdxButton exitButton = Widgets.button(skin, tr("menu.exit"), "reset", Gdx.app::exit);

        root.add(title).padBottom(10f).row();
        root.add(tagline).padBottom(60f).row();
        root.add(newGameButton).width(280f).height(60f).padBottom(16f).row();
        root.add(continueButton).width(280f).height(60f).padBottom(16f).row();
        root.add(settingsButton).width(280f).height(60f).padBottom(16f).row();
        root.add(exitButton).width(280f).height(60f).row();
    }

    @Override
    protected void disposeScreen() {
        if (background != null) {
            background.dispose();
        }
    }

    private static String tr(String key) {
        return LocalizationManager.tr(key);
    }
}
