package com.packs.mosaic.components;

import java.util.EnumMap;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.world.BuildingCatalog;
import com.packs.mosaic.world.BuildingPlacementController;
import com.packs.mosaic.world.BuildingType;
import com.packs.mosaic.world.DiscoveryManager;
import com.packs.mosaic.world.PlayerProgress;

/**
 * Phase 3 bottom toolbar (replaces BuildingSelectionMenu). Always-visible
 * bar with:
 *   left  — action buttons: undo, redo, rotate, delete (toggle)
 *   right — category tabs plus a horizontal row of large icon buttons,
 *           one per unlocked BuildingType.
 *
 * Types are gated by PlayerProgress; {@link #refresh()} rebuilds the
 * option buttons after stars change or language switches. Selecting a
 * type exits delete mode, and undo/redo keep the button states in sync
 * via the placement controller's command history.
 */
public class BottomToolbar extends Table {

    private final Skin skin;
    private final BuildingPlacementController placementController;
    private final PlayerProgress progress;
    private final DiscoveryManager discoveryManager;
    private String mapId;

    private final ButtonGroup<LibGdxButton> selectionGroup = new ButtonGroup<>();
    private final ObjectMap<BuildingType, LibGdxButton> typeButtons = new ObjectMap<>();
    private final EnumMap<BuildingType.Category, Table> categoryRows =
        new EnumMap<>(BuildingType.Category.class);
    private Table options;
    private Table categoryTabs;
    private final Label hintLabel;

    private LibGdxButton deleteButton;
    private LibGdxButton undoButton;
    private LibGdxButton redoButton;

    private BuildingType.Category activeCategory = BuildingType.Category.BUILDING;
    private Runnable onDeleteModeChanged;

    public BottomToolbar(Skin skin, BuildingPlacementController placementController, PlayerProgress progress) {
        this(skin, placementController, progress, null);
    }

    public BottomToolbar(Skin skin, BuildingPlacementController placementController, PlayerProgress progress,
                         DiscoveryManager discoveryManager) {
        super(skin);
        this.skin = skin;
        this.placementController = placementController;
        this.progress = progress;
        this.discoveryManager = discoveryManager;

        bottom();
        setFillParent(true);

        Table panel = new Table(skin);
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(8f);

        buildActions();
        buildCategories();

        Table right = new Table(skin);
        right.add(categoryTabs).growX().padBottom(4f).row();
        right.add(new ScrollPane(options, skin)).growX().height(78f).row();
        hintLabel = new Label("", skin, "small");
        right.add(hintLabel).left().padTop(3f);

        Table actions = new Table(skin);
        actions.defaults().size(104f, 40f).pad(2f);
        actions.add(undoButton);
        actions.add(redoButton).row();
        actions.add(rotateButton());
        actions.add(deleteButton);

        panel.add(actions).left().padRight(16f);
        panel.add(right).expandX().fill().align(Align.bottomLeft);

        add(panel).growX().align(Align.bottom);

        placementController.addListener(new BuildingPlacementController.PlacementListener() {
            @Override
            public void onSelectionChanged(BuildingType type) {
                syncSelection(type);
            }
        });

        refresh();
    }

    private void buildActions() {
        undoButton = new LibGdxButton(tr("toolbar.undo"), "ghost", LibGdxButton.Size.SM, skin, () -> {
            placementController.undo();
            refresh();
        });
        redoButton = new LibGdxButton(tr("toolbar.redo"), "ghost", LibGdxButton.Size.SM, skin, () -> {
            placementController.redo();
            refresh();
        });
        deleteButton = new LibGdxButton(tr("toolbar.delete"), "reset", LibGdxButton.Size.SM, skin, this::toggleDeleteMode);
        deleteButton.setChecked(placementController.isDeleteMode());
    }

    private LibGdxButton rotateButton() {
        return new LibGdxButton(tr("toolbar.rotate"), "secondary", LibGdxButton.Size.SM, skin,
            () -> placementController.rotateSelection());
    }

    private void buildCategories() {
        categoryTabs = new Table(skin);
        categoryTabs.defaults().pad(2f, 4f, 2f, 4f);

        options = new Table(skin);
        options.defaults().pad(4f);
    }

    private void toggleDeleteMode() {
        boolean next = !placementController.isDeleteMode();
        placementController.setDeleteMode(next);
        deleteButton.setChecked(next);
        if (onDeleteModeChanged != null) {
            onDeleteModeChanged.run();
        }
    }

    /** Optional hook so the owning screen can toast about delete mode. */
    public void setOnDeleteModeChanged(Runnable onDeleteModeChanged) {
        this.onDeleteModeChanged = onDeleteModeChanged;
    }

