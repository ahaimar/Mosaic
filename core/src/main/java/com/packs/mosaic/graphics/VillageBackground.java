package com.packs.mosaic.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;

/**
 * A cheerful procedural village skyline rendered once into a Texture and
 * drawn full-screen behind the main menu. Purely code-drawn: gradient
 * sky, sun, clouds, rolling hills, a row of little houses and trees —
 * so the menu looks alive without any image assets.
 */
public class VillageBackground extends Actor implements Disposable {

    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;

    private final Texture texture;

    public VillageBackground() {
        Pixmap px = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        drawSky(px);
        drawSun(px);
        drawClouds(px);
        drawHills(px);
        drawVillage(px);
        texture = new Texture(px);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        px.dispose();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public void dispose() {
        texture.dispose();
    }

    private static void drawSky(Pixmap px) {
        for (int y = 0; y < HEIGHT; y++) {
            float t = (float) y / HEIGHT;
            float r = 0.42f + (0.72f - 0.42f) * t;
            float g = 0.66f + (0.88f - 0.66f) * t;
            float b = 0.90f + (0.98f - 0.90f) * t;
            px.setColor(r, g, b, 1f);
            px.drawLine(0, y, WIDTH, y);
        }
    }

    private static void drawSun(Pixmap px) {
        px.setColor(new Color(1f, 0.85f, 0.45f, 1f));
        px.fillCircle(520, 300, 34);
        px.setColor(new Color(1f, 0.94f, 0.70f, 0.6f));
        px.fillCircle(520, 300, 50);
    }

    private static void drawClouds(Pixmap px) {
        px.setColor(new Color(1f, 1f, 1f, 0.85f));
        cloud(px, 110, 280, 18);
        cloud(px, 330, 250, 14);
        cloud(px, 560, 220, 12);
    }

    private static void cloud(Pixmap px, int x, int y, int r) {
        px.fillCircle(x, y, r);
        px.fillCircle(x + r, y + r / 2, (int) (r * 0.8f));
        px.fillCircle(x - r, y + r / 2, (int) (r * 0.8f));
        px.fillCircle(x + r / 2, y - r / 2, (int) (r * 0.7f));
    }

    private static void drawHills(Pixmap px) {
        px.setColor(new Color(0.45f, 0.72f, 0.42f, 1f));
        px.fillCircle(120, -60, 150);
        px.fillCircle(420, -80, 190);
        px.fillCircle(660, -50, 150);
        px.setColor(new Color(0.55f, 0.80f, 0.48f, 1f));
        px.fillCircle(260, -30, 120);
        px.fillCircle(540, -60, 140);
    }

    private static void drawVillage(Pixmap px) {
        // grass strip
        px.setColor(new Color(0.42f, 0.72f, 0.38f, 1f));
        px.fillRectangle(0, 0, WIDTH, 110);

        Color houseBody = new Color(0.95f, 0.65f, 0.45f, 1f);
        Color houseRoof = new Color(0.75f, 0.35f, 0.28f, 1f);
        for (int i = 0; i < 6; i++) {
            int x = 30 + i * 104;
            drawHouse(px, x, 40, 56, 52, houseBody, houseRoof);
            drawTree(px, x + 62, 34);
        }
        drawTree(px, 30, 34);
        drawTree(px, 604, 34);
    }

    private static void drawHouse(Pixmap px, int x, int y, int w, int h,
                                  Color body, Color roof) {
        px.setColor(body);
        px.fillRectangle(x, y, w, h);
        px.setColor(roof);
        px.fillTriangle(x - 8, y + h, x + w / 2, y + h + 30, x + w + 8, y + h);
        px.setColor(new Color(0.35f, 0.25f, 0.15f, 1f));
        px.fillRectangle(x + w / 2 - 5, y, 10, 16);
        px.setColor(new Color(0.60f, 0.80f, 0.95f, 1f));
        px.fillRectangle(x + 8, y + 16, 12, 12);
        px.fillRectangle(x + w - 20, y + 16, 12, 12);
    }

    private static void drawTree(Pixmap px, int x, int y) {
        px.setColor(new Color(0.42f, 0.28f, 0.16f, 1f));
        px.fillRectangle(x, y, 6, 22);
        px.setColor(new Color(0.25f, 0.55f, 0.25f, 1f));
        px.fillCircle(x + 3, y + 24, 15);
        px.fillCircle(x - 7, y + 20, 10);
        px.fillCircle(x + 13, y + 20, 10);
    }
}
