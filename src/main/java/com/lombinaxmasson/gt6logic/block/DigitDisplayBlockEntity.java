package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.control.DisplayReading;
import com.lombinaxmasson.gt6logic.control.DisplayReading.Unit;
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

/** The face text is the selected unit of the facing neighbor. Clients get a new line when it changes. */
public final class DigitDisplayBlockEntity extends BlockEntity implements GtSignalReceiver, ServerTick {
    private static final String READING = "Reading";
    private static final String UNIT = "Unit";

    private long reading;
    private Unit unit = Unit.SIGNAL;

    public DigitDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(DigitDisplayBlock.TYPE.get(), pos, state);
    }

    public long reading() {
        return reading;
    }

    public Unit unit() {
        return unit;
    }

    public String face() {
        return DisplayReading.face(unit, reading);
    }

    public Component status() {
        return Component.translatable(unit.messageKey(), DisplayReading.argument(unit, reading));
    }

    public void cycle() {
        unit = unit.next();
        setChanged();
        BlockFeedback.syncClient(this);
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(DigitDisplayBlock.BLOCK.get())) {
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
        reading = tag.getLong(READING);
        unit = Unit.byName(tag.getString(UNIT));
    }

    private void write(CompoundTag tag) {
        tag.putLong(READING, reading);
        tag.putString(UNIT, unit.name());
    }
}
