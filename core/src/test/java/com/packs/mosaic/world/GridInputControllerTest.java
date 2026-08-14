package com.packs.mosaic.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.packs.mosaic.support.HeadlessGdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Camera pan/zoom clamping and Android-touch vs desktop-mouse parity.
 *
 * The real screen uses a libGDX ExtendViewport whose apply() calls
 * camera.update(), which invokes native Matrix4 math that cannot run
 * headless. Camera.update() is the game loop's job, not the
 * controller's — so a plain Viewport carrying the same world/screen
 * dimensions exercises everything GridInputController reads without
 * touching the camera matrix. unproject() stays deterministic because
 * invProjectionView is the camera's identity matrix, which keeps the
 * mouse/touch paths consistent with each other.
 */
@DisplayName("GridInputController — camera clamp and input parity")
class GridInputControllerTest {

    private static final int COLS = 20;
    private static final int ROWS = 12;
    private static final float CELL = 64f;
    private static final int SCREEN_W = 1280;
    private static final int SCREEN_H = 768;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2f;

    private VillageGrid grid;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BuildingPlacementController placementController;
    private GridInputController controller;

    @BeforeEach
    void setUp() {
        HeadlessGdx.install(SCREEN_W, SCREEN_H);
        grid = new VillageGrid(COLS, ROWS, CELL);
        camera = new OrthographicCamera();
        camera.viewportWidth = COLS * CELL;
        camera.viewportHeight = ROWS * CELL;
        viewport = new Viewport() {
            @Override
            public void update(int screenWidth, int screenHeight, boolean centerCamera) {
                // Intentionally a no-op: apply() would call camera.update() (native math).
                // The game loop owns camera.update(); this controller never calls it.
            }
        };
        viewport.setCamera(camera);
        viewport.setWorldSize(COLS * CELL, ROWS * CELL);
        viewport.setScreenBounds(0, 0, SCREEN_W, SCREEN_H);
        placementController = new BuildingPlacementController(grid);
        controller = new GridInputController(camera, viewport, grid, MIN_ZOOM, MAX_ZOOM, placementController);
        camera.position.set(COLS * CELL / 2f, ROWS * CELL / 2f, 0);
        camera.zoom = 1f;
    }

    /** The cell GridInputController resolves for a screen point (same math mouse and touch share). */
    private int[] cellOfScreen(int screenX, int screenY) {
        Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
        return new int[]{grid.worldToCol(world.x), grid.worldToRow(world.y)};
    }

    private int countOccupied() {
        int count = 0;
        for (int c = 0; c < grid.getCols(); c++) {
            for (int r = 0; r < grid.getRows(); r++) {
                if (!grid.isCellFree(c, r)) count++;
            }
        }
        return count;
    }

    @Test
    void scrolledClampsZoomAtMinimum() {
        controller.scrolled(0, -100);
        assertEquals(MIN_ZOOM, camera.zoom, 1e-6f);
    }

    @Test
    void scrolledClampsZoomAtMaximum() {
        controller.scrolled(0, 100);
        assertEquals(MAX_ZOOM, camera.zoom, 1e-6f);
    }

    @Test
    void scrolledStepsZoomByATenth() {
        controller.scrolled(0, 1);
        assertEquals(1.1f, camera.zoom, 1e-6f);
        controller.scrolled(0, -1);
        assertEquals(1f, camera.zoom, 1e-6f);
    }

    @Test
    void updateClampsCameraToGridAtZoomOne() {
        camera.position.set(-1000, -1000, 0);
        controller.update(1f / 60f);
        assertEquals(640f, camera.position.x, 1e-4f);
        assertEquals(384f, camera.position.y, 1e-4f);

        camera.position.set(10000, 10000, 0);
        controller.update(1f / 60f);
        assertEquals(640f, camera.position.x, 1e-4f);
        assertEquals(384f, camera.position.y, 1e-4f);
    }

    @Test
    void updateClampsCameraWithinZoomedOutRange() {
        camera.zoom = MIN_ZOOM;
        camera.position.set(0, 0, 0);
        controller.update(1f / 60f);
        assertEquals(320f, camera.position.x, 1e-4f);
        assertEquals(192f, camera.position.y, 1e-4f);

        camera.position.set(10000, 10000, 0);
        controller.update(1f / 60f);
        assertEquals(960f, camera.position.x, 1e-4f);
        assertEquals(576f, camera.position.y, 1e-4f);
    }

    @Test
    void updateKeepsCameraInsideRangeWhenViewLargerThanGrid() {
        camera.zoom = MAX_ZOOM;
        camera.position.set(-50, -50, 0);
        controller.update(1f / 60f);
        assertEquals(0f, camera.position.x, 1e-4f);
        assertEquals(0f, camera.position.y, 1e-4f);

        camera.position.set(2000, 2000, 0);
        controller.update(1f / 60f);
        assertEquals(1280f, camera.position.x, 1e-4f);
        assertEquals(768f, camera.position.y, 1e-4f);
    }

