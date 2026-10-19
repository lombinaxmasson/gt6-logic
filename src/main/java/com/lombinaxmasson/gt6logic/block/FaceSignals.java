package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.signal.PublishedSignal;

import net.minecraft.core.Direction;

/**
 * Writes a block's pending outputs. The neighbor standing in {@code towardNeighbor}
 * reads the opposite side, the same side a signal wire asks for.
 */
public final class FaceSignals {
    public static final long ON = 15L * GtSignalCarrier.UNIT;

    private FaceSignals() {}

    /** Side index read by the neighbor that stands in {@code towardNeighbor}. */
    public static int sideReadBy(Direction towardNeighbor) {
        return towardNeighbor.getOpposite().ordinal();
    }

    /** Publishes {@code value} on every face except the faces those input neighbors read. */
    public static void send(PublishedSignal output, long value, Direction... inputs) {
        boolean[] quiet = new boolean[PublishedSignal.FACES];
        for (Direction input : inputs) {
            quiet[sideReadBy(input)] = true;
        }
        long clamped = GtSignalCarrier.clamp(value);
        for (int side = 0; side < PublishedSignal.FACES; side++) {
            output.set(side, quiet[side] ? 0L : clamped);
        }
    }
}
