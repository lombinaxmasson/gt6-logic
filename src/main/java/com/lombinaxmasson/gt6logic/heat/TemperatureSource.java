package com.lombinaxmasson.gt6logic.heat;

import java.util.OptionalLong;

import net.minecraft.core.Direction;

/**
 * A face that reports temperature in milli-celsius.
 * Empty means this face is not a probe, and the reader may fall back to a signal wire.
 */
public interface TemperatureSource {
    OptionalLong milliCelsius(Direction touchedFace);
}
