package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Strongest signal among a chosen set of faces. Call from the block's own tick. */
public final class NeighborSignals {
    private NeighborSignals() {}

    public static long strongest(Level level, BlockPos pos, Direction... faces) {
        long best = 0L;
        for (Direction face : faces) {
            best = Math.max(best, SignalInputs.read(level, pos.relative(face), face));
        }
        return best;
    }

    /**
     * Left side when looking along {@code facing}.
     * Up and down have no left; those use north so the read still has one face.
     */
    public static Direction leftOf(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return Direction.NORTH;
        }
        return facing.getCounterClockWise();
    }
}
