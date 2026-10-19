package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.MachineBands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Fill of the tank across the face this plate is mounted toward. */
public final class FloatSwitchBlockEntity extends HysteresisPlate {
    public FloatSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(FloatSwitchBlock.TYPE.get(), pos, state, MachineBands.FLOAT, false);
    }

    @Override
    protected long readSample(Direction facing) {
        return FluidLevel.basisPoints(level, worldPosition, facing);
    }
}
