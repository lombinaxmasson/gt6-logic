package com.lombinaxmasson.gt6logic.control;

import java.util.Locale;

/** A name painted on a button. It does not change the pulse. */
public enum ButtonLabel {
    RUN,
    STOP,
    FEED,
    DRAIN;

    public ButtonLabel next() {
        ButtonLabel[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static ButtonLabel byName(String name) {
        for (ButtonLabel label : values()) {
            if (label.name().equals(name)) {
                return label;
            }
        }
        return RUN;
    }

    public String messageKey() {
        return "message.gt6logic.button." + name().toLowerCase(Locale.ROOT);
    }
}
