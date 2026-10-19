package com.lombinaxmasson.gt6logic.control;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

/**
 * The flow the second line should carry: reference times a ratio.
 * The ratio is a whole number or a unit fraction, so the result stays exact.
 */
public final class RatioControl {
    public record Ratio(int numerator, int denominator) {
        public Ratio {
            if (numerator < 1 || denominator < 1) {
                throw new IllegalArgumentException("ratio needs positive numbers");
            }
        }

        /** {@code 2} or {@code 1/2}. The magnifying glass reads this back. */
        public String text() {
            return denominator == 1 ? Integer.toString(numerator) : numerator + "/" + denominator;
        }
    }

    /** First entry is the default: the second line wants twice the reference. */
    public static final Ratio[] RATIOS = {
            new Ratio(2, 1),
            new Ratio(4, 1),
            new Ratio(1, 1),
            new Ratio(1, 2),
            new Ratio(1, 4)
    };

    private RatioControl() {}

    public static long desired(long reference, Ratio ratio) {
        if (ratio == null) {
            throw new IllegalArgumentException("ratio is missing");
        }
        long clamped = GtSignalCarrier.clamp(reference);
        return GtSignalCarrier.clamp(SignalLevels.multiplyDivide(clamped, ratio.numerator(), ratio.denominator()));
    }
}
