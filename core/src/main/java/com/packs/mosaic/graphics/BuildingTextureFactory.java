package com.packs.mosaic.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.packs.mosaic.world.BuildingType;
import com.packs.mosaic.world.Season;

/**
 * Renders a small stylized vector-style sprite for every BuildingType
 * into a cached 64x64 Texture (one per type id). Textures are generated
 * once and reused every frame, and the grid draws them with a SpriteBatch
 * stretched to each object's footprint — no external art assets required.
 *
 * Task 4 — vegetation also gets a per-season sprite variant (amber in
 * autumn, pale snow-dusted in winter, brighter in summer), so trees
 * visibly change with the season while buildings stay put on the grid.
 *
 * The sprite shapes are intentionally flat/geometric to match the spec's
 * "placeholder upgrade": a house is a box + triangle roof, a tree a
 * circle canopy over a trunk, a road a dashed asphalt strip, etc.
 */
public final class BuildingTextureFactory implements Disposable {

    public static final int SIZE = 64;

    /** Types whose base colour is re-tinted by the current season. */
    private static final String[] SEASONAL_IDS = {
        "tree", "bush", "flower", "garden", "pine", "palm", "coconut_palm"
    };

    private final ObjectMap<String, Texture> textures = new ObjectMap<>();

    /** Returns (creating on first use) the sprite texture for a building type. */
    public Texture get(BuildingType type) {
        Texture texture = textures.get(type.getId());
        if (texture == null) {
            texture = new Texture(render(type, type.getPlaceholderColor()));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(type.getId(), texture);
        }
        return texture;
    }

    /**
     * Season-aware variant: vegetation re-renders through the season's
     * colour transform (spring reuses the base sprite). Non-vegetation
     * types always share the base sprite.
     */
    public Texture get(BuildingType type, Season season) {
        if (season == null || season == Season.SPRING || !isSeasonal(type)) {
            return get(type);
        }
        String key = type.getId() + "@" + season.getId();
        Texture texture = textures.get(key);
        if (texture == null) {
            texture = new Texture(render(type, season.tint(type.getPlaceholderColor())));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(key, texture);
        }
        return texture;
    }

    private static boolean isSeasonal(BuildingType type) {
        for (String id : SEASONAL_IDS) {
            if (id.equals(type.getId())) return true;
        }
        return false;
    }

