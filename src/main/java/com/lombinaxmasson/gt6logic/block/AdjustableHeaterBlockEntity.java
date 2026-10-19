package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class AdjustableHeaterBlockEntity extends ThermalBlockEntity {
    public AdjustableHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(AdjustableHeaterBlock.TYPE.get(), pos, state, false);
    }
}
