package com.lombinaxmasson.gt6logic.signal;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Reads one neighbor. Call this from the block's own tick, never from {@code gtSignal}:
 * a signal wire reports 0 while it is sampling, so a read started from inside {@code gtSignal}
 * sees a half-solved network.
 */
public final class SignalInputs {
    private SignalInputs() {}

    /**
     * @param towardNeighbor direction from this block toward {@code neighbor},
     *                       the same facing {@link Level#getSignal} expects
     */
    public static long read(Level level, BlockPos neighbor, Direction towardNeighbor) {
        if (!level.hasChunk(SectionPos.blockToSectionCoord(neighbor.getX()), SectionPos.blockToSectionCoord(neighbor.getZ()))) {
            return 0L;
        }
        BlockEntity entity = level.getBlockEntity(neighbor);
        if (entity instanceof GtSignalCarrier carrier) {
            return GtSignalCarrier.clamp(carrier.signal());
        }
        return SignalLevels.fromLevel(level.getSignal(neighbor, towardNeighbor));
    }
}
