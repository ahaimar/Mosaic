package com.packs.mosaic.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.packs.mosaic.world.BuildingPlacementController;
import com.packs.mosaic.world.GridInputController;
import com.packs.mosaic.world.VillageGrid;

/**
 * Phase 1 prototype screen: renders the building grid and lets the
 * player pan/zoom the camera. No building placement yet — that needs
 * GridInputController wired to a BuildingManager (next classes).
 *
 * Deliberately does NOT use BaseScreen's Stage/UI viewport for the
 * grid itself: the grid is drawn in world space via its own camera,
 * so panning/zooming the world never affects UI layout later.
 */
public class GridPrototypeScreen extends BaseScreen {

    public static final int GRID_COLS = 20;
    public static final int GRID_ROWS = 12;
    public static final float CELL_SIZE = 64f;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;

    private final OrthographicCamera worldCamera;
    private final Viewport worldViewport;
    private final ShapeRenderer shapeRenderer;
    private final VillageGrid grid;
    private GridInputController inputController;
    private BuildingPlacementController placementController;


    public GridPrototypeScreen(Skin skin) {
        super(skin);

        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE, worldCamera);
        // Center camera on the grid so it's fully visible on start.
        worldCamera.position.set(GRID_COLS * CELL_SIZE / 2f, GRID_ROWS * CELL_SIZE / 2f, 0);

        shapeRenderer = new ShapeRenderer();
        grid = new VillageGrid(GRID_COLS, GRID_ROWS, CELL_SIZE);
        placementController = new BuildingPlacementController(grid);
        placementController.selectType(com.packs.mosaic.world.BuildingCatalog.get("small_house")); // temp: Phase 3 toolbar will replace this
    }

    // GridPrototypeScreen — changes only

    @Override
    protected void buildUi() {
        inputController = new GridInputController(worldCamera, worldViewport, grid, MIN_ZOOM, MAX_ZOOM, placementController);

        com.packs.mosaic.components.BuildingSelectionMenu menu =
            new com.packs.mosaic.components.BuildingSelectionMenu(skin, placementController);
        menu.attachTo(stage);

        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(stage);          // UI first — buttons consume their own touches
        multiplexer.addProcessor(inputController); // grid gets whatever the UI didn't want
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        com.badlogic.gdx.utils.ScreenUtils.clear(0.55f, 0.78f, 0.45f, 1f);
        inputController.update(delta);
        worldCamera.update();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);

        drawGridLines();
        drawPlacedObjects();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        placementController.drawPreview(shapeRenderer);
        shapeRenderer.end();

        stage.act(delta);
        stage.draw();
    }

    private void drawGridLines() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
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
    private void drawPlacedObjects() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int col = 0; col < GRID_COLS; col++) {
            for (int row = 0; row < GRID_ROWS; row++) {
                VillageGrid.GridOccupant occupant = grid.getOccupant(col, row);
                if (occupant instanceof com.packs.mosaic.world.BuildingObject) {
                    com.packs.mosaic.world.BuildingObject obj = (com.packs.mosaic.world.BuildingObject) occupant;
                    if (obj.getOriginCol() == col && obj.getOriginRow() == row) {
                        shapeRenderer.setColor(obj.getType().getPlaceholderColor());
                        float x = grid.cellToWorldX(col);
                        float y = grid.cellToWorldY(row);
                        float w = obj.getType().getWidthCells() * grid.getCellSize();
                        float h = obj.getType().getHeightCells() * grid.getCellSize();
                        shapeRenderer.rect(x, y, w, h);
                    }
                }
            }
        }
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height, false);
    }

    @Override
    protected void disposeScreen() {
        shapeRenderer.dispose();
        grid.dispose();
    }
}