    /**
     * Restricts the shown buildings to one world (Task 3): map-specific types
     * only appear while building on their own map. Pass null to show every
     * unlocked type. Call before {@link #refresh()}.
     */
    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    /** Rebuilds option buttons from the current unlock set and re-syncs selection/hints. */
    public void refresh() {
        rebuildOptions();
        syncSelection(placementController.getSelectedType());
        updateNextUnlock();
        updateActions();
    }

    private void rebuildOptions() {
        categoryRows.clear();
        options.clearChildren();
        typeButtons.clear();
        selectionGroup.getButtons().clear();
        selectionGroup.setMinCheckCount(0);
        selectionGroup.setMaxCheckCount(1);
        selectionGroup.setUncheckLast(true);

        Array<BuildingType> unlocked = new Array<>(progress.getUnlockedTypes());
        if (discoveryManager != null) {
            unlocked.addAll(discoveryManager.getRewardUnlockedTypes());
        }

        for (BuildingType.Category category : BuildingType.Category.values()) {
            Table row = new Table(skin);
            row.defaults().pad(3f);

            boolean hasAny = false;
            for (BuildingType type : unlocked) {
                if (type.getCategory() != category) continue;
                if (!type.isAvailableOn(mapId)) continue;

                String variant = category == BuildingType.Category.ENVIRONMENT
                    ? "secondary" : "primary";
                LibGdxButton button = new LibGdxButton(
                    tr("building." + type.getId()), variant, LibGdxButton.Size.SM, skin, () -> {
                        if (placementController.isDeleteMode()) {
                            placementController.setDeleteMode(false);
                            deleteButton.setChecked(false);
                        }
                        placementController.selectType(type);
                    });

                Table option = new Table(skin);
                option.add(new CampusZoneIcon(type.getId())).size(36f).row();
                option.add(button).minWidth(128f).height(44f);
                row.add(option).pad(2f, 4f, 2f, 4f);

                selectionGroup.add(button);
                typeButtons.put(type, button);
                hasAny = true;
            }

            if (hasAny) {
                categoryRows.put(category, row);
                row.setVisible(category == activeCategory);
                options.add(row).growX();
            }
        }

        categoryTabs.clearChildren();
        ButtonGroup<LibGdxButton> tabGroup = new ButtonGroup<>();
        tabGroup.setMinCheckCount(1);
        tabGroup.setMaxCheckCount(1);
        tabGroup.setUncheckLast(true);

        for (BuildingType.Category category : categoryRows.keySet()) {
            LibGdxButton tab = new LibGdxButton(categoryLabel(category), "ghost", LibGdxButton.Size.SM, skin);
            tab.setOnClick(() -> {
                activeCategory = category;
                for (java.util.Map.Entry<BuildingType.Category, Table> entry : categoryRows.entrySet()) {
                    entry.getValue().setVisible(entry.getKey() == category);
                }
                options.invalidateHierarchy();
            });
            tabGroup.add(tab);
            categoryTabs.add(tab).growX();
        }
        if (tabGroup.getButtons().size > 0) {
            int target = 0;
            int i = 0;
            for (BuildingType.Category category : categoryRows.keySet()) {
                if (category == activeCategory) target = i;
                i++;
            }
            tabGroup.getButtons().get(target).setChecked(true);
        }
        options.invalidateHierarchy();
    }

    private void syncSelection(BuildingType type) {
        for (ObjectMap.Entry<BuildingType, LibGdxButton> entry : typeButtons) {
            entry.value.setChecked(entry.key == type);
        }
    }

    private void updateActions() {
        undoButton.setDisabled(!placementController.canUndo());
        redoButton.setDisabled(!placementController.canRedo());
        deleteButton.setChecked(placementController.isDeleteMode());
    }

    private void updateNextUnlock() {
        int needed = progress.starsUntilNextUnlock();
        if (needed == -1) {
            hintLabel.setText("");
            return;
        }
        BuildingType next = null;
        for (BuildingType type : BuildingCatalog.getAll()) {
            if (type.isDiscoveryReward()) continue;
            if (!type.isAvailableOn(mapId)) continue;
            if (type.getStarsToUnlock() > progress.getTotalStars()
                && (next == null || type.getStarsToUnlock() < next.getStarsToUnlock())) {
                next = type;
            }
        }
        hintLabel.setText(tr("hud.nextUnlock", next == null ? needed : next.getStarsToUnlock()));
    }

    private static String categoryLabel(BuildingType.Category category) {
        switch (category) {
            case BUILDING:        return tr("category.building");
            case ENVIRONMENT:     return tr("category.environment");
            case INFRASTRUCTURE:  return tr("category.infrastructure");
            default:              return "";
        }
    }

    private static String tr(String key, Object... args) {
        return LocalizationManager.tr(key, args);
    }

    /** Call after constructing to attach this toolbar to a screen's stage. */
    public void attachTo(Stage stage) {
        stage.addActor(this);
    }
}
