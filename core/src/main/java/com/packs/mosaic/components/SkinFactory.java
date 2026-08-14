package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Builds the shared Scene2D UI Skin entirely at runtime.
 * Drawables are generated as rounded-corner 9-patches (fill + border +
 * top gloss) so panels and buttons keep clean corners at any size.
 *
 * Registered styles:
 *   TextButton : primary / secondary / save / reset / ghost (+ default = primary)
 *   Button     : icon-toggle-sm / -md / -lg
 *   Label      : default / title / small
 *   Label      : notif-info / notif-error / notif-success / notif-warning
 *   Window     : modal
 *   ScrollPane : default
 *   List       : default
 *   SelectBox  : default
 *   Slider     : default-horizontal / default-vertical
 */
public final class SkinFactory {

    private static final Color DARK_PANEL   = new Color(0.17f, 0.17f, 0.22f, 1f);
    private static final Color DARK_MODAL   = new Color(0.13f, 0.13f, 0.19f, 1f);
    private static final Color ICON_BG      = new Color(0.20f, 0.20f, 0.28f, 1f);
    private static final Color ICON_ACTIVE  = new Color(0.35f, 0.38f, 0.60f, 1f);

    private static final Object[][] BUTTON_VARIANTS = {
        {"primary",   new Color(0.30f, 0.32f, 0.45f, 1f), new Color(0.45f, 0.48f, 0.65f, 1f), new Color(0.38f, 0.40f, 0.55f, 1f)},
        {"secondary", new Color(0.36f, 0.28f, 0.50f, 1f), new Color(0.50f, 0.40f, 0.68f, 1f), new Color(0.43f, 0.34f, 0.58f, 1f)},
        {"save",      new Color(0.16f, 0.45f, 0.35f, 1f), new Color(0.22f, 0.62f, 0.48f, 1f), new Color(0.19f, 0.53f, 0.41f, 1f)},
        {"reset",     new Color(0.55f, 0.18f, 0.20f, 1f), new Color(0.72f, 0.25f, 0.27f, 1f), new Color(0.63f, 0.21f, 0.23f, 1f)},
        {"ghost",     new Color(0.22f, 0.22f, 0.26f, 1f), new Color(0.32f, 0.32f, 0.38f, 1f), new Color(0.27f, 0.27f, 0.32f, 1f)},
    };

    // Alert type → background color
    static final Color COL_INFO    = new Color(0.20f, 0.40f, 0.65f, 1f);
    static final Color COL_SUCCESS = new Color(0.16f, 0.45f, 0.30f, 1f);
    static final Color COL_WARNING = new Color(0.60f, 0.45f, 0.10f, 1f);
    static final Color COL_ERROR   = new Color(0.55f, 0.18f, 0.18f, 1f);

    private SkinFactory() {}

