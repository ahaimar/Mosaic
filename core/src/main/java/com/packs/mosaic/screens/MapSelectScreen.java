package com.packs.mosaic.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.packs.mosaic.Main;
import com.packs.mosaic.components.LibGdxButton;
import com.packs.mosaic.components.MapPreviewIcon;
import com.packs.mosaic.components.Widgets;
import com.packs.mosaic.graphics.VillageBackground;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.persist.SaveData;
import com.packs.mosaic.persist.SaveManager;
import com.packs.mosaic.world.GameMap;
import com.packs.mosaic.world.MapCatalog;

/**
 * Map-selection screen (Task 3). Every world is an independent building
 * space, so the player picks where to build. Each card shows a procedural
 * preview of the world, its name and description, and how much is already
 * built there (or "New World"). Building then opens GridPrototypeScreen for
 * that map; the SAVE button always persists the whole multi-world file.
 */
public final class MapSelectScreen extends BaseScreen {

    private final Main game;
    private VillageBackground background;

    public MapSelectScreen(Main game) {
        super(game.getSkin());
        this.game = game;
    }

    @Override
    protected void buildUi() {
        background = new VillageBackground();
        background.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.addActor(background);

        SaveData save = new SaveManager().load();
        if (save == null) {
            save = new SaveData();
        }

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(Widgets.title(skin, tr("map.select.title"))).padBottom(4f).row();
        root.add(Widgets.smallLabel(skin, tr("map.select.subtitle"))).padBottom(18f).row();

        Table cards = new Table(skin);
        cards.defaults().pad(10f);
        for (GameMap map : MapCatalog.getAll()) {
            cards.add(buildCard(map, save)).width(340f).height(252f).top();
            if (cards.getChildren().size % 3 == 0) cards.row();
        }

        ScrollPane scroll = new ScrollPane(cards, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).grow().padBottom(12f).row();

        root.add(Widgets.button(skin, tr("map.back"), "ghost",
            () -> game.setScreen(new MainMenuScreen(game))))
            .width(220f).height(48f).padBottom(16f);
    }

    private Table buildCard(GameMap map, SaveData save) {
        Table card = new Table(skin);
        card.setBackground(skin.getDrawable("panel"));
        card.pad(14f);

        MapPreviewIcon preview = new MapPreviewIcon(map);
        preview.setSize(190f, 118f);
        card.add(preview).padBottom(8f).row();

        Label name = Widgets.label(skin, tr(map.getNameKey()));
        name.setFontScale(1.3f);
        card.add(name).padBottom(4f).row();

        Label description = Widgets.smallLabel(skin, tr(map.getDescriptionKey()));
        description.setWrap(true);
        description.setAlignment(Align.center);
        card.add(description).width(300f).height(34f).padBottom(6f).row();

        int buildings = countBuildings(save, map.getId());
        Label status = Widgets.smallLabel(skin, buildings > 0
            ? tr("map.select.buildings", buildings)
            : tr("map.select.newWorld"));
        card.add(status).padBottom(8f).row();

        LibGdxButton play = new LibGdxButton(
            buildings > 0 ? tr("map.select.continue") : tr("map.select.play"),
            "primary", LibGdxButton.Size.SM, skin, () -> openMap(map, save));
        card.add(play).width(150f).height(40f);

        return card;
    }

    private void openMap(GameMap map, SaveData save) {
        GridPrototypeScreen screen = new GridPrototypeScreen(game.getSkin(), map.getId(), save);
        screen.setOnBackToMaps(() -> game.setScreen(new MapSelectScreen(game)));
        game.setScreen(screen);
    }

    private static int countBuildings(SaveData save, String mapId) {
        for (SaveData.MapSaveData mapSave : save.maps) {
            if (mapSave != null && mapSave.mapId != null && mapSave.mapId.equals(mapId)) {
                return mapSave.placedObjects.size;
            }
        }
        if (mapId.equals(MapCatalog.MEADOW_ID) && save.maps.size == 0) {
            return save.placedObjects.size;
        }
        return 0;
    }

    private static String tr(String key, Object... args) {
        return LocalizationManager.tr(key, args);
    }
}
