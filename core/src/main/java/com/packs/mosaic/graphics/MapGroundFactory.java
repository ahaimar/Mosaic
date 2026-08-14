package com.packs.mosaic.graphics;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.world.GameMap;

/**
 * Generates the per-world ground tile (Task 3). Each world gets a tiled
 * Pixmap of its own terrain: a flat base colour, subtle grain, and baked-in
 * decoration accents (grass tufts, shells, snow patches, lava cracks, lagoon
 * puddles…). The meadow keeps its original ground asset untouched — this
 * factory only produces textures for the new environments.
 */
public final class MapGroundFactory implements Disposable {

    /** Tile size in pixels; each generated texture is 2x2 tiles so accents vary per tile. */
    public static final int TILE = 64;
    private static final int TILES_PER_SIDE = 2;

    private final ObjectMap<String, Texture> textures = new ObjectMap<>();

    /** Returns (creating on first use) the tiled ground texture for a map. */
    public Texture get(GameMap map) {
        Texture texture = textures.get(map.getId());
        if (texture == null) {
            texture = new Texture(render(map));
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            textures.put(map.getId(), texture);
        }
        return texture;
    }

    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }

    private static Pixmap render(GameMap map) {
        int size = TILE * TILES_PER_SIDE;
        Pixmap px = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        px.setBlending(Pixmap.Blending.None);

        Color base = map.getGroundColor();
        Color accent = map.getAccentColor();
        px.setColor(base);
        px.fill();

        px.setBlending(Pixmap.Blending.SourceOver);
        Random random = new Random(map.getId().hashCode());
        switch (map.getTerrain()) {
            case GRASS:
                drawGrass(px, base, accent, random, size);
                break;
            case FOREST:
                drawForest(px, base, random, size);
                break;
            case SAND:
                drawSand(px, random, size);
                break;
            case ROCK:
                drawRock(px, random, size);
                break;
            case SNOW:
                drawSnow(px, accent, random, size);
                break;
            case VOLCANIC:
                drawVolcanic(px, accent, random, size);
                break;
            case TROPICAL:
                drawTropical(px, accent, random, size);
                break;
        }
        return px;
    }

    private static void drawGrass(Pixmap px, Color base, Color accent, Random random, int size) {
        int flecks = 160;
        for (int i = 0; i < flecks; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean() ? shade(base, -0.06f) : lighten(base, 0.08f));
            px.fillRectangle(x, y, 2, 2);
        }
        int tufts = 26;
        for (int i = 0; i < tufts; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(lighten(base, 0.12f));
            px.fillRectangle(x, y, 2, 8);
            px.fillRectangle(x + 3, y, 2, 6);
            px.setColor(lighten(accent, 0.05f));
            px.fillRectangle(x + 1, y + 7, 2, 2);
        }
    }

    private static void drawForest(Pixmap px, Color base, Random random, int size) {
        int patches = 40;
        for (int i = 0; i < patches; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(shade(base, -0.08f + random.nextFloat() * 0.12f));
            px.fillCircle(x, y, 6 + random.nextInt(8));
        }
        int leaves = 120;
        for (int i = 0; i < leaves; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean()
                ? new Color(0.42f, 0.58f, 0.30f, 1f)
                : new Color(0.55f, 0.45f, 0.22f, 1f));
            px.fillRectangle(x, y, 3, 2);
        }
    }

    private static void drawSand(Pixmap px, Random random, int size) {
        int flecks = 200;
        for (int i = 0; i < flecks; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean()
                ? new Color(0.93f, 0.87f, 0.68f, 1f)
                : new Color(0.78f, 0.68f, 0.45f, 1f));
            px.fillRectangle(x, y, 2, 2);
        }
        int shells = 8;
        for (int i = 0; i < shells; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.98f, 0.95f, 0.85f, 1f));
            px.fillTriangle(x, y, x + 6, y, x + 3, y + 5);
        }
        int ripples = 6;
        for (int i = 0; i < ripples; i++) {
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.80f, 0.70f, 0.48f, 0.7f));
            px.drawLine(6, y, 26, y);
            px.drawLine(34, y, 58, y);
        }
    }

    private static void drawRock(Pixmap px, Random random, int size) {
        int speckles = 260;
        for (int i = 0; i < speckles; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean()
                ? new Color(0.38f, 0.40f, 0.44f, 1f)
                : new Color(0.64f, 0.66f, 0.70f, 1f));
            px.fillRectangle(x, y, 2, 2);
        }
        int boulders = 5;
        for (int i = 0; i < boulders; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.42f, 0.44f, 0.48f, 1f));
            px.fillCircle(x + 4, y + 4, 5 + random.nextInt(4));
            px.setColor(new Color(0.58f, 0.60f, 0.64f, 1f));
            px.fillCircle(x + 3, y + 6, 3);
        }
        int cracks = 4;
        for (int i = 0; i < cracks; i++) {
            int x = random.nextInt(size - 20) + 4;
            int y = random.nextInt(size - 20) + 4;
            px.setColor(new Color(0.30f, 0.32f, 0.36f, 1f));
            px.drawLine(x, y, x + 8, y + 6);
            px.drawLine(x + 8, y + 6, x + 12, y + 4);
        }
    }

    private static void drawSnow(Pixmap px, Color accent, Random random, int size) {
        int patches = 36;
        for (int i = 0; i < patches; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean()
                ? new Color(0.82f, 0.87f, 0.95f, 1f)
                : new Color(0.95f, 0.97f, 1f, 1f));
            px.fillCircle(x, y, 5 + random.nextInt(8));
        }
        int ice = 10;
        for (int i = 0; i < ice; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(accent);
            px.fillTriangle(x, y + 6, x + 4, y, x + 8, y + 6);
            px.setColor(new Color(0.95f, 0.97f, 1f, 1f));
            px.fillTriangle(x + 2, y + 3, x + 4, y, x + 6, y + 3);
        }
        int sparkle = 40;
        for (int i = 0; i < sparkle; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(new Color(0.72f, 0.82f, 0.95f, 0.6f));
            px.fillRectangle(x, y, 2, 2);
        }
    }

    private static void drawVolcanic(Pixmap px, Color accent, Random random, int size) {
        int rocks = 60;
        for (int i = 0; i < rocks; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(random.nextBoolean()
                ? new Color(0.24f, 0.19f, 0.18f, 1f)
                : new Color(0.38f, 0.31f, 0.28f, 1f));
            px.fillCircle(x, y, 3 + random.nextInt(5));
        }
        int cracks = 10;
        for (int i = 0; i < cracks; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.15f, 0.10f, 0.10f, 1f));
            px.drawLine(x, y, x + 10, y + 4);
            px.drawLine(x + 10, y + 4, x + 16, y + 2);
            px.setColor(accent);
            px.fillRectangle(x + 3, y + 1, 4, 2);
        }
        int glow = 14;
        for (int i = 0; i < glow; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            px.setColor(new Color(0.85f, 0.40f, 0.18f, 0.7f));
            px.fillCircle(x, y, 2 + random.nextInt(3));
        }
    }

    private static void drawTropical(Pixmap px, Color accent, Random random, int size) {
        drawSand(px, random, size);
        int puddles = 6;
        for (int i = 0; i < puddles; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.35f, 0.68f, 0.80f, 0.85f));
            px.fillCircle(x + 8, y + 5, 8 + random.nextInt(6));
            px.setColor(accent);
            px.fillCircle(x + 6, y + 6, 6 + random.nextInt(4));
        }
        int fronds = 12;
        for (int i = 0; i < fronds; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.35f, 0.60f, 0.30f, 1f));
            px.fillRectangle(x, y, 8, 3);
            px.fillRectangle(x + 2, y + 3, 8, 3);
        }
    }

    /** Offsets an accent so it is never drawn across the tile edge (keeps tiling seamless). */
    private static int edgeSafe(Random random, int size) {
        return 6 + random.nextInt(Math.max(1, size - 24));
    }

    private static Color shade(Color c, float amt) {
        return new Color(c.r + amt, c.g + amt, c.b + amt, 1f);
    }

    private static Color lighten(Color c, float amt) {
        return new Color(
            c.r + (1f - c.r) * amt,
            c.g + (1f - c.g) * amt,
            c.b + (1f - c.b) * amt,
            1f);
    }
}
