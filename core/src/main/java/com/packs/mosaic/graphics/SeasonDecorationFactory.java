package com.packs.mosaic.graphics;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.world.Season;

/**
 * Generates the per-season ground decoration layer (Task 4). Unlike the map
 * ground, this tile is transparent and only adds seasonal accents on top of
 * the world's own terrain: spring flower patches, summer sun-flecks, autumn
 * fallen-leaf piles and winter snow drifts. Drawn between the ground and the
 * season's overlay tint, so it reads as "this world, right now".
 */
public final class SeasonDecorationFactory implements Disposable {

    /** Tile size in pixels; each generated texture is 2x2 tiles so accents vary per tile. */
    public static final int TILE = 64;
    private static final int TILES_PER_SIDE = 2;

    private final ObjectMap<String, Texture> textures = new ObjectMap<>();

    /** Returns (creating on first use) the tiled decoration texture for a season. */
    public Texture get(Season season) {
        Texture texture = textures.get(season.getId());
        if (texture == null) {
            texture = new Texture(render(season.getDecoration()));
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            textures.put(season.getId(), texture);
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

    private static Pixmap render(Season.Decoration decoration) {
        int size = TILE * TILES_PER_SIDE;
        Pixmap px = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        px.setBlending(Pixmap.Blending.None);
        px.setColor(0f, 0f, 0f, 0f);
        px.fill();
        px.setBlending(Pixmap.Blending.SourceOver);

        Random random = new Random(decoration.name().hashCode());
        switch (decoration) {
            case FLOWERS:   drawFlowers(px, random, size); break;
            case SUNSHINE:  drawSunshine(px, random, size); break;
            case FALLEN:    drawFallen(px, random, size); break;
            case SNOWDRIFT: drawSnowdrift(px, random, size); break;
        }
        return px;
    }

    private static void drawFlowers(Pixmap px, Random random, int size) {
        int patches = 22;
        for (int i = 0; i < patches; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            int tone = random.nextInt(3);
            Color petal;
            if (tone == 0) petal = new Color(0.95f, 0.60f, 0.75f, 1f);
            else if (tone == 1) petal = new Color(0.98f, 0.85f, 0.40f, 1f);
            else petal = new Color(0.98f, 0.96f, 0.90f, 1f);
            px.setColor(petal);
            px.fillCircle(x, y + 3, 2);
            px.fillCircle(x, y - 3, 2);
            px.fillCircle(x + 3, y, 2);
            px.fillCircle(x - 3, y, 2);
            px.setColor(new Color(0.95f, 0.80f, 0.25f, 1f));
            px.fillCircle(x, y, 2);
        }
        int buds = 40;
        for (int i = 0; i < buds; i++) {
            px.setColor(new Color(0.95f, 0.45f, 0.65f, 0.9f));
            px.fillCircle(random.nextInt(size), random.nextInt(size), 1);
        }
    }

    private static void drawSunshine(Pixmap px, Random random, int size) {
        int flecks = 60;
        for (int i = 0; i < flecks; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.99f, 0.94f, 0.65f, 0.55f));
            px.fillCircle(x, y, 2 + random.nextInt(2));
        }
        int sparkles = 10;
        for (int i = 0; i < sparkles; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(1f, 0.97f, 0.80f, 0.8f));
            px.drawLine(x - 4, y, x + 4, y);
            px.drawLine(x, y - 4, x, y + 4);
        }
    }

    private static void drawFallen(Pixmap px, Random random, int size) {
        int piles = 16;
        for (int i = 0; i < piles; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.80f, 0.48f, 0.16f, 0.95f));
            px.fillCircle(x, y, 5 + random.nextInt(5));
            px.setColor(new Color(0.70f, 0.36f, 0.10f, 0.95f));
            px.fillCircle(x + 4, y + 2, 4);
            px.setColor(new Color(0.88f, 0.62f, 0.22f, 0.95f));
            px.fillCircle(x - 3, y + 3, 3);
        }
        int leaves = 70;
        for (int i = 0; i < leaves; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            int tone = random.nextInt(3);
            if (tone == 0) px.setColor(new Color(0.82f, 0.52f, 0.18f, 1f));
            else if (tone == 1) px.setColor(new Color(0.72f, 0.38f, 0.12f, 1f));
            else px.setColor(new Color(0.90f, 0.66f, 0.28f, 1f));
            px.fillRectangle(x, y, 4, 2);
            px.fillRectangle(x + 1, y - 1, 3, 1);
        }
    }

    private static void drawSnowdrift(Pixmap px, Random random, int size) {
        int drifts = 14;
        for (int i = 0; i < drifts; i++) {
            int x = edgeSafe(random, size);
            int y = edgeSafe(random, size);
            px.setColor(new Color(0.95f, 0.97f, 1f, 0.45f));
            px.fillCircle(x, y, 7 + random.nextInt(8));
            px.setColor(new Color(0.98f, 0.99f, 1f, 0.5f));
            px.fillCircle(x + 3, y - 2, 5);
        }
        int sparkles = 30;
        for (int i = 0; i < sparkles; i++) {
            px.setColor(new Color(0.90f, 0.95f, 1f, 0.5f));
            px.fillCircle(random.nextInt(size), random.nextInt(size), 1);
        }
    }

    /** Offsets an accent so it is never drawn across the tile edge (keeps tiling seamless). */
    private static int edgeSafe(Random random, int size) {
        return 6 + random.nextInt(Math.max(1, size - 24));
    }
}
