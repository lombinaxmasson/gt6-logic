package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
import com.lombinaxmasson.gt6logic.control.TenThousandths;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** Fill of the container across the mounting face, scaled across the whole signal range. */
public final class LevelGaugeBlockEntity extends SignalBlockEntity {
    private int part;

    public LevelGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(LevelGaugeBlock.TYPE.get(), pos, state);
    }

    public int part() {
        return part;
    }

    public Component status() {
        return Component.translatable("message.gt6logic.level_gauge.status", TenThousandths.percent(part));
    }

    @Override
    protected void onTick() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(LogicPlateBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        part = FluidLevel.basisPoints(level, worldPosition, facing);
        long signal = GtSignalCarrier.clamp(TenThousandths.toSignal(part));
        FaceSignals.onlyToward(output(), signal, facing.getOpposite());
        BlockFeedback.setPowered(this, LogicPlateBlock.POWERED, SignalLevels.ceilingLevel(signal) >= 1);
    }
}
