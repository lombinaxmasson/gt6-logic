package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ControlValveBlockEntity extends FlowBlockEntity {
    public ControlValveBlockEntity(BlockPos pos, BlockState state) {
        super(ControlValveBlock.TYPE.get(), pos, state, FlowKind.CONTROL);
    }
}
