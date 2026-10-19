package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.MachineBands;
import com.lombinaxmasson.gt6logic.control.TenThousandths;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** The faced neighbor's signal is a fraction of full. {@code 15 * UNIT} is 100%. */
public final class BatteryStewardBlockEntity extends HysteresisPlate {
    public BatteryStewardBlockEntity(BlockPos pos, BlockState state) {
        super(BatteryStewardBlock.TYPE.get(), pos, state, MachineBands.STEWARD, false);
    }

    @Override
    protected long readSample(Direction facing) {
        return TenThousandths.fromSignal(read(facing));
    }
}
