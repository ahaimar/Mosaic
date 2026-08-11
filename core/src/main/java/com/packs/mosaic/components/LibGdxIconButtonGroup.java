package com.packs.mosaic.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A segmented icon toggle group: one button checked at a time.
 *
 * Skin style names used: "icon-toggle-sm", "icon-toggle-md",
 * "icon-toggle-lg" — all registered by SkinFactory. Label style
 * "small" is also registered by SkinFactory for optional captions.
 */
public class LibGdxIconButtonGroup extends Table {

    public enum Size { SM, MD, LG }

    public interface OnChangeListener {
        void onChange(String value);
    }

    public static class Option {
        public final String   value;
        public final Drawable icon;
        public final String   caption; // nullable

        public Option(String value, Drawable icon) {
            this(value, icon, null);
        }

        public Option(String value, Drawable icon, String caption) {
            this.value   = value;
            this.icon    = icon;
            this.caption = caption;
        }
    }

    private final ButtonGroup<Button> buttonGroup;
    private OnChangeListener onChangeListener;

    public LibGdxIconButtonGroup(Skin skin, Option[] options, Size size) {
        super(skin);

        this.buttonGroup = new ButtonGroup<>();
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setUncheckLast(true);

        // Style name matches SkinFactory: "icon-toggle-sm/md/lg"
        String styleName = "icon-toggle-" + size.name().toLowerCase();
        float iconSize = switch (size) {
            case SM -> 24f;
            case MD -> 32f;
            case LG -> 44f;
        };

        for (Option opt : options) {
            Button button = new Button(skin, styleName);
            button.setProgrammaticChangeEvents(false);
            button.setUserObject(opt.value);

            Table content = new Table();
            content.add(new Image(opt.icon)).size(iconSize).row();
            if (opt.caption != null) {
                content.add(new Label(opt.caption, skin, "small")).padTop(2f);
            }
            button.add(content).expand().fill();

            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (button.isChecked() && onChangeListener != null) {
                        onChangeListener.onChange((String) button.getUserObject());
                    }
                }
            });

            buttonGroup.add(button);
            add(button).minWidth(iconSize + 20f).growX().fillY().uniformX();
        }

        if (options.length > 0) {
            buttonGroup.getButtons().first().setChecked(true);
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        this.onChangeListener = listener;
    }

    public void setValue(String value) {
        for (Button btn : buttonGroup.getButtons()) {
            if (value.equals(btn.getUserObject())) {
                btn.setChecked(true);
                return;
            }
        }
    }

    public String getValue() {
        Button checked = buttonGroup.getChecked();
        return checked != null ? (String) checked.getUserObject() : null;
    }
}
