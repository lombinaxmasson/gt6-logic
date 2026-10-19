package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The dial text is the four-decimal readout. Clients receive it when the value changes. */
public final class SignalMeterBlockEntity extends BlockEntity implements GtSignalReceiver {
    private static final String READING = "Reading";

    private long reading;

    public SignalMeterBlockEntity(BlockPos pos, BlockState state) {
        super(SignalMeterBlock.TYPE.get(), pos, state);
    }

    public long reading() {
        return reading;
    }

    public String formatted() {
        return SignalReadout.format(reading);
    }

    public Component status() {
        return Component.translatable("message.gt6logic.signal_meter.status", formatted());
    }

    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(SignalMeterBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(SignalMeterBlock.FACING);
        long next = SignalInputs.read(level, worldPosition.relative(facing), facing);
        if (next == reading) {
            return;
        }
        reading = next;
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(READING, reading);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(READING, reading);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reading = tag.getLong(READING);
    }
}
