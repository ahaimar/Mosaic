package com.packs.mosaic.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.packs.mosaic.audio.AudioManager;
import com.packs.mosaic.components.BottomToolbar;
import com.packs.mosaic.components.ChallengePanel;
import com.packs.mosaic.components.LibGdxButton;
import com.packs.mosaic.components.LibGdxToast;
import com.packs.mosaic.components.ResourceBar;
import com.packs.mosaic.components.SeasonBar;
import com.packs.mosaic.components.SparkleEffect;
import com.packs.mosaic.components.UnlockPanel;
import com.packs.mosaic.graphics.AmbientEffect;
import com.packs.mosaic.graphics.AnimalSimulation;
import com.packs.mosaic.graphics.BuildingTextureFactory;
import com.packs.mosaic.graphics.MapGroundFactory;
import com.packs.mosaic.graphics.SeasonDecorationFactory;
import com.packs.mosaic.i18n.LocalizationManager;
import com.packs.mosaic.persist.SaveData;
import com.packs.mosaic.persist.SaveManager;
import com.packs.mosaic.world.BuildingCatalog;
import com.packs.mosaic.world.BuildingObject;
import com.packs.mosaic.world.BuildingPlacementController;
import com.packs.mosaic.world.BuildingType;
import com.packs.mosaic.world.ChallengeCatalog;
import com.packs.mosaic.world.ChallengeDefinition;
import com.packs.mosaic.world.ChallengeManager;
import com.packs.mosaic.world.DiscoveryManager;
import com.packs.mosaic.world.GameMap;
import com.packs.mosaic.world.GridInputController;
import com.packs.mosaic.world.MapCatalog;
import com.packs.mosaic.world.PlayerProgress;
import com.packs.mosaic.world.Season;
import com.packs.mosaic.world.UnlockDefinition;
import com.packs.mosaic.world.VillageGrid;
import com.packs.mosaic.world.econ.EconomySimulation;

import java.util.TreeMap;

/**
 * Phase 3 screen: renders the building grid, lets the player pan/zoom the
 * camera, and place/remove buildings with live UI feedback. Buildings are
 * drawn as procedural sprites (BuildingTextureFactory) rotated to their
 * placement angle with a pop-in animation, the BottomToolbar replaces the
 * old picker, delete mode / undo / redo are exposed in the toolbar, and
 * every user-facing string is localized.
 *
 * Deliberately does NOT use BaseScreen's Stage/UI viewport for the grid
 * itself: the grid is drawn in world space via its own camera, so
 * panning/zooming the world never affects UI layout.
 */
public class GridPrototypeScreen extends BaseScreen {

    public static final int GRID_COLS = 20;
    public static final int GRID_ROWS = 12;
    public static final float CELL_SIZE = 64f;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.12f;
    private static final float POP_DURATION = 0.25f;
    private static final float POP_START_SCALE = 0.6f;

    private final OrthographicCamera worldCamera;
    private final Viewport worldViewport;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final Texture groundTileTexture;
    private final MapGroundFactory mapGroundFactory;
    private final BuildingTextureFactory textureFactory;
    private final VillageGrid grid;
    private final PlayerProgress progress;
    private final BuildingPlacementController placementController;
    private final ChallengeManager challengeManager;
    private final DiscoveryManager discoveryManager;
    private final SaveManager saveManager;
    private final ObjectMap<BuildingObject, Float> placedAt = new ObjectMap<>();
    private final String mapId;
    private final GameMap map;
    private final AmbientEffect ambientEffect;
    private final SaveData loadedSave;
    private final SeasonDecorationFactory seasonDecorationFactory;
    private final Texture seasonOverlayTexture;
    private final AnimalSimulation animalSimulation;
    private final EconomySimulation economy;
    private Season season;
    private SeasonBar seasonBar;
    private GridInputController inputController;
    private BottomToolbar toolbar;
    private ChallengePanel challengePanel;
    private UnlockPanel unlockPanel;
    private SparkleEffect sparkle;
    private SparkleEffect unlockSparkle;
    private Runnable onBackToMaps;

