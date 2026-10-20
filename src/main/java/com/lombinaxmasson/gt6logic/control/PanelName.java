package com.lombinaxmasson.gt6logic.control;

import java.util.Locale;

/** The word on a console plate. The lamp still follows the signal. */
public enum PanelName {
    POWER,
    HEAT,
    LEVEL,
    FLOW,
    ALARM;

    public PanelName next() {
        PanelName[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static PanelName byName(String name) {
        for (PanelName panelName : values()) {
            if (panelName.name().equals(name)) {
                return panelName;
            }
        }
        return POWER;
    }

    public String messageKey() {
        return "message.gt6logic.panel." + name().toLowerCase(Locale.ROOT);
    }
}
