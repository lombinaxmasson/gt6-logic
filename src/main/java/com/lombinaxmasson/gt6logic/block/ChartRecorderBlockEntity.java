package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.control.ChartTrace;
import com.lombinaxmasson.gt6logic.control.SignalReadout;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The trace is the record. Clients redraw when a new sample lands, once a second. */
public final class ChartRecorderBlockEntity extends BlockEntity implements GtSignalReceiver, ServerTick {
    private static final String SAMPLES = "Samples";
    private static final String WAIT = "Wait";

    private final ChartTrace trace = new ChartTrace();

    public ChartRecorderBlockEntity(BlockPos pos, BlockState state) {
        super(ChartRecorderBlock.TYPE.get(), pos, state);
    }

    public int bars() {
        return trace.count();
    }

    public float bar(int oldestIndex) {
        return trace.fraction(oldestIndex);
    }

    public long latest() {
        return trace.latest();
    }

    public long lowest() {
        return trace.lowest();
    }

    public long highest() {
        return trace.highest();
    }

    public Component status() {
        return Component.translatable(
                "message.gt6logic.chart.status",
                SignalReadout.format(trace.latest()),
                SignalReadout.format(trace.lowest()),
                SignalReadout.format(trace.highest()));
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ChartRecorderBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(ReceiverPlate.FACING);
        long signal = SignalInputs.read(level, worldPosition.relative(facing), facing);
        if (!trace.tick(signal)) {
            return;
        }
        setChanged();
        BlockFeedback.syncClient(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        write(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        trace.restore(tag.getLongArray(SAMPLES), tag.getInt(WAIT));
    }

    private void write(CompoundTag tag) {
        tag.putLongArray(SAMPLES, trace.ordered());
        tag.putInt(WAIT, trace.untilNext());
    }
}
