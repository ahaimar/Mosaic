package com.packs.mosaic.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.packs.mosaic.world.BuildingCatalog;
import com.packs.mosaic.world.BuildingPlacementController;
import com.packs.mosaic.world.BuildingType;

/**
 * Bottom-of-screen row of buttons, one per BuildingType in the catalog.
 * Tapping a button calls placementController.selectType(...). Plain
 * TextButton for now (placeholder icons per spec) — swap for
 * LibGdxButton/Widgets once real icon assets exist; only this class
 * would need to change, nothing else references TextButton directly.
 */
public class BuildingSelectionMenu extends Table {

    public BuildingSelectionMenu(Skin skin, BuildingPlacementController placementController) {
        super(skin);
        bottom();
        setFillParent(true);

        Table row = new Table(skin);
        row.defaults().pad(4);

        for (BuildingType type : BuildingCatalog.getAll()) {
            TextButton button = new TextButton(type.getDisplayName(), skin);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    placementController.selectType(type);
                }
            });
            row.add(button);
        }

        add(row).padBottom(12).align(Align.bottom);
    }

    /** Call after constructing to attach this menu to a screen's stage. */
    public void attachTo(Stage stage) {
        stage.addActor(this);
    }
}
