package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.control.BeaconBand;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The brightest neighbor picks the color. Nothing here is synced apart from that block state. */
public final class WarningBeaconBlockEntity extends BlockEntity implements GtSignalReceiver, ServerTick {
    public WarningBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(WarningBeaconBlock.TYPE.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        long strongest = 0L;
        for (Direction face : Direction.values()) {
            strongest = Math.max(strongest, SignalInputs.read(level, worldPosition.relative(face), face));
        }
        WarningBeaconBlock.Color color = WarningBeaconBlock.Color.of(BeaconBand.of(strongest));
        BlockState state = getBlockState();
        if (!state.is(WarningBeaconBlock.BLOCK.get()) || state.getValue(WarningBeaconBlock.BAND) == color) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(WarningBeaconBlock.BAND, color), Block.UPDATE_CLIENTS);
    }
}
