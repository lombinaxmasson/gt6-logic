package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class ElectricValveBlockEntity extends FlowBlockEntity {
    public ElectricValveBlockEntity(BlockPos pos, BlockState state) {
        super(ElectricValveBlock.TYPE.get(), pos, state, FlowKind.VALVE);
    }
}
