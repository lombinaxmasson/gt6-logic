package com.lombinaxmasson.gt6logic.heat;

import java.util.OptionalLong;

import com.lombinaxmasson.gt6logic.control.Temperatures;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Milli-celsius from the neighbor across {@code towardNeighbor}. An unloaded chunk reads as 0 °C. */
public final class TemperatureInputs {
    private TemperatureInputs() {}

    public static long milli(Level level, BlockPos pos, Direction towardNeighbor) {
        BlockPos neighbor = pos.relative(towardNeighbor);
        if (!level.hasChunk(SectionPos.blockToSectionCoord(neighbor.getX()), SectionPos.blockToSectionCoord(neighbor.getZ()))) {
            return 0L;
        }
        BlockEntity entity = level.getBlockEntity(neighbor);
        if (entity instanceof TemperatureSource source) {
            OptionalLong reading = source.milliCelsius(towardNeighbor.getOpposite());
            if (reading.isPresent()) {
                return Math.max(0L, reading.getAsLong());
            }
        }
        return Temperatures.toMilli(SignalInputs.read(level, neighbor, towardNeighbor));
    }
}
