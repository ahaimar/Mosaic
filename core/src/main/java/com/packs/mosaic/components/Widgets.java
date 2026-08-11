package com.packs.mosaic.components;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * One-liner factory methods for common UI elements so screen code
 * stays readable. All widget helpers live here — not in BaseScreen
 * or scattered across screens.
 */
public final class Widgets {

    private Widgets() {}

    public static LibGdxButton button(Skin skin, String text, String variant,
                                      LibGdxButton.Size size, Runnable onClick) {
        return new LibGdxButton(text, variant, size, skin, onClick);
    }

    /** Default variant (primary) and size (MD). */
    public static LibGdxButton button(Skin skin, String text, Runnable onClick) {
        return button(skin, text, "primary", LibGdxButton.Size.MD, onClick);
    }

    /** Named variant, default size (MD). */
    public static LibGdxButton button(Skin skin, String text, String variant, Runnable onClick) {
        return button(skin, text, variant, LibGdxButton.Size.MD, onClick);
    }

    public static Label title(Skin skin, String text) {
        Label label = new Label(text, skin, "title");
        label.setFontScale(3f);
        return label;
    }

    public static Label label(Skin skin, String text) {
        return new Label(text, skin);
    }

    public static Label smallLabel(Skin skin, String text) {
        return new Label(text, skin, "small");
    }
}
