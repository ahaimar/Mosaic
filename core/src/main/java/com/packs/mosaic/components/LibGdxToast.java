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
 *   LibGdxToast.show(stage, skin, Kind.SUCCESS, "Match saved!", 2.5f);
 *
 * Or short form (default 2 seconds):
 *   LibGdxToast.show(stage, skin, "Card added.");
 */
public class LibGdxToast extends Table {

    public enum Kind { INFO, SUCCESS, WARNING, ERROR }

    private static final float DEFAULT_DURATION  = 2.0f;
    private static final float FADE_IN_DURATION  = 0.18f;
    private static final float FADE_OUT_DURATION = 0.22f;
    private static final float BOTTOM_MARGIN     = 60f;

    private LibGdxToast(Skin skin, Kind kind, String message) {
        setBackground(skin.getDrawable(toBgDrawable(kind)));
        pad(10f, 20f, 10f, 20f);
        add(new Label(message, skin, toLabelStyle(kind)));
        pack();
        getColor().a = 0f; // start transparent for fade-in
    }

    public static void show(Stage stage, Skin skin, String message) {
        show(stage, skin, Kind.INFO, message, DEFAULT_DURATION);
    }

    public static void show(Stage stage, Skin skin, String message, float visibleSeconds) {
        show(stage, skin, Kind.INFO, message, visibleSeconds);
    }

    public static void show(Stage stage, Skin skin, Kind kind, String message) {
        show(stage, skin, kind, message, DEFAULT_DURATION);
    }

    public static void show(Stage stage, Skin skin, Kind kind, String message, float visibleSeconds) {
        LibGdxToast toast = new LibGdxToast(skin, kind, message);

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

    private static String toBgDrawable(Kind kind) {
        return switch (kind) {
            case INFO    -> "notif-info-bg";
            case SUCCESS -> "notif-success-bg";
            case WARNING -> "notif-warning-bg";
            case ERROR   -> "notif-error-bg";
        };
    }

    private static String toLabelStyle(Kind kind) {
        return switch (kind) {
            case INFO    -> "notif-info";
            case SUCCESS -> "notif-success";
            case WARNING -> "notif-warning";
            case ERROR   -> "notif-error";
        };
    }
}
