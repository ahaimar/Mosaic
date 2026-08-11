package com.packs.mosaic.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Handles camera panning (drag) and zooming (scroll wheel) for the
 * world camera, clamped to the grid's bounds. Also converts screen
 * touches to grid cell coordinates so future placement logic can
 * subscribe via a callback (see onCellTapped stub).
 */
public class GridInputController extends InputAdapter {

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final VillageGrid grid;
    private final float minZoom;
    private final float maxZoom;

    private final Vector3 lastTouch = new Vector3();
    private boolean dragging = false;

    private final BuildingPlacementController placementController;

    public GridInputController(OrthographicCamera camera, Viewport viewport, VillageGrid grid,
                               float minZoom, float maxZoom,
                               BuildingPlacementController placementController) {
        this.camera = camera;
        this.viewport = viewport;
        this.grid = grid;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.placementController = placementController;
    }

    /** Call once per frame from the screen's render loop, after camera.update(). */
    public void update(float delta) {
        clampCameraToGrid();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        placementController.updateHover(screenXToCol(screenX, screenY), screenYToRow(screenX, screenY));
        return false;
    }


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            lastTouch.set(screenX, screenY, 0);
            dragging = true;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        placementController.updateHover(screenXToCol(screenX, screenY), screenYToRow(screenX, screenY));
        if (!dragging) return false;

        Vector3 current = new Vector3(screenX, screenY, 0);
        Vector3 worldCurrent = viewport.unproject(current.cpy());
        Vector3 worldLast = viewport.unproject(lastTouch.cpy());

        float deltaX = worldLast.x - worldCurrent.x;
        float deltaY = worldLast.y - worldCurrent.y;

        camera.position.add(deltaX, deltaY, 0);
        lastTouch.set(screenX, screenY, 0);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        int col = screenXToCol(screenX, screenY);
        int row = screenYToRow(screenX, screenY);
        if (button == Input.Buttons.LEFT) {
            dragging = false;
            placementController.tryPlace(col, row);
        } else if (button == Input.Buttons.RIGHT) {
            placementController.tryDelete(col, row);
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.R) {
            placementController.rotateSelection();
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float newZoom = camera.zoom + amountY * 0.1f;
        camera.zoom = MathUtils.clamp(newZoom, minZoom, maxZoom);
        return true;
    }

    /** Stub — Phase 1's BuildingManager will override/replace this via a listener once it exists. */
    protected void onCellTapped(int col, int row) {
        // Intentionally empty for now.
    }

    private int screenXToCol(int screenX, int screenY) {
        Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
        return grid.worldToCol(world.x);
    }

    private int screenYToRow(int screenX, int screenY) {
        Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
        return grid.worldToRow(world.y);
    }

    private void clampCameraToGrid() {
        float gridWidth = grid.getCols() * grid.getCellSize();
        float gridHeight = grid.getRows() * grid.getCellSize();

        float effectiveViewWidth = viewport.getWorldWidth() * camera.zoom;
        float effectiveViewHeight = viewport.getWorldHeight() * camera.zoom;

        float halfW = effectiveViewWidth / 2f;
        float halfH = effectiveViewHeight / 2f;

        // If the zoomed-out view is bigger than the grid, center instead of clamping into an inverted range.
        float minX = Math.min(halfW, gridWidth - halfW);
        float maxX = Math.max(halfW, gridWidth - halfW);
        float minY = Math.min(halfH, gridHeight - halfH);
        float maxY = Math.max(halfH, gridHeight - halfH);

        camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
        camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
    }
}
