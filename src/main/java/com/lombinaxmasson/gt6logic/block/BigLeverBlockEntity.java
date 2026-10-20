package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The lever position is the block state. This tick only copies it onto the six faces. */
public final class BigLeverBlockEntity extends SignalBlockEntity {
    public BigLeverBlockEntity(BlockPos pos, BlockState state) {
        super(BigLeverBlock.TYPE.get(), pos, state);
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(BigLeverBlock.BLOCK.get())) {
            return;
        }
        boolean on = state.getValue(BigLeverBlock.POWERED);
        FaceSignals.send(output(), on ? FaceSignals.ON : 0L);
    }
}
