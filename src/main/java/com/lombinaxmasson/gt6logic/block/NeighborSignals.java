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

    /** The four faces that are not on {@code axis}. */
    public static Direction[] around(Direction axis) {
        Direction[] faces = new Direction[4];
        int count = 0;
        for (Direction face : Direction.values()) {
            if (face.getAxis() != axis.getAxis()) {
                faces[count++] = face;
            }
        }
        return faces;
    }
}