    /** Frees every generated texture. Safe to call more than once. */
    @Override
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }

    private static Pixmap render(BuildingType type, Color base) {
        Pixmap px = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        px.setBlending(Pixmap.Blending.None);
        px.setColor(0f, 0f, 0f, 0f);
        px.fill();
        px.setBlending(Pixmap.Blending.SourceOver);

        switch (type.getId()) {
            case "tree":        drawTree(px, base); break;
            case "bush":        drawBush(px, base); break;
            case "flower":      drawFlower(px, base); break;
            case "rock":        drawRock(px, base); break;
            case "bench":       drawBench(px, base); break;
            case "street_lamp": drawStreetLamp(px, base); break;
            case "road_straight": drawRoad(px, false); break;
            case "road_corner":   drawRoadCorner(px); break;
            case "road_cross":    drawRoad(px, true); break;
            case "bridge_small":  drawBridge(px, base); break;
            case "shop":          drawShop(px, base); break;
            case "school":        drawSchool(px, base); break;
            case "playground":    drawPlayground(px, base); break;
            case "garden":        drawGarden(px, base); break;
            case "fence":         drawFence(px, base); break;
            case "crystal":       drawCrystal(px, base); break;
            case "tower":         drawTower(px, base); break;
            case "magic_gate":    drawMagicGate(px, base); break;
            case "flower_palace": drawFlowerPalace(px, base); break;
            case "grand_town_hall": drawGrandTownHall(px, base); break;
            case "wizard_tower":  drawWizardTower(px, base); break;
            case "cozy_cottage":  drawCozyCottage(px, base); break;
            case "pine":          drawPine(px, base); break;
            case "mushroom":      drawMushroom(px, base); break;
            case "cabin":         drawCabin(px, base); break;
            case "palm":          drawPalm(px, base); break;
            case "beach_umbrella": drawBeachUmbrella(px, base); break;
            case "lifeguard_hut": drawLifeguardHut(px, base); break;
            case "summit_rock":   drawSummitRock(px, base); break;
            case "alpine_hut":    drawAlpineHut(px, base); break;
            case "mountain_lookout": drawMountainLookout(px, base); break;
            case "snowman":       drawSnowman(px, base); break;
            case "igloo":         drawIgloo(px, base); break;
            case "ski_lodge":     drawSkiLodge(px, base); break;
            case "lava_rock":     drawLavaRock(px, base); break;
            case "magma_spring":  drawMagmaSpring(px, base); break;
            case "obsidian_tower": drawObsidianTower(px, base); break;
            case "coconut_palm":  drawCoconutPalm(px, base); break;
            case "tiki_hut":      drawTikiHut(px, base); break;
            case "coral_spring":  drawCoralSpring(px, base); break;
            case "lumber_hut":
            case "stone_mine":
            case "farm":
            case "workshop":
            case "dairy":
            case "coop":
            case "ranch":
            case "iron_mine":
            case "coal_mine":
            case "smelter":
            case "carpentry":
            case "machine_factory":
            case "assembly_factory":
            case "warehouse":
            case "large_warehouse":
                                  drawIndustrial(px, base); break;
            default:              drawHouse(px, base); break;
        }
        return px;
    }

    private static void drawHouse(Pixmap px, Color base) {
        Color roof = darken(base, 0.25f);
        Color wall = lighten(base, 0.15f);
        px.setColor(wall);
        px.fillRectangle(8, 18, 48, 34);
        px.setColor(roof);
        px.fillTriangle(4, 22, 32, 6, 60, 22);
        px.setColor(new Color(0.35f, 0.25f, 0.15f, 1f));
        px.fillRectangle(27, 18, 10, 16);
        window(px, 12, 40, 14);
        window(px, 38, 40, 14);
    }

    private static void drawShop(Pixmap px, Color base) {
        Color wall = lighten(base, 0.2f);
        Color awning = darken(base, 0.25f);
        px.setColor(wall);
        px.fillRectangle(6, 18, 52, 40);
        px.setColor(awning);
        px.fillRectangle(4, 46, 56, 12);
        px.setColor(darken(awning, 0.3f));
        for (int x = 4; x < 60; x += 16) {
            px.fillRectangle(x, 46, 8, 12);
        }
        px.setColor(new Color(0.30f, 0.20f, 0.12f, 1f));
        px.fillRectangle(27, 18, 10, 18);
        px.drawLine(6, 56, 6, 62);
        px.drawLine(57, 56, 57, 62);
    }

    private static void drawSchool(Pixmap px, Color base) {
        Color wall = lighten(base, 0.15f);
        Color roof = darken(base, 0.2f);
        px.setColor(wall);
        px.fillRectangle(6, 22, 52, 36);
        px.setColor(roof);
        px.fillTriangle(2, 26, 32, 8, 62, 26);
        px.setColor(new Color(0.8f, 0.8f, 0.9f, 1f));
        px.fillRectangle(20, 26, 24, 8);
        window(px, 10, 40, 12);
        window(px, 42, 40, 12);
        px.setColor(new Color(0.9f, 0.3f, 0.3f, 1f));
        px.fillTriangle(32, 14, 35, 4, 38, 14);
    }

    private static void drawPlayground(Pixmap px, Color base) {
        px.setColor(lighten(base, 0.1f));
        px.fillRectangle(6, 8, 52, 24);
        px.setColor(darken(base, 0.2f));
        px.fillRectangle(10, 32, 8, 16);
        px.fillRectangle(46, 32, 8, 16);
        px.drawLine(14, 48, 50, 48);
        px.setColor(new Color(0.9f, 0.2f, 0.2f, 1f));
        px.fillCircle(30, 18, 9);
        px.setColor(new Color(0.9f, 0.8f, 0.2f, 1f));
        px.fillCircle(46, 18, 6);
        px.fillCircle(14, 14, 4);
    }

    private static void drawTree(Pixmap px, Color base) {
        Color trunk = new Color(0.45f, 0.32f, 0.18f, 1f);
        px.setColor(trunk);
        px.fillRectangle(28, 12, 8, 24);
        px.setColor(base);
        px.fillCircle(32, 40, 20);
        px.setColor(lighten(base, 0.2f));
        px.fillCircle(24, 46, 10);
        px.fillCircle(40, 44, 8);
    }

    private static void drawBush(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(32, 32, 18);
        px.setColor(lighten(base, 0.18f));
        px.fillCircle(20, 38, 11);
        px.fillCircle(42, 36, 9);
        px.setColor(new Color(0.5f, 0.3f, 0.5f, 1f));
        px.fillCircle(22, 34, 3);
        px.fillCircle(42, 32, 3);
        px.fillCircle(32, 44, 3);
    }

    private static void drawFlower(Pixmap px, Color base) {
        px.setColor(new Color(0.25f, 0.55f, 0.25f, 1f));
        px.fillRectangle(31, 8, 2, 34);
        px.setColor(lighten(new Color(0.25f, 0.55f, 0.25f, 1f), 0.3f));
        px.fillRectangle(20, 12, 24, 3);
        px.setColor(base);
        px.fillCircle(32, 46, 10);
        px.setColor(lighten(base, 0.2f));
        px.fillCircle(32, 46, 6);
        px.setColor(new Color(0.95f, 0.85f, 0.3f, 1f));
        px.fillCircle(32, 46, 3);
    }

    private static void drawRock(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(22, 34, 12);
        px.fillCircle(42, 30, 14);
        px.setColor(darken(base, 0.2f));
        px.fillTriangle(10, 40, 32, 40, 21, 52);
        px.fillTriangle(32, 36, 56, 36, 44, 50);
        px.setColor(lighten(base, 0.3f));
        px.fillCircle(38, 40, 5);
        px.fillCircle(28, 44, 4);
    }

    private static void drawBench(Pixmap px, Color base) {
        Color wood = new Color(0.55f, 0.40f, 0.22f, 1f);
        px.setColor(wood);
        px.fillRectangle(8, 40, 48, 6);
        px.fillRectangle(8, 48, 48, 4);
        px.setColor(darken(wood, 0.25f));
        px.fillRectangle(10, 14, 5, 28);
        px.fillRectangle(49, 14, 5, 28);
        px.setColor(lighten(base, 0.2f));
        px.fillRectangle(12, 14, 2, 26);
        px.fillRectangle(50, 14, 2, 26);
    }

    private static void drawStreetLamp(Pixmap px, Color base) {
        Color pole = new Color(0.25f, 0.25f, 0.30f, 1f);
        px.setColor(pole);
        px.fillRectangle(30, 10, 4, 40);
        px.setColor(base);
        px.fillCircle(32, 52, 7);
        px.setColor(lighten(base, 0.5f));
        px.fillCircle(32, 52, 4);
        px.setColor(new Color(0.20f, 0.20f, 0.24f, 1f));
        px.fillRectangle(26, 10, 12, 4);
    }

    private static void drawRoad(Pixmap px, boolean vertical) {
        px.setColor(new Color(0.32f, 0.32f, 0.34f, 1f));
        px.fillRectangle(4, 4, 56, 56);
        px.setColor(new Color(0.45f, 0.45f, 0.48f, 1f));
        px.fillRectangle(4, 28, 56, 8);
        if (vertical) {
            px.fillRectangle(28, 4, 8, 56);
        }
        px.setColor(new Color(0.95f, 0.90f, 0.55f, 1f));
        for (int x = 4; x < 60; x += 14) {
            px.fillRectangle(x, 31, 8, 2);
        }
        if (vertical) {
            for (int y = 4; y < 60; y += 14) {
                px.fillRectangle(31, y, 2, 8);
            }
        }
    }

    private static void drawRoadCorner(Pixmap px) {
        px.setColor(new Color(0.32f, 0.32f, 0.34f, 1f));
        px.fillRectangle(4, 4, 28, 56);
        px.fillRectangle(4, 4, 56, 28);
        px.fillCircle(32, 32, 28);
        px.setColor(new Color(0.45f, 0.45f, 0.48f, 1f));
        px.fillCircle(32, 32, 20);
        px.setColor(new Color(0.95f, 0.90f, 0.55f, 1f));
        px.drawLine(32, 10, 32, 54);
        px.drawLine(10, 32, 54, 32);
        px.drawLine(18, 32, 32, 46);
        px.drawLine(46, 32, 32, 18);
    }

    private static void drawBridge(Pixmap px, Color base) {
        Color wood = new Color(0.60f, 0.45f, 0.25f, 1f);
        Color rail = darken(wood, 0.2f);
        px.setColor(new Color(0.15f, 0.35f, 0.55f, 1f));
        px.fillRectangle(2, 6, 60, 34);
        px.setColor(wood);
        px.fillRectangle(4, 20, 56, 18);
        px.setColor(lighten(wood, 0.1f));
        for (int x = 4; x < 60; x += 12) {
            px.fillRectangle(x, 20, 6, 18);
        }
        px.setColor(rail);
        px.fillRectangle(4, 42, 56, 4);
        px.fillRectangle(4, 50, 56, 2);
        px.setColor(base);
        px.fillRectangle(14, 30, 8, 8);
    }

    private static void drawGarden(Pixmap px, Color base) {
        px.setColor(new Color(0.55f, 0.42f, 0.28f, 1f));
        px.fillRectangle(6, 10, 52, 6);
        px.setColor(base);
        for (int x = 14; x <= 42; x += 14) {
            px.fillCircle(x, 34, 10);
            px.fillCircle(x + 6, 46, 8);
        }
        px.setColor(new Color(0.95f, 0.85f, 0.3f, 1f));
        px.fillCircle(20, 46, 3);
        px.fillCircle(36, 46, 3);
    }

    private static void drawFence(Pixmap px, Color base) {
        px.setColor(darken(base, 0.25f));
        for (int x = 8; x < 60; x += 12) {
            px.fillRectangle(x, 12, 6, 44);
        }
        px.setColor(base);
        px.fillRectangle(4, 44, 56, 7);
        px.fillRectangle(4, 26, 56, 7);
    }

    private static void drawCrystal(Pixmap px, Color base) {
        px.setColor(base);
        px.fillTriangle(32, 6, 14, 42, 32, 58);
        px.fillTriangle(32, 6, 50, 42, 32, 58);
        px.setColor(lighten(base, 0.4f));
        px.fillTriangle(32, 6, 32, 58, 24, 42);
        px.setColor(lighten(base, 0.7f));
        px.fillTriangle(32, 20, 38, 38, 32, 48);
    }

    private static void drawTower(Pixmap px, Color base) {
        Color wall = lighten(base, 0.15f);
        Color roof = darken(base, 0.3f);
        px.setColor(wall);
        px.fillRectangle(16, 10, 32, 44);
        px.setColor(roof);
        px.fillTriangle(10, 56, 32, 62, 54, 56);
        px.setColor(darken(roof, 0.25f));
        px.fillTriangle(31, 62, 33, 62, 32, 70);
        window(px, 22, 26, 12);
        px.setColor(new Color(0.35f, 0.3f, 0.35f, 1f));
        px.fillRectangle(14, 10, 36, 4);
    }

    private static void drawMagicGate(Pixmap px, Color base) {
        px.setColor(darken(base, 0.25f));
        px.fillRectangle(10, 8, 44, 52);
        px.setColor(new Color(0.1f, 0.1f, 0.2f, 1f));
        px.fillRectangle(18, 8, 28, 34);
        px.fillCircle(32, 42, 14);
        px.setColor(base);
        px.fillCircle(32, 44, 8);
        px.setColor(lighten(base, 0.5f));
        px.fillCircle(32, 44, 4);
    }

    private static void drawFlowerPalace(Pixmap px, Color base) {
        px.setColor(lighten(base, 0.2f));
        px.fillRectangle(6, 16, 52, 34);
        px.setColor(base);
        px.fillCircle(14, 48, 8);
        px.fillCircle(50, 48, 8);
        px.setColor(lighten(base, 0.3f));
        px.fillCircle(14, 48, 5);
        px.fillCircle(50, 48, 5);
        px.setColor(new Color(0.95f, 0.85f, 0.3f, 1f));
        px.fillCircle(14, 48, 3);
        px.fillCircle(50, 48, 3);
        px.setColor(darken(base, 0.2f));
        px.fillTriangle(20, 26, 32, 8, 44, 26);
        px.setColor(new Color(0.55f, 0.75f, 0.95f, 1f));
        px.fillRectangle(26, 16, 12, 10);
        window(px, 12, 40, 10);
        window(px, 42, 40, 10);
    }

    private static void drawGrandTownHall(Pixmap px, Color base) {
        Color wall = lighten(base, 0.12f);
        Color roof = darken(base, 0.25f);
        px.setColor(wall);
        px.fillRectangle(6, 18, 52, 30);
        px.setColor(roof);
        px.fillTriangle(2, 26, 32, 6, 62, 26);
        px.setColor(darken(roof, 0.2f));
        px.fillRectangle(30, 6, 4, 16);
        px.setColor(new Color(0.55f, 0.75f, 0.95f, 1f));
        px.fillCircle(32, 24, 7);
        px.setColor(new Color(0.95f, 0.85f, 0.3f, 1f));
        px.fillCircle(32, 24, 3);
        window(px, 14, 40, 12);
        window(px, 40, 40, 12);
        px.setColor(new Color(0.35f, 0.25f, 0.15f, 1f));
        px.fillRectangle(30, 18, 4, 14);
    }

    private static void drawWizardTower(Pixmap px, Color base) {
        drawTower(px, base);
        px.setColor(new Color(0.95f, 0.85f, 0.3f, 1f));
        px.fillTriangle(32, 14, 29, 22, 35, 22);
        px.fillTriangle(32, 26, 29, 20, 35, 20);
    }

    private static void drawCozyCottage(Pixmap px, Color base) {
        drawHouse(px, base);
        px.setColor(new Color(0.5f, 0.35f, 0.2f, 1f));
        px.fillRectangle(52, 44, 6, 18);
        px.setColor(new Color(0.85f, 0.85f, 0.9f, 1f));
        px.fillRectangle(50, 58, 4, 3);
    }

    private static void drawPine(Pixmap px, Color base) {
        px.setColor(new Color(0.40f, 0.28f, 0.16f, 1f));
        px.fillRectangle(30, 10, 4, 18);
        px.setColor(base);
        px.fillTriangle(16, 34, 32, 10, 48, 34);
        px.fillTriangle(14, 44, 32, 18, 50, 44);
        px.fillTriangle(18, 54, 32, 30, 46, 54);
        px.setColor(lighten(base, 0.2f));
        px.fillCircle(30, 46, 3);
    }

    private static void drawMushroom(Pixmap px, Color base) {
        px.setColor(new Color(0.95f, 0.90f, 0.75f, 1f));
        px.fillRectangle(30, 12, 5, 26);
        px.setColor(base);
        px.fillCircle(32, 42, 16);
        px.setColor(darken(base, 0.15f));
        px.fillRectangle(12, 38, 40, 6);
        px.setColor(new Color(0.98f, 0.95f, 0.9f, 1f));
        px.fillCircle(24, 48, 3);
        px.fillCircle(36, 52, 3);
        px.fillCircle(42, 44, 2);
    }

    private static void drawCabin(Pixmap px, Color base) {
        Color wall = lighten(base, 0.12f);
        Color roof = darken(base, 0.25f);
        px.setColor(wall);
        px.fillRectangle(8, 16, 48, 36);
        px.setColor(roof);
        px.fillTriangle(3, 22, 32, 4, 61, 22);
        px.setColor(lighten(base, 0.25f));
        px.drawLine(8, 24, 56, 24);
        px.drawLine(8, 34, 56, 34);
        px.drawLine(8, 44, 56, 44);
        px.setColor(new Color(0.30f, 0.22f, 0.14f, 1f));
        px.fillRectangle(24, 16, 16, 22);
        px.setColor(new Color(0.55f, 0.75f, 0.95f, 1f));
        px.fillRectangle(10, 28, 8, 8);
    }

    private static void drawPalm(Pixmap px, Color base) {
        px.setColor(new Color(0.45f, 0.30f, 0.16f, 1f));
        px.fillRectangle(30, 10, 5, 34);
        px.setColor(base);
        px.fillCircle(32, 50, 14);
        px.setColor(lighten(base, 0.15f));
        px.fillCircle(20, 50, 7);
        px.fillCircle(30, 56, 7);
        px.fillCircle(32, 44, 7);
        px.fillCircle(44, 52, 7);
    }

    private static void drawBeachUmbrella(Pixmap px, Color base) {
        px.setColor(new Color(0.9f, 0.85f, 0.7f, 1f));
        px.fillRectangle(31, 14, 3, 26);
        px.setColor(base);
        px.fillCircle(32, 44, 18);
        px.setColor(darken(base, 0.25f));
        for (int i = 0; i < 4; i++) {
            float angle = (float) (Math.PI * i / 4);
            px.fillRectangle((int) (32 + Math.cos(angle) * 14) - 2, (int) (44 + Math.sin(angle) * 14) - 2, 4, 14);
        }
        px.setColor(new Color(0.98f, 0.95f, 0.85f, 1f));
        px.fillCircle(32, 44, 5);
        px.setColor(new Color(0.75f, 0.55f, 0.3f, 1f));
        px.fillRectangle(29, 8, 6, 8);
    }

    private static void drawLifeguardHut(Pixmap px, Color base) {
        Color wall = lighten(base, 0.15f);
        px.setColor(wall);
        px.fillRectangle(10, 20, 44, 30);
        px.setColor(darken(base, 0.2f));
        px.fillRectangle(8, 14, 48, 6);
        px.fillRectangle(14, 8, 5, 8);
        px.fillRectangle(45, 8, 5, 8);
        px.setColor(new Color(0.95f, 0.98f, 1f, 1f));
        px.fillRectangle(22, 30, 20, 14);
        px.setColor(new Color(0.85f, 0.2f, 0.2f, 1f));
        px.fillRectangle(29, 30, 6, 14);
        px.fillRectangle(22, 35, 20, 4);
    }

    private static void drawSummitRock(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(20, 30, 13);
        px.fillCircle(44, 26, 15);
        px.setColor(darken(base, 0.2f));
        px.fillTriangle(8, 34, 32, 34, 20, 56);
        px.fillTriangle(30, 30, 58, 30, 46, 52);
        px.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        px.fillTriangle(20, 56, 32, 34, 26, 44);
        px.setColor(lighten(base, 0.25f));
        px.fillCircle(40, 36, 4);
    }

    private static void drawAlpineHut(Pixmap px, Color base) {
        drawCabin(px, base);
        px.setColor(new Color(0.92f, 0.94f, 0.98f, 1f));
        px.fillRectangle(8, 42, 48, 4);
        px.setColor(new Color(0.95f, 0.97f, 1f, 1f));
        px.fillTriangle(16, 52, 28, 40, 40, 52);
    }

    private static void drawMountainLookout(Pixmap px, Color base) {
        Color wood = darken(base, 0.15f);
        px.setColor(new Color(0.35f, 0.30f, 0.25f, 1f));
        px.fillRectangle(14, 8, 5, 40);
        px.fillRectangle(45, 8, 5, 40);
        px.setColor(wood);
        px.fillRectangle(8, 40, 48, 8);
        px.setColor(lighten(wood, 0.15f));
        px.fillRectangle(10, 48, 44, 6);
        px.setColor(darken(wood, 0.15f));
        px.fillRectangle(16, 28, 3, 12);
        px.fillRectangle(45, 28, 3, 12);
        px.setColor(darken(base, 0.3f));
        px.fillTriangle(4, 58, 32, 46, 60, 58);
        px.fillTriangle(12, 64, 32, 54, 52, 64);
    }

    private static void drawSnowman(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(32, 24, 14);
        px.fillCircle(32, 42, 11);
        px.fillCircle(32, 56, 8);
        px.setColor(new Color(0.25f, 0.3f, 0.4f, 1f));
        px.fillRectangle(26, 62, 12, 5);
        px.fillRectangle(23, 64, 18, 3);
        px.setColor(new Color(0.2f, 0.2f, 0.25f, 1f));
        px.fillCircle(29, 56, 2);
        px.fillCircle(35, 56, 2);
        px.fillCircle(32, 54, 2);
        px.setColor(new Color(0.95f, 0.45f, 0.3f, 1f));
        px.fillTriangle(32, 48, 28, 52, 36, 52);
        px.fillCircle(28, 24, 2);
        px.fillCircle(36, 24, 2);
    }

    private static void drawIgloo(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(32, 26, 20);
        px.setColor(darken(base, 0.15f));
        px.fillRectangle(12, 8, 40, 8);
        px.setColor(new Color(0.30f, 0.35f, 0.45f, 1f));
        px.fillCircle(26, 18, 9);
        px.fillCircle(24, 18, 8);
        px.setColor(lighten(base, 0.1f));
        px.drawLine(24, 28, 40, 28);
        px.drawLine(28, 38, 44, 38);
    }

    private static void drawSkiLodge(Pixmap px, Color base) {
        Color wall = lighten(base, 0.15f);
        Color roof = darken(base, 0.2f);
        px.setColor(wall);
        px.fillRectangle(6, 18, 52, 30);
        px.setColor(roof);
        px.fillTriangle(2, 24, 32, 6, 62, 24);
        px.setColor(darken(roof, 0.2f));
        px.fillRectangle(30, 8, 4, 10);
        window(px, 12, 36, 12);
        window(px, 40, 36, 12);
        px.setColor(new Color(0.90f, 0.94f, 0.98f, 1f));
        px.fillRectangle(6, 44, 52, 4);
    }

    private static void drawLavaRock(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(22, 30, 13);
        px.fillCircle(42, 26, 15);
        px.setColor(darken(base, 0.2f));
        px.fillTriangle(8, 32, 32, 32, 20, 54);
        px.fillTriangle(30, 28, 58, 28, 46, 50);
        px.setColor(new Color(0.95f, 0.4f, 0.15f, 1f));
        px.drawLine(20, 50, 26, 40);
        px.drawLine(26, 40, 24, 32);
        px.setColor(new Color(0.85f, 0.35f, 0.12f, 1f));
        px.fillCircle(24, 34, 2);
        px.fillCircle(44, 40, 2);
    }

    private static void drawMagmaSpring(Pixmap px, Color base) {
        px.setColor(darken(base, 0.35f));
        px.fillRectangle(8, 8, 48, 30);
        px.setColor(base);
        px.fillCircle(32, 24, 17);
        px.setColor(lighten(base, 0.25f));
        px.fillCircle(32, 24, 11);
        px.setColor(lighten(base, 0.55f));
        px.fillCircle(32, 24, 5);
        px.setColor(darken(base, 0.3f));
        px.fillTriangle(24, 40, 40, 40, 32, 56);
        px.setColor(new Color(0.55f, 0.35f, 0.25f, 1f));
        px.fillCircle(12, 48, 6);
        px.fillCircle(50, 50, 7);
    }

    private static void drawObsidianTower(Pixmap px, Color base) {
        px.setColor(base);
        px.fillRectangle(18, 10, 28, 46);
        px.setColor(lighten(base, 0.15f));
        px.fillRectangle(22, 10, 8, 46);
        px.setColor(darken(base, 0.3f));
        px.fillTriangle(14, 58, 32, 66, 50, 58);
        px.setColor(new Color(0.75f, 0.4f, 0.9f, 1f));
        px.fillCircle(32, 34, 7);
        px.setColor(new Color(0.98f, 0.8f, 1f, 1f));
        px.fillCircle(32, 34, 3);
        px.setColor(new Color(0.45f, 0.4f, 0.55f, 1f));
        px.fillRectangle(14, 10, 36, 4);
    }

    private static void drawCoconutPalm(Pixmap px, Color base) {
        drawPalm(px, base);
        px.setColor(new Color(0.55f, 0.4f, 0.22f, 1f));
        px.fillCircle(29, 42, 4);
        px.fillCircle(35, 41, 4);
        px.setColor(lighten(base, 0.1f));
        px.fillCircle(44, 50, 4);
    }

    private static void drawTikiHut(Pixmap px, Color base) {
        px.setColor(new Color(0.35f, 0.25f, 0.16f, 1f));
        px.fillRectangle(16, 10, 6, 22);
        px.fillRectangle(42, 10, 6, 22);
        px.setColor(lighten(base, 0.15f));
        px.fillRectangle(12, 28, 40, 6);
        px.setColor(base);
        px.fillTriangle(4, 36, 32, 62, 60, 36);
        px.setColor(darken(base, 0.2f));
        px.drawLine(10, 38, 54, 38);
        px.setColor(new Color(0.25f, 0.2f, 0.14f, 1f));
        px.fillRectangle(26, 14, 12, 14);
    }

    private static void drawCoralSpring(Pixmap px, Color base) {
        px.setColor(base);
        px.fillCircle(32, 24, 17);
        px.setColor(new Color(0.55f, 0.55f, 0.75f, 1f));
        px.fillCircle(32, 24, 11);
        px.setColor(new Color(0.95f, 0.3f, 0.4f, 1f));
        px.fillCircle(24, 26, 3);
        px.fillCircle(40, 28, 4);
        px.fillCircle(32, 18, 3);
        px.setColor(new Color(0.98f, 0.8f, 0.4f, 1f));
        px.fillCircle(36, 24, 2);
        px.fillCircle(28, 34, 2);
        px.setColor(new Color(0.3f, 0.45f, 0.6f, 1f));
        px.fillRectangle(30, 34, 4, 16);
    }

    private static void window(Pixmap px, int x, int y, int size) {
        px.setColor(new Color(0.55f, 0.75f, 0.95f, 1f));
        px.fillRectangle(x, y, size, size);
        px.setColor(new Color(0.25f, 0.20f, 0.12f, 1f));
        px.drawRectangle(x, y, size, size);
        px.drawLine(x + size / 2, y, x + size / 2, y + size);
    }

    private static Color lighten(Color c, float amt) {
        return new Color(
            c.r + (1f - c.r) * amt,
            c.g + (1f - c.g) * amt,
            c.b + (1f - c.b) * amt,
            1f);
    }

    private static Color darken(Color c, float amt) {
        return new Color(c.r * (1f - amt), c.g * (1f - amt), c.b * (1f - amt), 1f);
    }

    /** Simple workshop/warehouse: box with a pitched roof and a pipe stack. */
    private static void drawIndustrial(Pixmap px, Color base) {
        Color wall = lighten(base, 0.2f);
        Color roof = darken(base, 0.35f);
        px.setColor(wall);
        px.fillRectangle(10, 16, 44, 32);
        px.setColor(roof);
        px.fillRectangle(6, 40, 52, 8);
        px.fillRectangle(34, 6, 12, 12);
        px.setColor(darken(base, 0.45f));
        px.fillRectangle(26, 28, 12, 20);
        px.fillRectangle(40, 20, 8, 8);
    }
}
