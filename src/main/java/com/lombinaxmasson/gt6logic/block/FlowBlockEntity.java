package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.FlowRate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Inlet is the face opposite facing. Outlet is facing.
 * The signal is only the left side of the outlet, so one wire is the input.
 */
public class FlowBlockEntity extends PoweredEntity {
    static final long BUFFER = 256;

    private final FlowKind kind;
    private int lastRate;
    private boolean unpowered;

    protected FlowBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, FlowKind kind) {
        super(type, pos, state, BUFFER);
        this.kind = kind;
    }

    public int lastRate() {
        return lastRate;
    }

    public boolean unpowered() {
        return unpowered;
    }

    public Component status() {
        if (unpowered) {
            return Component.translatable("message.gt6logic.valve.unpowered");
        }
        if (lastRate <= 0) {
            return Component.translatable("message.gt6logic.valve.shut");
        }
        return Component.translatable("message.gt6logic.valve.open", lastRate);
    }

    @Override
    public boolean acceptsEu() {
        return kind.needsPower;
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(ActuatorBlock.FACING)) {
            return;
        }
        Direction outlet = state.getValue(ActuatorBlock.FACING);
        Direction input = NeighborSignals.leftOf(outlet);
        long signal = NeighborSignals.strongest(level, worldPosition, input);
        int wanted = FlowRate.millibuckets(signal, kind.fullRate, kind.proportional);
        if (wanted > 0 && kind.needsPower && eu().consumeUpTo(1L) < 1L) {
            lastRate = 0;
            unpowered = true;
            BlockFeedback.setPowered(this, ActuatorBlock.POWERED, false);
            return;
        }
        if (wanted > 0 && kind.needsPower) {
            setChanged();
        }
        unpowered = false;
        lastRate = wanted;
        if (wanted > 0) {
            FluidMove.transfer(level, worldPosition, outlet.getOpposite(), outlet, wanted);
        }
        BlockFeedback.setPowered(this, ActuatorBlock.POWERED, wanted > 0);
    }
}