    @Test
    void updateDoesNotDriftCameraAlreadyInsideBounds() {
        controller.update(1f / 60f);
        assertEquals(640f, camera.position.x, 1e-4f);
        assertEquals(384f, camera.position.y, 1e-4f);
    }

    @Test
    void setZoomClampsZoomAndImmediatelyClampsPosition() {
        camera.position.set(-1000, -1000, 0);
        controller.setZoom(100f);
        assertEquals(MAX_ZOOM, camera.zoom, 1e-6f);
        assertEquals(0f, camera.position.x, 1e-4f, "view bigger than grid clamps to the low edge");
        assertEquals(0f, camera.position.y, 1e-4f);

        camera.position.set(10000, 10000, 0);
        controller.setZoom(-100f);
        assertEquals(MIN_ZOOM, camera.zoom, 1e-6f);
        assertEquals(960f, camera.position.x, 1e-4f, "zoomed-in view clamps to the high edge");
        assertEquals(576f, camera.position.y, 1e-4f);
    }

    @Test
    void setZoomKeepsCameraInsideGridWhenZoomingIn() {
        camera.zoom = MAX_ZOOM;
        camera.position.set(0, 0, 0);
        controller.setZoom(MIN_ZOOM);
        assertEquals(MIN_ZOOM, camera.zoom, 1e-6f);
        assertEquals(320f, camera.position.x, 1e-4f, "zooming in shrinks the allowed range, so the corner must re-clamp");
        assertEquals(192f, camera.position.y, 1e-4f);
    }

    @Test
    void mouseHoverAndTouchTapResolveTheSameCell() {
        placementController.selectType(BuildingCatalog.get("tree"));
        int sx = SCREEN_W / 2;
        int sy = SCREEN_H / 2;

        controller.mouseMoved(sx, sy);
        int hoverCol = placementController.getHoverCol();
        int hoverRow = placementController.getHoverRow();

        controller.touchDown(sx, sy, 0, Input.Buttons.LEFT);
        controller.touchUp(sx, sy, 0, Input.Buttons.LEFT);

        BuildingObject placed = (BuildingObject) grid.getOccupant(hoverCol, hoverRow);
        assertTrue(placed != null && "tree".equals(placed.getType().getId()),
            "a desktop click must place exactly where the mouse hover showed");
    }

    @Test
    void touchTapPlacesAtTheSameCellTheUnprojectionComputes() {
        placementController.selectType(BuildingCatalog.get("tree"));
        int sx = SCREEN_W / 2;
        int sy = SCREEN_H / 2;
        int[] expected = cellOfScreen(sx, sy);

        controller.touchDown(sx, sy, 0, Input.Buttons.LEFT);
        controller.touchUp(sx, sy, 0, Input.Buttons.LEFT);

        assertEquals(-1, placementController.getHoverCol(),
            "touch events must not depend on a preceding mouseMoved (Android has none)");
        assertEquals(1, countOccupied(), "a plain Android tap must still place");
        assertTrue(!grid.isCellFree(expected[0], expected[1]),
            "the building must land on the unprojected cell");
    }

    @Test
    void dragBeyondThresholdPansInsteadOfPlacing() {
        placementController.selectType(BuildingCatalog.get("tree"));
        int startX = SCREEN_W / 2;
        int startY = SCREEN_H / 2;

        controller.touchDown(startX, startY, 0, Input.Buttons.LEFT);
        controller.touchDragged(startX + 60, startY, 0);
        controller.touchUp(startX + 60, startY, 0, Input.Buttons.LEFT);

        assertEquals(0, countOccupied(), "a pan must not place a building");
        assertNotEquals(640f, camera.position.x, 1e-3f, "the camera must have panned");
    }

    @Test
    void rightClickAndDeleteModeRemoveIdentically() {
        int sx = SCREEN_W / 2;
        int sy = SCREEN_H / 2;
        int[] cell = cellOfScreen(sx, sy);

        placementController.selectType(BuildingCatalog.get("tree"));
        placementController.tryPlace(cell[0], cell[1]);

        controller.touchDown(sx, sy, 0, Input.Buttons.RIGHT);
        controller.touchUp(sx, sy, 0, Input.Buttons.RIGHT);
        assertTrue(grid.isCellFree(cell[0], cell[1]), "desktop right-click must delete");

        placementController.tryPlace(cell[0], cell[1]);
        placementController.setDeleteMode(true);
        controller.touchDown(sx, sy, 0, Input.Buttons.LEFT);
        controller.touchUp(sx, sy, 0, Input.Buttons.LEFT);
        assertTrue(grid.isCellFree(cell[0], cell[1]), "Android delete-mode tap must delete the same way");
    }
}
