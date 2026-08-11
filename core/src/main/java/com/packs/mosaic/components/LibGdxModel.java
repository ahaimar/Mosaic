package com.packs.mosaic.components;


import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A modal dialog: blocks interaction behind a semi-transparent overlay,
 * shows a title + message, and one or two action buttons.
 *
 * The class is intentionally named "LibGdxModel" to match the stub you
 * started — but semantically this is a modal dialog, not a data model.
 *
 * Usage — simple confirm/cancel:
 *   LibGdxModel.show(stage, skin,
 *       "Discard deck?",
 *       "All unsaved changes will be lost.",
 *       "Discard", () -> game.changeScreen(...),
 *       "Cancel",  null);
 *
 * Usage — single OK button:
 *   LibGdxModel.show(stage, skin,
 *       "Error", "Card database failed to load.",
 *       "OK", null,
 *       null, null);
 */
public class LibGdxModel extends Group {

    private static final float SCALE_DURATION = 0.22f;
    private static final float MODAL_WIDTH    = 420f;

    private LibGdxModel(Stage stage, Skin skin,
                        String title, String message,
                        String confirmLabel, Runnable onConfirm,
                        String cancelLabel,  Runnable onCancel) {

        float stageW = stage.getViewport().getWorldWidth();
        float stageH = stage.getViewport().getWorldHeight();

        // ── Overlay — fills the whole stage and blocks input ──────────────
        Image overlay = new Image(skin.getDrawable("overlay"));
        overlay.setSize(stageW, stageH);
        overlay.addListener(new ClickListener() {}); // consume all clicks behind modal
        addActor(overlay);

        // ── Dialog window ─────────────────────────────────────────────────
        Window window = new Window(title, skin, "modal");
        window.setMovable(false);
        window.pad(20f);

        Label messageLabel = new Label(message, skin);
        messageLabel.setWrap(true);
        window.add(messageLabel).width(MODAL_WIDTH - 40f).padBottom(24f).row();

        Table buttons = new Table();

        if (confirmLabel != null) {
            LibGdxButton confirmBtn = new LibGdxButton(confirmLabel, "primary",
                LibGdxButton.Size.MD, skin, () -> {
                dismiss();
                if (onConfirm != null) onConfirm.run();
            });
            buttons.add(confirmBtn).padRight(cancelLabel != null ? 12f : 0f);
        }

        if (cancelLabel != null) {
            LibGdxButton cancelBtn = new LibGdxButton(cancelLabel, "ghost",
                LibGdxButton.Size.MD, skin, () -> {
                dismiss();
                if (onCancel != null) onCancel.run();
            });
            buttons.add(cancelBtn);
        }

        window.add(buttons).center().row();
        window.pack();

        // Centre the window on stage
        window.setPosition(
            (stageW - window.getWidth())  / 2f,
            (stageH - window.getHeight()) / 2f
        );
        addActor(window);

        // Pop-in animation
        window.setScale(0.85f);
        window.addAction(Actions.scaleTo(1f, 1f, SCALE_DURATION, Interpolation.swingOut));
    }

    private void dismiss() {
        addAction(Actions.sequence(
            Actions.fadeOut(0.15f),
            Actions.removeActor()
        ));
    }

    /** Shows a modal dialog on the given stage. Returns the modal instance for programmatic dismiss. */
    public static LibGdxModel show(Stage stage, Skin skin,
                                   String title,  String message,
                                   String confirmLabel, Runnable onConfirm,
                                   String cancelLabel,  Runnable onCancel) {
        LibGdxModel modal = new LibGdxModel(stage, skin,
            title, message,
            confirmLabel, onConfirm,
            cancelLabel,  onCancel);
        modal.setSize(stage.getViewport().getWorldWidth(),
            stage.getViewport().getWorldHeight());
        stage.addActor(modal);
        return modal;
    }
}
