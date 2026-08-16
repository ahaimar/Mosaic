package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.BuildingCatalog;
import com.packs.mosaic.world.BuildingType;
import com.packs.mosaic.world.ChallengeDefinition;
import com.packs.mosaic.world.ChallengeManager;

/**
 * Small HUD panel showing the active challenge: its title, star reward,
 * and one two-column line per requirement (name left, live placed/required
 * count right-aligned, green when met). The requirement list lives in a
 * ScrollPane so long checklists scroll instead of clipping through the card
 * boundary. Refreshes itself from ChallengeManager events, so it needs no
 * direct wiring from the screen beyond construction. All text goes through
 * LocalizationManager, and {@link #refresh()} can be called after a
 * language switch to re-render.
 */
public class ChallengePanel extends Table {

    private static final Color MET_COLOR = new Color(0.4f, 0.9f, 0.5f, 1f);
    private static final Color DEFAULT_COLOR = new Color(0.75f, 0.75f, 0.85f, 1f);
    private static final Color STAR_COLOR = new Color(1f, 0.82f, 0.35f, 1f);
    private static final float MAX_REQUIREMENTS_HEIGHT = 168f;

    private final ChallengeManager manager;
    private final Label titleLabel;
    private final Label rewardLabel;
    private final Table requirementsTable;

    public ChallengePanel(Skin skin, ChallengeManager manager) {
        super(skin);
        this.manager = manager;

        setBackground(skin.getDrawable("panel"));
        top().left();
        defaults().pad(6f);

        titleLabel = new Label("", skin, "small");
        titleLabel.setFontScale(0.82f);
        rewardLabel = new Label("", skin, "small");
        rewardLabel.setColor(STAR_COLOR);

        requirementsTable = new Table(skin);
        requirementsTable.top().left();
        requirementsTable.defaults().pad(3f);

        ScrollPane scroll = new ScrollPane(requirementsTable, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        scroll.setFadeScrollBars(false);

        add(titleLabel).left().pad(10f, 14f, 2f, 14f).row();
        add(rewardLabel).left().pad(0f, 14f, 5f, 14f).row();
        add(scroll).growX().pad(0f, 14f, 12f, 14f).maxHeight(MAX_REQUIREMENTS_HEIGHT);

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

            Color lineColor = have >= need ? MET_COLOR : DEFAULT_COLOR;

            Table row = new Table(getSkin());
            row.defaults().minHeight(24f);

            Label nameLabel = new Label(name, getSkin(), "small");
            nameLabel.setWrap(true);
            nameLabel.setAlignment(Align.left);
            nameLabel.setColor(lineColor);

            Label countLabel = new Label(have + "/" + need, getSkin(), "small");
            countLabel.setAlignment(Align.right);
            countLabel.setColor(lineColor);

            row.add(nameLabel).left().expandX().growX();
            row.add(countLabel).right().padLeft(12f);
            requirementsTable.add(row).growX().row();
        }
        setVisible(true);
    }
}
