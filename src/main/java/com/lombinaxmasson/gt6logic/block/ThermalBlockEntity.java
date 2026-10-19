package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.PowerDraw;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Listens to the face it points at. Full signal draws one LV packet per tick
 * and offers that much heat or cold for a neighbor to pull.
 */
public class ThermalBlockEntity extends PoweredEntity {
    static final long BUFFER = 1024;

    private final boolean cold;
    private long offer;
    private long spent;
    private boolean starved;

    protected ThermalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean cold) {
        super(type, pos, state, BUFFER);
        this.cold = cold;
    }

    public long spent() {
        return spent;
    }

    public boolean starved() {
        return starved;
    }

    public Component status() {
        if (starved) {
            return Component.translatable("message.gt6logic.valve.unpowered");
        }
        return Component.translatable("message.gt6logic.heater.draw", spent);
    }

    @Override
    public boolean emitsHeat() {
        return !cold;
    }

    @Override
    public boolean emitsCold() {
        return cold;
    }

    @Override
    public long heat() {
        return cold ? 0L : offer;
    }

    @Override
    public long cold() {
        return cold ? offer : 0L;
    }

    @Override
    public void setHeat(long heat) {
        if (!cold) {
            offer = Math.max(0L, heat);
        }
    }

    @Override
    public void setCold(long coldAmount) {
        if (cold) {
            offer = Math.max(0L, coldAmount);
        }
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
        Direction facing = state.getValue(ActuatorBlock.FACING);
        long signal = SignalInputs.read(level, worldPosition.relative(facing), facing);
        long wanted = PowerDraw.eu(signal, PowerDraw.LV_PACKET);
        spent = eu().consumeUpTo(wanted);
        starved = wanted > 0L && spent == 0L;
        if (cold) {
            setCold(spent);
        } else {
            setHeat(spent);
        }
        if (spent > 0L) {
            setChanged();
        }
        BlockFeedback.setPowered(this, ActuatorBlock.POWERED, spent > 0L);
    }
}
