package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AdjustableCoolerBlockEntity extends ThermalBlockEntity {
    public AdjustableCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(AdjustableCoolerBlock.TYPE.get(), pos, state, true);
    }
}
