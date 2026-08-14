package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** Small code-native campus icon used in the build picker. */
final class CampusZoneIcon extends Actor {
    private static final ShapeRenderer RENDERER = new ShapeRenderer();
    private final String typeId;
    private final Vector2 stagePosition = new Vector2();

    CampusZoneIcon(String typeId) {
        this.typeId = typeId;
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

        RENDERER.setColor(new Color(0.86f, 0.95f, 0.92f, 1f));
        RENDERER.circle(x + w / 2f, y + h / 2f, Math.min(w, h) * 0.46f);

        if (typeId.equals("school")) {
            drawSchool(x, y, w, h);
        } else if (typeId.equals("shop")) {
            drawShop(x, y, w, h);
        } else if (typeId.equals("bench")) {
            drawCourtyard(x, y, w, h);
        } else if (typeId.equals("tree") || typeId.equals("bush")) {
            drawTree(x, y, w, h);
        } else if (typeId.equals("small_house") || typeId.equals("large_house")) {
            drawHouse(x, y, w, h, typeId.equals("large_house"));
        } else if (typeId.equals("playground")) {
            drawPlayground(x, y, w, h);
        } else if (typeId.equals("flower")) {
            drawFlower(x, y, w, h);
        } else if (typeId.equals("rock")) {
            drawRock(x, y, w, h);
        } else if (typeId.equals("street_lamp")) {
            drawLamp(x, y, w, h);
        } else if (typeId.startsWith("road_")) {
            drawRoad(x, y, w, h, typeId);
        } else if (typeId.equals("bridge_small")) {
            drawBridge(x, y, w, h);
        } else if (typeId.equals("garden") || typeId.equals("flower_palace") || typeId.equals("royal_garden")) {
            drawFlower(x, y, w, h);
        } else if (typeId.equals("crystal") || typeId.equals("crystal_quarry")) {
            drawCrystal(x, y, w, h);
        } else if (typeId.equals("fence")) {
            drawFence(x, y, w, h);
        } else if (typeId.equals("tower") || typeId.equals("wizard_tower") || typeId.equals("megalopolis_tower")
            || typeId.equals("obsidian_tower")) {
            drawTower(x, y, w, h);
        } else if (typeId.equals("magic_gate")) {
            drawMagicGate(x, y, w, h);
        } else if (typeId.equals("pine")) {
            drawPine(x, y, w, h);
        } else if (typeId.equals("mushroom")) {
            drawMushroom(x, y, w, h);
        } else if (typeId.equals("palm") || typeId.equals("coconut_palm")) {
            drawPalm(x, y, w, h);
        } else if (typeId.equals("beach_umbrella")) {
            drawUmbrella(x, y, w, h);
        } else if (typeId.equals("snowman")) {
            drawSnowman(x, y, w, h);
        } else if (typeId.equals("igloo")) {
            drawIgloo(x, y, w, h);
        } else if (typeId.equals("magma_spring")) {
            drawMagma(x, y, w, h);
        } else if (typeId.equals("coral_spring")) {
            drawCoral(x, y, w, h);
        } else if (typeId.equals("cabin") || typeId.equals("alpine_hut") || typeId.equals("ski_lodge")
            || typeId.equals("tiki_hut") || typeId.equals("lifeguard_hut") || typeId.equals("mountain_lookout")) {
            drawHouse(x, y, w, h, false);
        } else if (typeId.equals("summit_rock") || typeId.equals("lava_rock")) {
            drawRock(x, y, w, h);
        } else {
            RENDERER.setColor(new Color(0.25f, 0.55f, 0.62f, 1f));
            RENDERER.circle(x + w / 2f, y + h / 2f, w * 0.18f);
        }

        RENDERER.end();
        batch.begin();
    }

