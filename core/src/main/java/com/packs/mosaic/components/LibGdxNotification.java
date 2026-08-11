package com.packs.mosaic.components;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A persistent banner shown at the top of the stage.
 * Stays visible until dismiss() is called or the user taps "✕".
 * Slides in from the top on show().
 *
 * Usage:
 *   LibGdxNotification notif = new LibGdxNotification(
 *       skin, Alert.Success, "Deck saved successfully!");
 *   notif.show(stage);
 *   // later:
 *   notif.dismiss();
 */
public class LibGdxNotification extends Table {

    public enum Alert { INFO, ERROR, SUCCESS, WARNING }

    private static final float SLIDE_DURATION = 0.25f;
    private static final float DISMISS_DURATION = 0.20f;

    private final Stage ownerStage;

    public LibGdxNotification(Skin skin, Alert alert, String message) {
        this(skin, alert, message, null);
    }

    public LibGdxNotification(Skin skin, Alert alert, String message, Stage stage) {
        this.ownerStage = stage;

        String bgDrawable  = toBgDrawable(alert);
        String labelStyle  = toLabelStyle(alert);

        setBackground(skin.getDrawable(bgDrawable));
        pad(10f, 16f, 10f, 16f);

        Label messageLabel = new Label(message, skin, labelStyle);
        messageLabel.setWrap(true);

        LibGdxButton closeBtn = new LibGdxButton("✕", "ghost", LibGdxButton.Size.SM, skin,
            this::dismiss);

        add(messageLabel).expandX().fillX().left();
        add(closeBtn).right().padLeft(12f);

        // Start off-screen (above the top), slide down into position
        setY(10000f); // will be repositioned in show()
    }

    /**
     * Adds this notification to the stage and slides it in from the top.
     * The stage's viewport height is used to position it correctly.
     */
    public void show(Stage stage) {
        stage.addActor(this);

        float stageH = stage.getViewport().getWorldHeight();
        float stageW = stage.getViewport().getWorldWidth();

        setWidth(stageW);
        pack();

        float targetY = stageH - getHeight();
        setPosition(0, stageH); // start above visible area
        addAction(Actions.moveTo(0, targetY, SLIDE_DURATION, Interpolation.swingOut));
    }

    /** Slides the notification back up and removes it from the stage. */
    public void dismiss() {
        float stageH = getStage() != null
            ? getStage().getViewport().getWorldHeight()
            : getY() + getHeight() + 10f;
        addAction(Actions.sequence(
            Actions.moveTo(0, stageH, DISMISS_DURATION, Interpolation.swingIn),
            Actions.removeActor()
        ));
    }

    private static String toBgDrawable(Alert alert) {
        return switch (alert) {
            case INFO    -> "notif-info-bg";
            case SUCCESS -> "notif-success-bg";
            case WARNING -> "notif-warning-bg";
            case ERROR   -> "notif-error-bg";
        };
    }

    private static String toLabelStyle(Alert alert) {
        return switch (alert) {
            case INFO    -> "notif-info";
            case SUCCESS -> "notif-success";
            case WARNING -> "notif-warning";
            case ERROR   -> "notif-error";
        };
    }
}
