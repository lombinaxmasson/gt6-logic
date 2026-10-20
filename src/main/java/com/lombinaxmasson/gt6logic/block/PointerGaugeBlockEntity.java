package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.control.PointerAngle;
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

/** Stores the facing signal so the needle can be drawn without reading the world on the client. */
public final class PointerGaugeBlockEntity extends BlockEntity implements GtSignalReceiver, ServerTick {
    private static final String READING = "Reading";

    private long reading;

    public PointerGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(PointerGaugeBlock.TYPE.get(), pos, state);
    }

    public long reading() {
        return reading;
    }

    public float angle() {
        return PointerAngle.radians(reading);
    }

    public Component status() {
        return Component.translatable("message.gt6logic.pointer.status", SignalReadout.format(reading));
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(PointerGaugeBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(ReceiverPlate.FACING);
        long next = SignalInputs.read(level, worldPosition.relative(facing), facing);
        if (next == reading) {
            return;
        }
        reading = next;
        setChanged();
        BlockFeedback.syncClient(this);
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
