package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.packs.mosaic.world.GameMap;

/**
 * Small code-native thumbnail of a world, used on the map-selection screen.
 * Draws the map's ground colour plus a tiny themed emblem (pine, palm,
 * mountain peaks, snowman, volcano…), so each environment is recognisable at
 * a glance without any image assets.
 */
public class MapPreviewIcon extends Actor {

    private static final ShapeRenderer RENDERER = new ShapeRenderer();
    private final GameMap map;
    private final Vector2 stagePosition = new Vector2();

    public MapPreviewIcon(GameMap map) {
        this.map = map;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        localToStageCoordinates(stagePosition.set(0f, 0f));
        batch.end();
        RENDERER.setProjectionMatrix(batch.getProjectionMatrix());
        RENDERER.begin(ShapeRenderer.ShapeType.Filled);

        float x = stagePosition.x;
        float y = stagePosition.y;
        float w = getWidth();
        float h = getHeight();

        RENDERER.setColor(map.getGroundColor());
        RENDERER.rect(x, y, w, h);

        RENDERER.setColor(new Color(1f, 1f, 1f, 0.22f));
        RENDERER.rect(x, y, w, 3f);

        switch (map.getId()) {
            case "meadow":   drawMeadow(x, y, w, h); break;
            case "forest":   drawForest(x, y, w, h); break;
            case "beach":    drawBeach(x, y, w, h); break;
            case "mountain": drawMountain(x, y, w, h); break;
            case "snowland": drawSnowland(x, y, w, h); break;
            case "volcano":  drawVolcano(x, y, w, h); break;
            case "island":   drawIsland(x, y, w, h); break;
            default:         break;
        }

        RENDERER.end();
        batch.begin();
    }

    private void drawMeadow(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.30f, 0.55f, 0.28f, 1f));
        RENDERER.circle(x + w * .3f, y + h * .7f, w * .14f);
        RENDERER.setColor(new Color(0.96f, 0.45f, 0.36f, 1f));
        RENDERER.triangle(x + w * .6f, y + h * .5f, x + w * .78f, y + h * .75f, x + w * .96f, y + h * .5f);
        RENDERER.setColor(new Color(0.98f, 0.72f, 0.48f, 1f));
        RENDERER.rect(x + w * .62f, y + h * .3f, w * .32f, h * .22f);
    }

    private void drawForest(float x, float y, float w, float h) {
        pine(x + w * .26f, y + h * .34f, w * .26f, new Color(0.16f, 0.42f, 0.24f, 1f));
        pine(x + w * .52f, y + h * .3f, w * .32f, new Color(0.20f, 0.50f, 0.28f, 1f));
        pine(x + w * .78f, y + h * .36f, w * .22f, new Color(0.16f, 0.42f, 0.24f, 1f));
    }

    private void drawBeach(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.85f, 0.76f, 0.52f, 1f));
        RENDERER.rect(x, y + h * .12f, w, h * .22f);
        RENDERER.setColor(new Color(1f, 0.82f, 0.35f, 1f));
        RENDERER.circle(x + w * .85f, y + h * .8f, w * .1f);
        palm(x + w * .3f, y + h * .22f, w * .4f);
    }

    private void drawMountain(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.55f, 0.58f, 0.62f, 1f));
        RENDERER.triangle(x + w * .2f, y + h * .22f, x + w * .5f, y + h * .8f, x + w * .8f, y + h * .22f);
        RENDERER.setColor(new Color(0.72f, 0.74f, 0.78f, 1f));
        RENDERER.triangle(x + w * .5f, y + h * .46f, x + w * .58f, y + h * .62f, x + w * .68f, y + h * .46f);
        RENDERER.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        RENDERER.triangle(x + w * .42f, y + h * .8f, x + w * .5f, y + h * .62f, x + w * .58f, y + h * .8f);
        RENDERER.setColor(new Color(0.30f, 0.32f, 0.36f, 1f));
        RENDERER.triangle(x + w * .1f, y + h * .24f, x + w * .34f, y + h * .24f, x + w * .22f, y + h * .42f);
    }

    private void drawSnowland(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        RENDERER.circle(x + w * .35f, y + h * .36f, w * .18f);
        RENDERER.circle(x + w * .35f, y + h * .58f, w * .14f);
        RENDERER.setColor(new Color(0.2f, 0.25f, 0.35f, 1f));
        RENDERER.rect(x + w * .32f, y + h * .68f, w * .06f, h * .1f);
        RENDERER.circle(x + w * .78f, y + h * .7f, w * .08f);
        RENDERER.circle(x + w * .7f, y + h * .66f, w * .06f);
    }

    private void drawVolcano(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.30f, 0.24f, 0.22f, 1f));
        RENDERER.triangle(x + w * .16f, y + h * .22f, x + w * .5f, y + h * .78f, x + w * .84f, y + h * .22f);
        RENDERER.setColor(new Color(0.95f, 0.45f, 0.18f, 1f));
        RENDERER.triangle(x + w * .4f, y + h * .66f, x + w * .5f, y + h * .82f, x + w * .6f, y + h * .66f);
        RENDERER.setColor(new Color(0.85f, 0.40f, 0.18f, 0.8f));
        RENDERER.circle(x + w * .5f, y + h * .4f, w * .05f);
        RENDERER.circle(x + w * .62f, y + h * .3f, w * .04f);
    }

    private void drawIsland(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.35f, 0.68f, 0.80f, 1f));
        RENDERER.rect(x, y + h * .12f, w, h * .3f);
        RENDERER.setColor(new Color(0.62f, 0.72f, 0.48f, 1f));
        RENDERER.ellipse(x + w * .18f, y + h * .3f, w * .7f, h * .26f);
        palm(x + w * .4f, y + h * .34f, w * .36f);
    }

    private void pine(float x, float y, float w, Color color) {
        RENDERER.setColor(new Color(0.38f, 0.25f, 0.15f, 1f));
        RENDERER.rect(x, y, w * .1f, w * .3f);
        RENDERER.setColor(color);
        RENDERER.triangle(x - w * .25f, y + w * .3f, x + w * .05f, y + w * .7f, x + w * .35f, y + w * .3f);
        RENDERER.triangle(x - w * .2f, y + w * .45f, x + w * .05f, y + w * .85f, x + w * .3f, y + w * .45f);
    }

    private void palm(float x, float y, float w) {
        RENDERER.setColor(new Color(0.45f, 0.30f, 0.16f, 1f));
        RENDERER.rectLine(x + w * .1f, y, x + w * .12f, y + w * .5f, w * .08f);
        RENDERER.setColor(new Color(0.25f, 0.55f, 0.28f, 1f));
        RENDERER.triangle(x + w * .08f, y + w * .5f, x - w * .18f, y + w * .3f, x + w * .02f, y + w * .44f);
        RENDERER.triangle(x + w * .14f, y + w * .5f, x + w * .36f, y + w * .34f, x + w * .16f, y + w * .46f);
        RENDERER.triangle(x + w * .16f, y + w * .52f, x + w * .2f, y + w * .78f, x + w * .02f, y + w * .62f);
    }
}
