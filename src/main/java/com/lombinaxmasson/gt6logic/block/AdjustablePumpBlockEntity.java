package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AdjustablePumpBlockEntity extends FlowBlockEntity {
    public AdjustablePumpBlockEntity(BlockPos pos, BlockState state) {
        super(AdjustablePumpBlock.TYPE.get(), pos, state, FlowKind.PUMP);
    }
}