    public static Skin buildDefaultSkin() {
        Skin skin = new Skin();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            com.badlogic.gdx.Gdx.files.internal("font/Fredoka-VariableFont_wdth,wght.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParameter.size = 24;
        BitmapFont font = generator.generateFont(fontParameter);

        FreeTypeFontGenerator.FreeTypeFontParameter smallFontParameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        smallFontParameter.size = 18;
        BitmapFont smallFont = generator.generateFont(smallFontParameter);
        generator.dispose();

        skin.add("default-font", font,      BitmapFont.class);
        skin.add("small-font",   smallFont, BitmapFont.class);

        // ── Drawables ──────────────────────────────────────────────────────
        skin.add("panel",    rounded(skin, "panel-tex",    DARK_PANEL, lighten(DARK_PANEL, 0.14f), lighten(DARK_PANEL, 0.45f)), Drawable.class);
        skin.add("modal-bg", rounded(skin, "modal-bg-tex", DARK_MODAL, lighten(DARK_MODAL, 0.16f), lighten(DARK_MODAL, 0.40f)), Drawable.class);
        skin.add("overlay",        solid(skin, "overlay-tex",        new Color(0f,    0f,    0f,    0.55f)), Drawable.class);
        skin.add("icon-toggle-bg", rounded(skin, "icon-toggle-bg-tex", ICON_BG,     darken(ICON_BG, 0.45f),     lighten(ICON_BG, 0.40f)), Drawable.class);
        skin.add("icon-toggle-on", rounded(skin, "icon-toggle-on-tex", ICON_ACTIVE, lighten(ICON_ACTIVE, 0.45f), lighten(ICON_ACTIVE, 0.50f)), Drawable.class);
        skin.add("notif-info-bg",    rounded(skin, "notif-info-tex",    COL_INFO,    darken(COL_INFO, 0.35f),    lighten(COL_INFO, 0.40f)), Drawable.class);
        skin.add("notif-success-bg", rounded(skin, "notif-success-tex", COL_SUCCESS, darken(COL_SUCCESS, 0.35f), lighten(COL_SUCCESS, 0.40f)), Drawable.class);
        skin.add("notif-warning-bg", rounded(skin, "notif-warning-tex", COL_WARNING, darken(COL_WARNING, 0.35f), lighten(COL_WARNING, 0.40f)), Drawable.class);
        skin.add("notif-error-bg",   rounded(skin, "notif-error-tex",   COL_ERROR,   darken(COL_ERROR, 0.35f),   lighten(COL_ERROR, 0.40f)), Drawable.class);

        // ── SelectBox / List / Slider colors ───────────────────────────────
        Color selectBg   = new Color(0.24f, 0.25f, 0.36f, 1f);
        Color selectOpen = new Color(0.35f, 0.38f, 0.60f, 1f);
        Color sliderFill = new Color(0.40f, 0.45f, 0.70f, 1f);
        Color sliderKnob = new Color(0.58f, 0.63f, 0.88f, 1f);
        skin.add("select-bg",      rounded(skin, "select-bg-tex",      selectBg,   darken(selectBg, 0.40f),   lighten(selectBg, 0.40f)), Drawable.class);
        skin.add("select-open-bg", rounded(skin, "select-open-tex",    selectOpen, darken(selectOpen, 0.35f), lighten(selectOpen, 0.40f)), Drawable.class);
        skin.add("select-over-bg", rounded(skin, "select-over-tex",    lighten(selectBg, 0.25f), darken(selectBg, 0.35f), lighten(selectBg, 0.45f)), Drawable.class);
        skin.add("list-bg",        rounded(skin, "list-bg-tex",        DARK_MODAL, darken(DARK_MODAL, 0.30f), lighten(DARK_MODAL, 0.35f)), Drawable.class);
        skin.add("list-sel-bg",    rounded(skin, "list-sel-tex",       selectOpen, darken(selectOpen, 0.30f), lighten(selectOpen, 0.35f)), Drawable.class);
        skin.add("slider-track",   rounded(skin, "slider-track-tex",   new Color(0.22f, 0.22f, 0.28f, 1f), darken(selectBg, 0.30f), lighten(selectBg, 0.20f)), Drawable.class);
        skin.add("slider-before",  rounded(skin, "slider-before-tex",  sliderFill, darken(sliderFill, 0.30f), lighten(sliderFill, 0.30f)), Drawable.class);
        skin.add("slider-knob",    rounded(skin, "slider-knob-tex",    sliderKnob, darken(sliderKnob, 0.35f), lighten(sliderKnob, 0.45f)), Drawable.class);

        // ── Label styles ───────────────────────────────────────────────────
        skin.add("default", new Label.LabelStyle(font,      Color.WHITE));
        skin.add("title",   new Label.LabelStyle(font,      new Color(0.85f, 0.85f, 1f,    1f)));
        skin.add("small",   new Label.LabelStyle(smallFont, new Color(0.75f, 0.75f, 0.85f, 1f)));

        // Notification text styles — same white text, bg color is on the container
        skin.add("notif-info",    new Label.LabelStyle(font, Color.WHITE));
        skin.add("notif-error",   new Label.LabelStyle(font, Color.WHITE));
        skin.add("notif-success", new Label.LabelStyle(font, new Color(0.85f, 1f, 0.85f, 1f)));
        skin.add("notif-warning", new Label.LabelStyle(font, new Color(1f,    1f, 0.75f, 1f)));

        // ── TextButton color variants ──────────────────────────────────────
        for (Object[] v : BUTTON_VARIANTS) {
            String name = (String) v[0];
            Color up   = (Color) v[1];
            Color down = (Color) v[2];
            Color over = (Color) v[3];

            TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
            s.up      = rounded(skin, name + "-up-tex",  up,   darken(up, 0.40f),   lighten(up, 0.50f));
            s.down    = rounded(skin, name + "-dn-tex",  down, darken(down, 0.40f), lighten(down, 0.15f));
            s.over    = rounded(skin, name + "-ov-tex",  over, lighten(over, 0.30f), lighten(over, 0.50f));
            s.checked = rounded(skin, name + "-ck-tex",  over, lighten(over, 0.45f), lighten(over, 0.55f));
            s.font      = font;
            s.fontColor = Color.WHITE;
            skin.add(name, s);
        }
        skin.add("default", skin.get("primary", TextButton.TextButtonStyle.class));

        // ── Icon toggle buttons ────────────────────────────────────────────
        for (String sz : new String[]{"sm", "md", "lg"}) {
            Button.ButtonStyle s = new Button.ButtonStyle();
            s.up      = skin.getDrawable("icon-toggle-bg");
            s.down    = skin.getDrawable("icon-toggle-on");
            s.over    = skin.getDrawable("icon-toggle-on");
            s.checked = skin.getDrawable("icon-toggle-on");
            skin.add("icon-toggle-" + sz, s);
        }

        // ── Window (used by LibGdxModel) ──────────────────────────────────
        Window.WindowStyle modalStyle = new Window.WindowStyle();
        modalStyle.background  = skin.getDrawable("modal-bg");
        modalStyle.titleFont   = font;
        modalStyle.titleFontColor = new Color(0.85f, 0.85f, 1f, 1f);
        skin.add("modal", modalStyle);

        // ── ScrollPane ────────────────────────────────────────────────────
        skin.add("default", new ScrollPane.ScrollPaneStyle());

        // ── List (SelectBox popup rows) ───────────────────────────────────
        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = new Color(0.75f, 0.75f, 0.85f, 1f);
        listStyle.selection = skin.getDrawable("list-sel-bg");
        skin.add("default", listStyle);

        // ── SelectBox ─────────────────────────────────────────────────────
        SelectBox.SelectBoxStyle selectStyle = new SelectBox.SelectBoxStyle();
        selectStyle.font = font;
        selectStyle.fontColor = Color.WHITE;
        selectStyle.background = skin.getDrawable("select-bg");
        selectStyle.backgroundOpen = skin.getDrawable("select-open-bg");
        selectStyle.backgroundOver = skin.getDrawable("select-over-bg");
        selectStyle.scrollStyle = skin.get("default", ScrollPane.ScrollPaneStyle.class);
        selectStyle.listStyle = listStyle;
        skin.add("default", selectStyle);

        // ── Slider (horizontal, knob + filled "before" rail) ──────────────
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = skin.getDrawable("slider-track");
        sliderStyle.knob = skin.getDrawable("slider-knob");
        sliderStyle.knobBefore = skin.getDrawable("slider-before");
        // Slider(min,max,step,vertical,skin) looks up "default-horizontal" /
        // "default-vertical" by convention, so register under those names.
        skin.add("default-horizontal", sliderStyle);
        skin.add("default-vertical", sliderStyle);

        return skin;
    }

    private static Drawable solid(Skin skin, String key, Color color) {
        Pixmap px = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        px.setColor(color);
        px.fill();
        Texture tex = new Texture(px);
        px.dispose();
        skin.add(key, tex, Texture.class);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    /**
     * Generates a rounded-corner 9-patch drawable: opaque fill, optional
     * 2px border and a subtle top gloss. Splits are placed just past the
     * corner radius so corners stay clean at any stretched size.
     */
    private static Drawable rounded(Skin skin, String key, Color fill, Color border, Color gloss) {
        int size = 32;
        int radius = 8;

        Pixmap px = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (inRounded(x, y, size, radius, 0)) {
                    px.drawPixel(x, y, Color.rgba8888(fill));
                }
            }
        }

        if (gloss != null) {
            int split = radius + 2;
            for (int y = 2; y < split / 2; y++) {
                for (int x = 0; x < size; x++) {
                    if (inRounded(x, y, size, radius, 0)) {
                        px.drawPixel(x, y, Color.rgba8888(lerpColor(fill, gloss, 0.65f)));
                    }
                }
            }
        }

        if (border != null) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (inRounded(x, y, size, radius, 0) && !inRounded(x, y, size, radius, 2)) {
                        px.drawPixel(x, y, Color.rgba8888(border));
                    }
                }
            }
        }

        Texture tex = new Texture(px);
        px.dispose();
        int split = radius + 2;
        NinePatch patch = new NinePatch(new TextureRegion(tex), split, split, split, split);
        skin.add(key, tex, Texture.class);
        return new NinePatchDrawable(patch);
    }

    /** True if (x, y) lies inside a rounded rect of the given size, radius and inset. */
    private static boolean inRounded(int x, int y, int size, int radius, int inset) {
        int r = Math.max(1, radius - inset);
        int lo = inset;
        int hi = size - inset - 1;
        if (x < lo || x > hi || y < lo || y > hi) return false;
        if (x >= lo + r && x <= hi - r) return true;
        if (y >= lo + r && y <= hi - r) return true;
        int cx = x < lo + r ? lo + r : hi - r;
        int cy = y < lo + r ? lo + r : hi - r;
        int dx = x - cx;
        int dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private static Color lerpColor(Color a, Color b, float t) {
        return new Color(
            a.r + (b.r - a.r) * t,
            a.g + (b.g - a.g) * t,
            a.b + (b.b - a.b) * t,
            a.a + (b.a - a.a) * t);
    }

    private static Color lighten(Color c, float amt) {
        return lerpColor(c, Color.WHITE, amt);
    }

    private static Color darken(Color c, float amt) {
        return lerpColor(c, Color.BLACK, amt);
    }

    public static Skin createSkin() {
        return buildDefaultSkin();
    }
}
