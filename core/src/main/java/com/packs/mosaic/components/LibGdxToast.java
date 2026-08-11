package com.packs.mosaic.components;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * A short auto-dismissing message that appears at the bottom of the
 * stage, stays for a configurable duration, then fades out and removes
 * itself. No user interaction required or supported.
 *
 * Usage:
 *   LibGdxToast.show(stage, skin, "Match saved!", 2.5f);
 *
 * Or short form (default 2 seconds):
 *   LibGdxToast.show(stage, skin, "Card added.");
 */
public class LibGdxToast extends Table {

    private static final float DEFAULT_DURATION  = 2.0f;
    private static final float FADE_IN_DURATION  = 0.18f;
    private static final float FADE_OUT_DURATION = 0.22f;
    private static final float BOTTOM_MARGIN     = 60f;

    private LibGdxToast(Skin skin, String message) {
        setBackground(skin.getDrawable("panel"));
        pad(10f, 20f, 10f, 20f);
        add(new Label(message, skin, "small"));
        pack();
        getColor().a = 0f; // start transparent for fade-in
    }

    public static void show(Stage stage, Skin skin, String message) {
        show(stage, skin, message, DEFAULT_DURATION);
    }

    public static void show(Stage stage, Skin skin, String message, float visibleSeconds) {
        LibGdxToast toast = new LibGdxToast(skin, message);

        float stageW = stage.getViewport().getWorldWidth();
        toast.setPosition(
            (stageW - toast.getWidth()) / 2f,
            BOTTOM_MARGIN
        );

        stage.addActor(toast);
        toast.addAction(Actions.sequence(
            Actions.fadeIn(FADE_IN_DURATION, Interpolation.fade),
            Actions.delay(visibleSeconds),
            Actions.fadeOut(FADE_OUT_DURATION, Interpolation.fade),
            Actions.removeActor()
        ));
    }
}
