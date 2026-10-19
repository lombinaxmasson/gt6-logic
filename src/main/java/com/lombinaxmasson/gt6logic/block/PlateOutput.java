package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.Direction;

/** Writes a plate's pending faces and the lamp that follows them. */
final class PlateOutput {
    private PlateOutput() {}

    static void digital(SignalBlockEntity entity, boolean on, Direction... quiet) {
        FaceSignals.send(entity.output(), on ? FaceSignals.ON : 0L, quiet);
        BlockFeedback.setPowered(entity, LogicPlateBlock.POWERED, on);
    }

    static void value(SignalBlockEntity entity, long signal, Direction... quiet) {
        FaceSignals.send(entity.output(), signal, quiet);
        BlockFeedback.setPowered(entity, LogicPlateBlock.POWERED, SignalLevels.ceilingLevel(signal) >= 1);
    }
}