    private void drawSchool(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.36f, 0.43f, 0.90f, 1f));
        RENDERER.triangle(x + w * .22f, y + h * .55f, x + w * .5f, y + h * .82f, x + w * .78f, y + h * .55f);
        RENDERER.rect(x + w * .28f, y + h * .28f, w * .44f, h * .3f);
        RENDERER.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) RENDERER.rect(x + w * (.35f + i * .12f), y + h * .32f, w * .06f, h * .18f);
    }

    private void drawShop(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(1f, 0.67f, 0.28f, 1f));
        RENDERER.rect(x + w * .22f, y + h * .52f, w * .56f, h * .12f);
        RENDERER.setColor(new Color(1f, 0.39f, 0.34f, 1f));
        RENDERER.rect(x + w * .26f, y + h * .3f, w * .48f, h * .23f);
        RENDERER.setColor(Color.WHITE);
        RENDERER.rect(x + w * .43f, y + h * .3f, w * .14f, h * .16f);
    }

    private void drawCourtyard(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.20f, 0.37f, 0.40f, 1f));
        RENDERER.rect(x + w * .25f, y + h * .35f, w * .5f, h * .1f);
        RENDERER.rect(x + w * .3f, y + h * .24f, w * .07f, h * .12f);
        RENDERER.rect(x + w * .63f, y + h * .24f, w * .07f, h * .12f);
        drawTree(x, y, w, h);
    }

    private void drawTree(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.28f, 0.64f, 0.36f, 1f));
        RENDERER.circle(x + w * .68f, y + h * .68f, w * .18f);
        RENDERER.circle(x + w * .5f, y + h * .72f, w * .2f);
        RENDERER.circle(x + w * .34f, y + h * .67f, w * .16f);
        RENDERER.setColor(new Color(0.38f, 0.25f, 0.15f, 1f));
        RENDERER.rect(x + w * .46f, y + h * .32f, w * .08f, h * .32f);
    }

    private void drawHouse(float x, float y, float w, float h, boolean large) {
        RENDERER.setColor(new Color(0.96f, 0.45f, 0.36f, 1f));
        RENDERER.triangle(x + w * .2f, y + h * .52f, x + w * .5f, y + h * .82f, x + w * .8f, y + h * .52f);
        RENDERER.setColor(new Color(0.98f, 0.72f, 0.48f, 1f));
        RENDERER.rect(x + w * .28f, y + h * .25f, w * .44f, h * .3f);
        RENDERER.setColor(new Color(0.25f, 0.55f, 0.62f, 1f));
        RENDERER.rect(x + w * .44f, y + h * .25f, w * .12f, h * .18f);
        if (large) RENDERER.rect(x + w * .3f, y + h * .34f, w * .1f, h * .1f);
    }

    private void drawPlayground(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.98f, 0.45f, 0.48f, 1f));
        RENDERER.rect(x + w * .25f, y + h * .28f, w * .08f, h * .42f);
        RENDERER.rect(x + w * .67f, y + h * .28f, w * .08f, h * .42f);
        RENDERER.setColor(new Color(0.36f, 0.43f, 0.90f, 1f));
        RENDERER.rect(x + w * .25f, y + h * .64f, w * .5f, h * .07f);
        RENDERER.triangle(x + w * .3f, y + h * .64f, x + w * .5f, y + h * .32f, x + w * .7f, y + h * .64f);
        RENDERER.setColor(new Color(1f, 0.79f, 0.3f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .2f, w * .1f);
    }

    private void drawFlower(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.29f, 0.68f, 0.38f, 1f));
        RENDERER.rect(x + w * .48f, y + h * .22f, w * .05f, h * .42f);
        RENDERER.setColor(new Color(1f, 0.42f, 0.68f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .68f, w * .15f);
        RENDERER.circle(x + w * .36f, y + h * .62f, w * .12f);
        RENDERER.circle(x + w * .64f, y + h * .62f, w * .12f);
        RENDERER.setColor(new Color(1f, 0.82f, 0.28f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .63f, w * .06f);
    }

    private void drawRock(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.42f, 0.48f, 0.52f, 1f));
        RENDERER.triangle(x + w * .25f, y + h * .3f, x + w * .4f, y + h * .68f, x + w * .78f, y + h * .3f);
        RENDERER.setColor(new Color(0.62f, 0.68f, 0.7f, 1f));
        RENDERER.circle(x + w * .45f, y + h * .5f, w * .08f);
    }

    private void drawLamp(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.18f, 0.24f, 0.3f, 1f));
        RENDERER.rect(x + w * .47f, y + h * .2f, w * .07f, h * .5f);
        RENDERER.rect(x + w * .28f, y + h * .18f, w * .44f, h * .07f);
        RENDERER.setColor(new Color(1f, 0.8f, 0.3f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .72f, w * .14f);
    }

    private void drawRoad(float x, float y, float w, float h, String roadType) {
        RENDERER.setColor(new Color(0.25f, 0.3f, 0.34f, 1f));
        if (roadType.equals("road_corner")) {
            RENDERER.rect(x + w * .32f, y + h * .2f, w * .28f, h * .6f);
            RENDERER.rect(x + w * .32f, y + h * .2f, w * .48f, h * .28f);
        } else if (roadType.equals("road_cross")) {
            RENDERER.rect(x + w * .38f, y + h * .18f, w * .24f, h * .64f);
            RENDERER.rect(x + w * .18f, y + h * .38f, w * .64f, h * .24f);
        } else {
            RENDERER.rect(x + w * .32f, y + h * .16f, w * .36f, h * .68f);
        }
        RENDERER.setColor(new Color(1f, 0.82f, 0.35f, 1f));
        RENDERER.rect(x + w * .48f, y + h * .25f, w * .04f, h * .12f);
        RENDERER.rect(x + w * .48f, y + h * .63f, w * .04f, h * .12f);
    }

    private void drawBridge(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.25f, 0.55f, 0.72f, 1f));
        RENDERER.rect(x + w * .18f, y + h * .3f, w * .64f, h * .4f);
        RENDERER.setColor(new Color(0.62f, 0.4f, 0.2f, 1f));
        for (int i = 0; i < 4; i++) RENDERER.rect(x + w * (.23f + i * .17f), y + h * .32f, w * .1f, h * .36f);
        RENDERER.setColor(new Color(0.93f, 0.74f, 0.34f, 1f));
        RENDERER.rect(x + w * .18f, y + h * .67f, w * .64f, h * .06f);
    }

    private void drawCrystal(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.5f, 0.8f, 0.95f, 1f));
        RENDERER.triangle(x + w * .5f, y + h * .22f, x + w * .28f, y + h * .68f, x + w * .5f, y + h * .82f);
        RENDERER.triangle(x + w * .5f, y + h * .22f, x + w * .72f, y + h * .68f, x + w * .5f, y + h * .82f);
        RENDERER.setColor(new Color(0.85f, 0.95f, 1f, 1f));
        RENDERER.triangle(x + w * .5f, y + h * .22f, x + w * .5f, y + h * .82f, x + w * .4f, y + h * .62f);
    }

    private void drawFence(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.72f, 0.6f, 0.4f, 1f));
        for (int i = 0; i < 4; i++) RENDERER.rect(x + w * (.14f + i * .24f), y + h * .16f, w * .1f, h * .68f);
        RENDERER.setColor(new Color(0.85f, 0.75f, 0.55f, 1f));
        RENDERER.rect(x + w * .08f, y + h * .62f, w * .84f, h * .1f);
        RENDERER.rect(x + w * .08f, y + h * .34f, w * .84f, h * .1f);
    }

    private void drawTower(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.55f, 0.45f, 0.8f, 1f));
        RENDERER.rect(x + w * .3f, y + h * .2f, w * .4f, h * .58f);
        RENDERER.setColor(new Color(0.35f, 0.28f, 0.55f, 1f));
        RENDERER.triangle(x + w * .22f, y + h * .78f, x + w * .5f, y + h * .94f, x + w * .78f, y + h * .78f);
        RENDERER.setColor(new Color(1f, 0.82f, 0.35f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .66f, w * .07f);
    }

    private void drawMagicGate(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.35f, 0.3f, 0.65f, 1f));
        RENDERER.rect(x + w * .2f, y + h * .18f, w * .6f, h * .62f);
        RENDERER.setColor(new Color(0.1f, 0.1f, 0.2f, 1f));
        RENDERER.rect(x + w * .3f, y + h * .18f, w * .4f, h * .42f);
        RENDERER.circle(x + w * .5f, y + h * .62f, w * .2f);
        RENDERER.setColor(new Color(1f, 0.82f, 0.35f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .62f, w * .09f);
    }

    private void drawPine(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.40f, 0.28f, 0.16f, 1f));
        RENDERER.rect(x + w * .46f, y + h * .2f, w * .08f, h * .24f);
        RENDERER.setColor(new Color(0.20f, 0.52f, 0.30f, 1f));
        RENDERER.triangle(x + w * .22f, y + h * .44f, x + w * .5f, y + h * .8f, x + w * .78f, y + h * .44f);
        RENDERER.triangle(x + w * .3f, y + h * .58f, x + w * .5f, y + h * .9f, x + w * .7f, y + h * .58f);
    }

    private void drawMushroom(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.95f, 0.9f, 0.75f, 1f));
        RENDERER.rect(x + w * .46f, y + h * .2f, w * .08f, h * .34f);
        RENDERER.setColor(new Color(0.90f, 0.35f, 0.3f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .62f, w * .24f);
        RENDERER.setColor(new Color(0.98f, 0.95f, 0.9f, 1f));
        RENDERER.circle(x + w * .38f, y + h * .7f, w * .05f);
        RENDERER.circle(x + w * .56f, y + h * .74f, w * .05f);
    }

    private void drawPalm(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.45f, 0.30f, 0.16f, 1f));
        RENDERER.rect(x + w * .46f, y + h * .2f, w * .08f, h * .44f);
        RENDERER.setColor(new Color(0.30f, 0.62f, 0.30f, 1f));
        RENDERER.triangle(x + w * .48f, y + h * .64f, x + w * .16f, y + h * .5f, x + w * .4f, y + h * .6f);
        RENDERER.triangle(x + w * .52f, y + h * .64f, x + w * .84f, y + h * .5f, x + w * .6f, y + h * .6f);
        RENDERER.triangle(x + w * .52f, y + h * .66f, x + w * .6f, y + h * .9f, x + w * .36f, y + h * .78f);
    }

    private void drawUmbrella(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.9f, 0.85f, 0.7f, 1f));
        RENDERER.rect(x + w * .48f, y + h * .2f, w * .05f, h * .3f);
        RENDERER.setColor(new Color(0.95f, 0.35f, 0.35f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .56f, w * .26f);
        RENDERER.setColor(new Color(0.98f, 0.92f, 0.85f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .56f, w * .08f);
    }

    private void drawSnowman(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .3f, w * .22f);
        RENDERER.circle(x + w * .5f, y + h * .56f, w * .16f);
        RENDERER.setColor(new Color(0.25f, 0.3f, 0.4f, 1f));
        RENDERER.rect(x + w * .42f, y + h * .72f, w * .16f, h * .08f);
    }

    private void drawIgloo(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .36f, w * .3f);
        RENDERER.setColor(new Color(0.30f, 0.35f, 0.45f, 1f));
        RENDERER.circle(x + w * .4f, y + h * .3f, w * .12f);
        RENDERER.circle(x + w * .38f, y + h * .3f, w * .1f);
    }

    private void drawMagma(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.55f, 0.3f, 0.2f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .42f, w * .3f);
        RENDERER.setColor(new Color(0.95f, 0.5f, 0.2f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .42f, w * .2f);
        RENDERER.setColor(new Color(1f, 0.8f, 0.4f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .42f, w * .1f);
    }

    private void drawCoral(float x, float y, float w, float h) {
        RENDERER.setColor(new Color(0.35f, 0.75f, 0.8f, 1f));
        RENDERER.circle(x + w * .5f, y + h * .4f, w * .28f);
        RENDERER.setColor(new Color(0.95f, 0.3f, 0.4f, 1f));
        RENDERER.circle(x + w * .38f, y + h * .44f, w * .07f);
        RENDERER.circle(x + w * .62f, y + h * .46f, w * .09f);
        RENDERER.circle(x + w * .5f, y + h * .3f, w * .07f);
    }
}