    private ResourceBar resourceBar;
    private Label selectionLabel;
    private float elapsedTime;

    public GridPrototypeScreen(Skin skin) {
        this(skin, null, null);
    }

    public GridPrototypeScreen(Skin skin, SaveData save) {
        this(skin, null, save);
    }

    public GridPrototypeScreen(Skin skin, String mapId, SaveData save) {
        super(skin);
        this.mapId = mapId != null ? mapId : MapCatalog.MEADOW_ID;
        this.map = MapCatalog.get(this.mapId);
        this.loadedSave = save;

        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE, worldCamera);
        // Center camera on the grid so it's fully visible on start.
        worldCamera.position.set(GRID_COLS * CELL_SIZE / 2f, GRID_ROWS * CELL_SIZE / 2f, 0);

        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        textureFactory = new BuildingTextureFactory();
        // Dedicated 64x64 ground tile (power-of-two, pre-downsampled 3:1 from the
        // 192px source in Tileset.png) so tiles render at 1:1 with no GPU minification
        // blur. Mipmaps keep the ground crisp when zoomed out. The meadow keeps this
        // original tile untouched; the other worlds get a procedural tile per map.
        groundTileTexture = new Texture(Gdx.files.internal("texture/tile/ground.png"), Pixmap.Format.RGBA8888, true);
        groundTileTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        mapGroundFactory = new MapGroundFactory();
        seasonDecorationFactory = new SeasonDecorationFactory();
        // 1x1 white quad, tinted with the season's overlay colour to wash the whole ground.
        Pixmap whitePixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixel.setColor(Color.WHITE);
        whitePixel.fill();
        seasonOverlayTexture = new Texture(whitePixel);
        whitePixel.dispose();
        season = save != null ? Season.byId(save.season) : Season.getDefault();
        grid = new VillageGrid(GRID_COLS, GRID_ROWS, CELL_SIZE);
        progress = save != null ? new PlayerProgress(save.totalStars) : new PlayerProgress();
        placementController = new BuildingPlacementController(grid);
        if (save != null) {
            restoreSave(save);
        }
        discoveryManager = new DiscoveryManager(grid);
        if (save != null) {
            discoveryManager.restoreUnlocked(save.discoveredUnlocks);
        }
        String challengeId = save != null && save.currentChallengeId != null
            ? save.currentChallengeId
            : ChallengeCatalog.getFirst().getId();
        challengeManager = new ChallengeManager(grid, progress, challengeId);
        saveManager = new SaveManager();
        economy = new EconomySimulation(save != null ? save.economy : null);
        syncEconomyBuildings(true);
        ambientEffect = new AmbientEffect(season.getEffect(), GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE);
        animalSimulation = new AnimalSimulation(GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE);
        animalSimulation.spawnDefault(mapId);
    }

    /** Restores this map's own building space from a save (legacy saves belong to the meadow). */
    private void restoreSave(SaveData save) {
        Array<SaveData.PlacedObject> objects = null;
        for (SaveData.MapSaveData mapSave : save.maps) {
            if (mapSave != null && mapSave.mapId != null && mapSave.mapId.equals(mapId)) {
                objects = mapSave.placedObjects;
                break;
            }
        }
        if (objects == null && mapId.equals(MapCatalog.MEADOW_ID) && save.maps.size == 0) {
            objects = save.placedObjects; // pre-Task-3 save: the whole village was on the meadow
        }
        if (objects == null) return;
        for (SaveData.PlacedObject placed : objects) {
            if (placed == null) continue;
            BuildingType type = BuildingCatalog.get(placed.typeId);
            if (type == null) continue;
            if (!canFit(type, placed.col, placed.row)) continue;
            placementController.placeRestored(type, placed.col, placed.row, placed.rotationDegrees);
        }
    }

    private boolean canFit(BuildingType type, int col, int row) {
        for (int dc = 0; dc < type.getWidthCells(); dc++) {
            for (int dr = 0; dr < type.getHeightCells(); dr++) {
                if (!grid.isCellFree(col + dc, row + dr)) return false;
            }
        }
        return true;
    }

    @Override
    protected void buildUi() {
        inputController = new GridInputController(worldCamera, worldViewport, grid, MIN_ZOOM, MAX_ZOOM, placementController);

        buildHud();
        buildResourceBar();
        buildSideControls();
        buildSeasonOverlay();

        challengePanel = new ChallengePanel(skin, challengeManager);
        buildChallengeOverlay();

        sparkle = new SparkleEffect();
        challengePanel.addActor(sparkle);
        sparkle.setPosition(140f, 0f);
        sparkle.setOrigin(Align.center);

        unlockPanel = new UnlockPanel(skin, discoveryManager);
        unlockPanel.setVisible(false);
        buildUnlockOverlay();

        unlockSparkle = new SparkleEffect();
        unlockPanel.addActor(unlockSparkle);
        unlockSparkle.setPosition(150f, 40f);
        unlockSparkle.setOrigin(Align.center);

        toolbar = new BottomToolbar(skin, placementController, progress, discoveryManager);
        toolbar.setMapId(mapId);
        toolbar.setOnDeleteModeChanged(() ->
            LibGdxToast.show(stage, skin, LibGdxToast.Kind.INFO,
                placementController.isDeleteMode() ? tr("toolbar.deleteHint") : "", 1.6f));
        toolbar.attachTo(stage);

        wirePlacementFeedback();
        wireChallengeFeedback();
        wireDiscoveryFeedback();

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(stage);          // UI first — buttons consume their own touches
        multiplexer.addProcessor(inputController); // grid gets whatever the UI didn't want
        Gdx.input.setInputProcessor(multiplexer);

        updateStatus();
        updateSelectionLabel();
    }

    private void buildHud() {
        Table hud = new Table(skin);
        hud.setBackground(skin.getDrawable("panel"));
        hud.defaults().pad(8f);

        Label title = new Label("MOSAIC", skin, "title");
        title.setFontScale(1.35f);
        hud.add(title).left().padLeft(16f);

        Label subtitle = new Label(tr("map." + mapId + ".name").toUpperCase(), skin, "small");
        hud.add(subtitle).left().padLeft(12f);

        selectionLabel = new Label("", skin, "small");
        hud.add(selectionLabel).left().expandX().padLeft(20f);

        LibGdxButton saveButton = new LibGdxButton("SAVE", "save", LibGdxButton.Size.SM, skin, this::saveGame);
        hud.add(saveButton).right().top().width(92f).height(36f).padRight(16f);

        LibGdxButton mapsButton = new LibGdxButton(tr("map.menu"), "ghost", LibGdxButton.Size.SM, skin, () -> {
            if (onBackToMaps != null) onBackToMaps.run();
        });
        hud.add(mapsButton).right().top().width(100f).height(36f).padRight(10f);

        LibGdxButton unlockButton = new LibGdxButton(tr("unlock.menu"), "secondary", LibGdxButton.Size.SM, skin, () -> {
            unlockPanel.setVisible(!unlockPanel.isVisible());
            AudioManager.getInstance().play(AudioManager.Sfx.CLICK);
        });
        hud.add(unlockButton).right().top().width(140f).height(36f).padRight(10f);

        Table overlay = new Table(skin);
        overlay.setFillParent(true);
        overlay.top();
        overlay.add(hud).growX().height(72f).pad(16f, 20f, 0f, 20f);
        stage.addActor(overlay);
    }

    /** Full-width economy strip (grouped container chips) under the HUD row. */
    private void buildResourceBar() {
        resourceBar = new ResourceBar(skin, economy, progress);
        Table overlay = new Table(skin);
        overlay.setFillParent(true);
        overlay.top();
        overlay.add(resourceBar).growX().pad(96f, 20f, 0f, 20f);
        stage.addActor(overlay);
    }

    private void buildChallengeOverlay() {
        Table overlay = new Table(skin);
        overlay.setFillParent(true);
        overlay.top().left();
        overlay.add(challengePanel).width(280f).pad(168f, 20f, 0f, 0f);
        stage.addActor(overlay);
    }

    private void buildUnlockOverlay() {
        Table overlay = new Table(skin);
        overlay.setFillParent(true);
        overlay.top().right();
        overlay.add(unlockPanel).width(316f).pad(168f, 0f, 0f, 20f);
        stage.addActor(overlay);
    }

    private void buildSeasonOverlay() {
        seasonBar = new SeasonBar(skin, season, newSeason -> {
            if (newSeason == season) return;
            season = newSeason;
            ambientEffect.set(newSeason.getEffect(), GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE);
            LibGdxToast.show(stage, skin, LibGdxToast.Kind.INFO, tr(newSeason.getNameKey()), 1.6f);
        });
        Table overlay = new Table(skin);
        overlay.setFillParent(true);
        overlay.top();
        overlay.add(seasonBar).padTop(170f);
        stage.addActor(overlay);
    }

    private void buildSideControls() {
        Table side = new Table(skin);
        side.setFillParent(true);
        side.right().center();
        LibGdxButton zoomIn = new LibGdxButton("+", "ghost", LibGdxButton.Size.SM, skin,
            () -> inputController.setZoom(worldCamera.zoom - ZOOM_STEP));
        LibGdxButton zoomOut = new LibGdxButton("-", "ghost", LibGdxButton.Size.SM, skin,
            () -> inputController.setZoom(worldCamera.zoom + ZOOM_STEP));

        side.add(zoomIn).size(46f).padBottom(6f).row();
        side.add(zoomOut).size(46f).padRight(10f).row();
        stage.addActor(side);
    }

    private void wirePlacementFeedback() {
        placementController.addListener(new BuildingPlacementController.PlacementListener() {
            @Override
            public void onSelectionChanged(BuildingType type) {
                updateSelectionLabel();
            }

            @Override
            public void onPlaced(BuildingType type, int col, int row) {
                BuildingObject object = objectAt(col, row);
                if (object != null) {
                    placedAt.put(object, elapsedTime);
                }
                onGridChanged();
                AudioManager.getInstance().play(AudioManager.Sfx.PLACE);
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.SUCCESS,
                    tr("building." + type.getId()) + " " + tr("hud.placed"), 1.6f);
            }

            @Override
            public void onPlacementBlocked(BuildingType type, int col, int row) {
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.WARNING,
                    tr("hud.cantBuild"), 1.8f);
            }

            @Override
            public void onDeleted(int col, int row) {
                onGridChanged();
                AudioManager.getInstance().play(AudioManager.Sfx.DELETE);
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.INFO,
                    tr("hud.removed"), 1.4f);
            }

            @Override
            public void onDeleteBlocked(int col, int row) {
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.WARNING,
                    tr("hud.nothingToRemove"), 1.4f);
            }
        });
    }

    private void wireChallengeFeedback() {
        challengeManager.addListener(new ChallengeManager.ChallengeListener() {
            @Override
            public void onChallengeChanged(ChallengeDefinition challenge) {
                updateStatus();
            }

            @Override
            public void onChallengeCompleted(ChallengeDefinition challenge) {
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.SUCCESS,
                    tr("challenge.done") + "  +" + challenge.getStarReward() + " ★", 2.4f);
                AudioManager.getInstance().play(AudioManager.Sfx.CHALLENGE_COMPLETE);
                AudioManager.getInstance().play(AudioManager.Sfx.STAR);
                sparkle.play();
                starBounce();
                toolbar.refresh();
                updateStatus();
            }
        });
    }

    private void wireDiscoveryFeedback() {
        discoveryManager.addListener(new DiscoveryManager.DiscoveryListener() {
            @Override
            public void onUnlocked(UnlockDefinition unlock) {
                LibGdxToast.show(stage, skin, LibGdxToast.Kind.SUCCESS,
                    tr("unlock.discovered") + "  " + tr("building." + unlock.getRewardBuildingId()), 2.6f);
                AudioManager.getInstance().play(AudioManager.Sfx.DISCOVERY);
                unlockPanel.setVisible(true);
                unlockSparkle.play();
                toolbar.refresh();
                unlockPanel.refresh();
            }
        });
    }

    /** Called after any grid mutation: re-check the challenge, discoveries and refresh HUD. */
    private void onGridChanged() {
        challengeManager.checkCompletion();
        discoveryManager.checkUnlocks();
        syncEconomyBuildings(false);
        updateStatus();
    }

    /**
     * Keeps the economic simulation in step with the buildings currently on
     * the grid. Runs after placement/delete/undo/redo (alreadyBuilt = false,
     * so new economic buildings enter construction) and once at load
     * (alreadyBuilt = true, so restored buildings are finished already).
     */
    private void syncEconomyBuildings(boolean alreadyBuilt) {
        TreeMap<String, Integer> counts = new TreeMap<>();
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
                if (occupant instanceof BuildingObject) {
                    BuildingObject obj = (BuildingObject) occupant;
                    if (obj.getOriginCol() == col && obj.getOriginRow() == row) {
                        counts.merge(obj.getType().getId(), 1, Integer::sum);
                    }
                }
            }
        }
        economy.reconcileBuildings(counts, alreadyBuilt);
    }

    private BuildingObject objectAt(int col, int row) {
        VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
        return occupant instanceof BuildingObject ? (BuildingObject) occupant : null;
    }

    private void starBounce() {
        resourceBar.bounceStars();
    }

    private void updateSelectionLabel() {
        BuildingType type = placementController.getSelectedType();
        String name = type == null ? tr("hud.none")
            : LocalizationManager.tr("building." + type.getId()).toUpperCase();
        selectionLabel.setText(tr("hud.selected") + "  " + name);
    }

    private void updateStatus() {
        resourceBar.refresh();
    }

    private void saveGame() {
        saveManager.save(toSaveData());
        AudioManager.getInstance().play(AudioManager.Sfx.CLICK);
        LibGdxToast.show(stage, skin, LibGdxToast.Kind.SUCCESS, tr("hud.saved"), 1.6f);
    }

    /** Back-navigation hook so the owning menu can open the map selection. */
    public void setOnBackToMaps(Runnable onBackToMaps) {
        this.onBackToMaps = onBackToMaps;
    }

    /** Snapshots the current world state for persistence. */
    public SaveData toSaveData() {
        SaveData data = loadedSave != null ? loadedSave : new SaveData();
        data.totalStars = progress.getTotalStars();
        ChallengeDefinition challenge = challengeManager.getCurrentChallenge();
        data.currentChallengeId = challenge != null ? challenge.getId() : null;
        data.discoveredUnlocks = discoveryManager.getUnlockedIds();
        data.season = season.getId();
        data.economy = economy.toState();

        Array<SaveData.PlacedObject> current = collectPlacedObjects();
        data.placedObjects = current;

        SaveData.MapSaveData entry = null;
        for (SaveData.MapSaveData mapSave : data.maps) {
            if (mapSave != null && mapSave.mapId != null && mapSave.mapId.equals(mapId)) {
                entry = mapSave;
                break;
            }
        }
        if (entry == null) {
            entry = new SaveData.MapSaveData();
            entry.mapId = mapId;
            data.maps.add(entry);
        }
        entry.placedObjects = current;
        return data;
    }

    private Array<SaveData.PlacedObject> collectPlacedObjects() {
        Array<SaveData.PlacedObject> placed = new Array<>();
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
                if (occupant instanceof BuildingObject) {
                    BuildingObject obj = (BuildingObject) occupant;
                    if (obj.getOriginCol() == col && obj.getOriginRow() == row) {
                        placed.add(new SaveData.PlacedObject(
                            obj.getType().getId(), col, row, obj.getRotationDegrees()));
                    }
                }
            }
        }
        return placed;
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;
        com.badlogic.gdx.utils.ScreenUtils.clear(map.getClearColor());
        inputController.update(delta);
        worldCamera.update();
        spriteBatch.setProjectionMatrix(worldCamera.combined);

        boolean isMeadow = mapId.equals(MapCatalog.MEADOW_ID);
        Texture ground = isMeadow ? groundTileTexture : mapGroundFactory.get(map);
        Texture seasonDecorations = seasonDecorationFactory.get(season);

        spriteBatch.begin();
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                spriteBatch.draw(ground, col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                spriteBatch.draw(seasonDecorations, col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        // Season overlay tint washes the whole ground (e.g. snow cover in winter).
        spriteBatch.setColor(season.getOverlayColor());
        spriteBatch.draw(seasonOverlayTexture, 0, 0, GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
        drawPlacedObjects();
        drawPlacementPreview();
        spriteBatch.end();

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        drawGridLines();
        drawGridFrame();
        drawHoverHighlight();

        ambientEffect.update(delta);
        animalSimulation.update(delta);
        economy.update(delta);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        animalSimulation.render(shapeRenderer);
        ambientEffect.render(shapeRenderer);
        shapeRenderer.end();

        stage.act(delta);
        stage.draw();
    }

    private void drawGridLines() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(map.getGridLineColor());
        for (int col = 0; col <= GRID_COLS; col++) {
            float x = col * CELL_SIZE;
            shapeRenderer.line(x, 0, x, GRID_ROWS * CELL_SIZE);
        }
        for (int row = 0; row <= GRID_ROWS; row++) {
            float y = row * CELL_SIZE;
            shapeRenderer.line(0, y, GRID_COLS * CELL_SIZE, y);
        }
        shapeRenderer.end();
    }

    /** Dark frame around the play field so the map reads as a clean, distinct surface. */
    private void drawGridFrame() {
        float w = GRID_COLS * CELL_SIZE;
        float h = GRID_ROWS * CELL_SIZE;
        float thickness = 7f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.05f, 0.05f, 0.09f, 0.92f));
        shapeRenderer.rect(-thickness, -thickness, w + 2f * thickness, thickness);
        shapeRenderer.rect(-thickness, h, w + 2f * thickness, thickness);
        shapeRenderer.rect(-thickness, 0f, thickness, h);
        shapeRenderer.rect(w, 0f, thickness, h);
        shapeRenderer.setColor(new Color(0.90f, 0.90f, 1f, 0.22f));
        shapeRenderer.rect(-2f, -2f, w + 4f, 2f);
        shapeRenderer.rect(-2f, h, w + 4f, 2f);
        shapeRenderer.rect(-2f, 0f, 2f, h);
        shapeRenderer.rect(w, 0f, 2f, h);
        shapeRenderer.end();
    }

    private void drawHoverHighlight() {
        int col = placementController.getHoverCol();
        int row = placementController.getHoverRow();
        if (!grid.isInBounds(col, row)) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.08f);
        shapeRenderer.rect(grid.cellToWorldX(col), grid.cellToWorldY(row), CELL_SIZE, CELL_SIZE);
        shapeRenderer.setColor(1f, 1f, 1f, 0.35f);
        shapeRenderer.rectLine(
            grid.cellToWorldX(col), grid.cellToWorldY(row),
            grid.cellToWorldX(col + 1), grid.cellToWorldY(row), 2f);
        shapeRenderer.rectLine(
            grid.cellToWorldX(col + 1), grid.cellToWorldY(row),
            grid.cellToWorldX(col + 1), grid.cellToWorldY(row + 1), 2f);
        shapeRenderer.rectLine(
            grid.cellToWorldX(col + 1), grid.cellToWorldY(row + 1),
            grid.cellToWorldX(col), grid.cellToWorldY(row + 1), 2f);
        shapeRenderer.rectLine(
            grid.cellToWorldX(col), grid.cellToWorldY(row + 1),
            grid.cellToWorldX(col), grid.cellToWorldY(row), 2f);
        shapeRenderer.end();
    }

    /** Draws every placed building as its procedural sprite, rotated and with a pop-in scale. */
    private void drawPlacedObjects() {
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
                if (occupant instanceof BuildingObject) {
                    BuildingObject obj = (BuildingObject) occupant;
                    if (obj.getOriginCol() == col && obj.getOriginRow() == row) {
                        drawObjectSprite(obj, grid.cellToWorldX(col), grid.cellToWorldY(row), 1f, 1f, 1f, 1f);
                    }
                }
            }
        }
    }

    /** Draws a single building's sprite at its footprint, applying rotation and pop-in. */
    private void drawObjectSprite(BuildingObject obj, float x, float y,
                                  float r, float g, float b, float a) {
        BuildingType type = obj.getType();
        float cell = grid.getCellSize();
        int rotation = obj.getRotationDegrees();
        boolean swapped = rotation == 90 || rotation == 270;
        float w = type.getWidthCells() * cell;
        float h = type.getHeightCells() * cell;
        if (swapped) {
            float temp = w;
            w = h;
            h = temp;
        }

        float scale = 1f;
        Float placedTime = placedAt.get(obj);
        if (placedTime != null) {
            float elapsed = elapsedTime - placedTime;
            if (elapsed < POP_DURATION) {
                float progress = MathUtils.clamp(elapsed / POP_DURATION, 0f, 1f);
                scale = POP_START_SCALE + (1f - POP_START_SCALE) * Interpolation.swingOut.apply(progress);
            } else {
                placedAt.remove(obj);
            }
        }

        Texture texture = textureFactory.get(type, season);
        spriteBatch.setColor(r, g, b, a);
        spriteBatch.draw(texture, x, y, w / 2f, h / 2f, w, h, scale, scale, rotation,
            0, 0, texture.getWidth(), texture.getHeight(), false, false);
        spriteBatch.setColor(1f, 1f, 1f, 1f);
    }

    /** Draws the translucent sprite preview under the cursor, tinted green/red by validity. */
    private void drawPlacementPreview() {
        BuildingType type = placementController.getSelectedType();
        int col = placementController.getHoverCol();
        int row = placementController.getHoverRow();
        if (type == null || placementController.isDeleteMode() || !grid.isInBounds(col, row)) {
            return;
        }

        boolean valid = placementController.isPlacementValid(col, row);
        float tint = 0.6f;
        if (valid) {
            drawObjectSprite(new BuildingObject(type, col, row, placementController.getRotationDegrees()),
                grid.cellToWorldX(col), grid.cellToWorldY(row),
                0.6f, 1f, 0.6f, tint);
        } else {
            drawObjectSprite(new BuildingObject(type, col, row, placementController.getRotationDegrees()),
                grid.cellToWorldX(col), grid.cellToWorldY(row),
                1f, 0.4f, 0.4f, tint);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height, false);
    }

    @Override
    protected void disposeScreen() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        groundTileTexture.dispose();
        mapGroundFactory.dispose();
        seasonDecorationFactory.dispose();
        seasonOverlayTexture.dispose();
        animalSimulation.dispose();
        textureFactory.dispose();
        grid.dispose();
    }

    private static String tr(String key) {
        return LocalizationManager.tr(key);
    }
}
