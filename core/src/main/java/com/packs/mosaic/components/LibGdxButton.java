package com.packs.mosaic.components;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.packs.mosaic.audio.AudioManager;

/**
 * A TextButton with a semantic color variant and explicit size.
 *
 * Color comes from the skin style (variant name: primary/secondary/
 * save/reset/ghost). Size controls internal padding via pad() on the
 * actor — NOT via style fields, which don't exist on TextButtonStyle.
 *
 * Every button also gets tactile feedback: it dips down slightly while
 * pressed and plays the UI click sound (AudioManager null-guards the
 * headless test environment, so this is always safe).
 */
public class LibGdxButton extends TextButton {

    private static final float PRESS_SCALE = 0.90f;

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
        setOrigin(Align.center);

        addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                animatePress(true);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                animatePress(false);
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().play(AudioManager.Sfx.CLICK);
            }
        });
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

    private void animatePress(boolean pressed) {
        clearActions();
        addAction(Actions.scaleTo(
            pressed ? PRESS_SCALE : 1f,
            pressed ? PRESS_SCALE : 1f,
            0.08f,
            Interpolation.swingOut));
    }
}
