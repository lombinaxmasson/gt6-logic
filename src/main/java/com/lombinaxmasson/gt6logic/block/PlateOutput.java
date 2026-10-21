package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.Direction;

/** Writes a plate's pending faces and the lamp that follows them. */
final class PlateOutput {
    private PlateOutput() {}

    /** On or off, sent only away from {@code facing}. {@code facing} points at the measured block. */
    static void away(SignalBlockEntity entity, boolean on, Direction facing) {
        FaceSignals.onlyToward(entity.output(), on ? FaceSignals.ON : 0L, facing.getOpposite());
        BlockFeedback.setPowered(entity, LogicPlateBlock.POWERED, on);
    }
}
