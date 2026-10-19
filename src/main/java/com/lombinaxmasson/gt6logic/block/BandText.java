package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.Band;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.control.TenThousandths;

import net.minecraft.network.chat.Component;

/** Sentences for a hysteresis band. Percent bands and degree bands use different units. */
final class BandText {
    private BandText() {}

    static Component percent(Band band) {
        if (band.mode() == Mode.HEAT) {
            return Component.translatable("message.gt6logic.band.heat",
                    TenThousandths.percent((int) band.lower()), TenThousandths.percent((int) band.upper()));
        }
        return Component.translatable("message.gt6logic.band.cool",
                TenThousandths.percent((int) band.upper()), TenThousandths.percent((int) band.lower()));
    }

    static Component celsius(Band band) {
        if (band.mode() == Mode.HEAT) {
            return Component.translatable("message.gt6logic.temperature.heat", band.lower() / 1000L, band.upper() / 1000L);
        }
        return Component.translatable("message.gt6logic.temperature.cool", band.upper() / 1000L, band.lower() / 1000L);
    }
}
