package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.BuildingCatalog;
import com.packs.mosaic.world.BuildingType;
import com.packs.mosaic.world.ChallengeDefinition;
import com.packs.mosaic.world.ChallengeManager;

/**
 * Small HUD panel showing the active challenge: its title, star reward,
 * and one line per requirement with live placed/required counts (green
 * when met). Refreshes itself from ChallengeManager events, so it needs
 * no direct wiring from the screen beyond construction. All text goes
 * through LocalizationManager, and {@link #refresh()} can be called after
 * a language switch to re-render.
 */
public class ChallengePanel extends Table {

    private static final Color MET_COLOR = new Color(0.4f, 0.9f, 0.5f, 1f);

    private final ChallengeManager manager;
    private final Label titleLabel;
    private final Label rewardLabel;
    private final Table requirementsTable;

    public ChallengePanel(Skin skin, ChallengeManager manager) {
        super(skin);
        this.manager = manager;

        setBackground(skin.getDrawable("panel"));
        top().left();
        defaults().pad(4f);

        titleLabel = new Label("", skin, "small");
        rewardLabel = new Label("", skin, "small");
        requirementsTable = new Table(skin);
        requirementsTable.top().left();
        requirementsTable.defaults().pad(2f);

        add(titleLabel).left().pad(6f, 10f, 2f, 10f).row();
        add(rewardLabel).left().pad(0f, 10f, 4f, 10f).row();
        add(requirementsTable).growX().pad(0f, 10f, 8f, 10f);

        manager.addListener(new ChallengeManager.ChallengeListener() {
            @Override
            public void onChallengeChanged(ChallengeDefinition challenge) {
                refresh();
            }

            @Override
            public void onChallengeProgress(ChallengeDefinition challenge) {
                refresh();
            }
        });
        refresh();
    }

    /** Re-reads the manager and rebuilds the requirement lines. Safe to call any time. */
    public void refresh() {
        ChallengeDefinition challenge = manager.getCurrentChallenge();
        requirementsTable.clearChildren();

        if (challenge == null) {
            titleLabel.setText(LocalizationManager.tr("challenge.allDone"));
            rewardLabel.setText("");
            setVisible(true);
            return;
        }

        titleLabel.setText(LocalizationManager.tr("challenge." + challenge.getId() + ".title").toUpperCase());
        rewardLabel.setText("★ " + challenge.getStarReward());

        ObjectMap<String, Integer> counts = manager.getCurrentCounts();
        for (ObjectMap.Entry<String, Integer> requirement : challenge.getRequiredCounts()) {
            BuildingType type = BuildingCatalog.get(requirement.key);
            String name = type != null
                ? LocalizationManager.tr("building." + type.getId())
                : requirement.key;
            int have = counts.get(requirement.key, 0);
            int need = requirement.value;

            Label line = new Label(name + "   " + have + "/" + need, getSkin(), "small");
            line.setColor(have >= need ? MET_COLOR : line.getColor());
            requirementsTable.add(line).left().row();
        }
        setVisible(true);
    }
}
