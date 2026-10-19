package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.MachineBands;
import com.lombinaxmasson.gt6logic.heat.TemperatureInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Milli-celsius from the faced block, or from a temperature signal on that face. */
public final class ThermostatBlockEntity extends HysteresisPlate {
    public ThermostatBlockEntity(BlockPos pos, BlockState state) {
        super(ThermostatBlock.TYPE.get(), pos, state, MachineBands.THERMOSTAT, true);
    }

    @Override
    protected long readSample(Direction facing) {
        return TemperatureInputs.milli(level, worldPosition, facing);
    }
}
