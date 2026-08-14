package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.DiscoveryManager;
import com.packs.mosaic.world.UnlockCatalog;
import com.packs.mosaic.world.UnlockDefinition;
import com.packs.mosaic.world.UnlockRequirement;

/**
 * HUD panel for the recipe/discovery interface. Shows every special
 * building: discovered entries get a gold checkmark and their name; locked
 * entries show each requirement as a progress line (green when met). Two
 * sections separate count-based discoveries from combination recipes.
 * Refreshes itself from DiscoveryManager events and via
 * {@link #refresh()} after a language switch.
 */
public class UnlockPanel extends Table {

    private static final Color MET_COLOR = new Color(0.4f, 0.9f, 0.5f, 1f);
    private static final Color DISCOVERED_COLOR = new Color(1f, 0.85f, 0.3f, 1f);
    private static final Color SECTION_COLOR = new Color(0.7f, 0.8f, 0.95f, 1f);

    private final DiscoveryManager manager;
    private final Table rows;

    public UnlockPanel(Skin skin, DiscoveryManager manager) {
        super(skin);
        this.manager = manager;

        setBackground(skin.getDrawable("panel"));
        top().left();
        defaults().pad(4f);

        Label header = new Label(LocalizationManager.tr("unlock.title").toUpperCase(), skin, "small");
        add(header).left().pad(8f, 10f, 4f, 10f).row();

        rows = new Table(skin);
        rows.top().left();
        rows.defaults().pad(2f);

        ScrollPane scroll = new ScrollPane(rows, skin);
        scroll.setFadeScrollBars(false);
        add(scroll).size(300f, 360f).pad(0f, 8f, 8f, 8f);

        manager.addListener(new DiscoveryManager.DiscoveryListener() {
            @Override
            public void onUnlocked(UnlockDefinition unlock) {
                refresh();
            }
        });
        refresh();
    }

    /** Re-reads the manager and rebuilds the entry list. Safe to call any time. */
    public void refresh() {
        rows.clearChildren();
        ObjectMap<String, Integer> counts = manager.getCurrentCounts();

        addSection(LocalizationManager.tr("unlock.discoveries"));
        for (UnlockDefinition unlock : UnlockCatalog.getDiscoveries()) {
            addEntry(unlock, counts);
        }

        addSection(LocalizationManager.tr("unlock.recipes"));
        for (UnlockDefinition unlock : UnlockCatalog.getRecipes()) {
            addEntry(unlock, counts);
        }
    }

    private void addSection(String title) {
        Label section = new Label(title.toUpperCase(), getSkin(), "small");
        section.setColor(SECTION_COLOR);
        rows.add(section).left().padTop(8f).row();
    }

    private void addEntry(UnlockDefinition unlock, ObjectMap<String, Integer> counts) {
        boolean discovered = manager.isUnlocked(unlock);
        String name = LocalizationManager.tr("building." + unlock.getRewardBuildingId());

        Label title = new Label((discovered ? "\u2713 " : "") + name, getSkin(), "small");
        title.setColor(discovered ? DISCOVERED_COLOR : Color.WHITE);
        rows.add(title).left().row();

        if (discovered) return;

        for (UnlockRequirement requirement : unlock.getRequirements()) {
            boolean met = requirement.isMet(counts);
            Label line = new Label("   " + LocalizationManager.tr(requirement.getLabelKey())
                + "  " + requirement.getProgressText(counts), getSkin(), "small");
            line.setColor(met ? MET_COLOR : line.getColor());
            rows.add(line).left().row();
        }
    }
}
