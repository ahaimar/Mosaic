package com.packs.mosaic.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Builds the shared Scene2D UI Skin entirely at runtime.
 * No .json/.atlas/.png files required — placeholder visuals only.
 *
 * Registered styles:
 *   TextButton : primary / secondary / save / reset / ghost (+ default = primary)
 *   Button     : icon-toggle-sm / -md / -lg
 *   Label      : default / title / small
 *   Label      : notif-info / notif-error / notif-success / notif-warning
 *   Window     : modal
 *   ScrollPane : default
 */
public final class SkinFactory {

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

        BitmapFont font      = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.75f);

        skin.add("default-font", font,      BitmapFont.class);
        skin.add("small-font",   smallFont, BitmapFont.class);

        // ── Drawables ──────────────────────────────────────────────────────
        skin.add("panel",          solid(skin, "panel-tex",          new Color(0.18f, 0.18f, 0.24f, 1f)), Drawable.class);
        skin.add("modal-bg",       solid(skin, "modal-bg-tex",       new Color(0.14f, 0.14f, 0.20f, 1f)), Drawable.class);
        skin.add("overlay",        solid(skin, "overlay-tex",        new Color(0f,    0f,    0f,    0.55f)), Drawable.class);
        skin.add("icon-toggle-bg", solid(skin, "icon-toggle-bg-tex", new Color(0.20f, 0.20f, 0.28f, 1f)), Drawable.class);
        skin.add("icon-toggle-on", solid(skin, "icon-toggle-on-tex", new Color(0.35f, 0.38f, 0.60f, 1f)), Drawable.class);
        skin.add("notif-info-bg",    solid(skin, "notif-info-tex",    COL_INFO),    Drawable.class);
        skin.add("notif-success-bg", solid(skin, "notif-success-tex", COL_SUCCESS), Drawable.class);
        skin.add("notif-warning-bg", solid(skin, "notif-warning-tex", COL_WARNING), Drawable.class);
        skin.add("notif-error-bg",   solid(skin, "notif-error-tex",   COL_ERROR),   Drawable.class);

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
            TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
            s.up        = solid(skin, name + "-up-tex", (Color) v[1]);
            s.down      = solid(skin, name + "-dn-tex", (Color) v[2]);
            s.over      = solid(skin, name + "-ov-tex", (Color) v[3]);
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

    public static Skin createSkin() {
        return buildDefaultSkin();
    }
}
