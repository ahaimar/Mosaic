package com.packs.mosaic.components;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.Season;

/**
 * Small HUD bar for switching the visual season (Task 4). Shows one toggle
 * button per season; exactly one is always checked. Changing the season only
 * re-themes the world — the grid and every placed building are untouched, so
 * the player never loses progress by switching.
 */
public class SeasonBar extends Table {

    /** Fired when the player picks a season from the bar. */
    public interface SeasonListener {
        void onSeasonSelected(Season season);
    }

    private final ObjectMap<Season, LibGdxButton> buttons = new ObjectMap<>();

    public SeasonBar(Skin skin, Season active, SeasonListener listener) {
        super(skin);
        setBackground(skin.getDrawable("panel"));
        defaults().pad(4f);

        Label caption = new Label(LocalizationManager.tr("hud.season").toUpperCase(), skin, "small");
        add(caption).padLeft(10f).padRight(2f);

        ButtonGroup<LibGdxButton> group = new ButtonGroup<>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        for (Season season : Season.values()) {
            LibGdxButton button = new LibGdxButton(
                LocalizationManager.tr(season.getNameKey()), "ghost", LibGdxButton.Size.SM, skin,
                () -> listener.onSeasonSelected(season));
            button.setProgrammaticChangeEvents(false);
            group.add(button);
            buttons.put(season, button);
            add(button).pad(2f);
        }
        add().padRight(10f);

        setActive(active);
    }

    /** Highlights the given season as the active one. */
    public void setActive(Season season) {
        LibGdxButton button = buttons.get(season);
        if (button != null) button.setChecked(true);
    }
}
