package com.packs.mosaic.components;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A TextButton with a semantic color variant and explicit size.
 *
 * Color comes from the skin style (variant name: primary/secondary/
 * save/reset/ghost). Size controls internal padding via pad() on the
 * actor — NOT via style fields, which don't exist on TextButtonStyle.
 */
public class LibGdxButton extends TextButton {

    public enum Size {
        SM  (10f,  6f),
        MD  (18f, 10f),
        LG  (28f, 14f);

        final float padX, padY;
        Size(float padX, float padY) { this.padX = padX; this.padY = padY; }
    }

    public LibGdxButton(String text, String variant, Size size, Skin skin) {
        super(text, skin, variant);
        pad(size.padY, size.padX, size.padY, size.padX); // top, left, bottom, right
    }

    public LibGdxButton(String text, String variant, Size size, Skin skin, Runnable onClick) {
        this(text, variant, size, skin);
        setOnClick(onClick);
    }

    public void setOnClick(Runnable onClick) {
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });
    }
}
