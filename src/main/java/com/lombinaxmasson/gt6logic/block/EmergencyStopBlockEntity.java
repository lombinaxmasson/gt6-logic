package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.StopLatch;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** The latch is the memory. The powered lamp follows it, including after a reload. */
public final class EmergencyStopBlockEntity extends SignalBlockEntity {
    private static final String TRIPPED = "Tripped";

    private final StopLatch latch = new StopLatch();

    public EmergencyStopBlockEntity(BlockPos pos, BlockState state) {
        super(EmergencyStopBlock.TYPE.get(), pos, state);
    }

    public boolean tripped() {
        return latch.tripped();
    }

    public void trip() {
        latch.trip();
        BlockFeedback.setPowered(this, EmergencyStopBlock.POWERED, true);
        setChanged();
    }

    public void reset() {
        latch.reset();
        BlockFeedback.setPowered(this, EmergencyStopBlock.POWERED, false);
        setChanged();
    }

    public Component status() {
        return Component.translatable(latch.tripped()
                ? "message.gt6logic.stop.tripped"
                : "message.gt6logic.stop.ready");
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(EmergencyStopBlock.BLOCK.get())) {
            return;
        }
        FaceSignals.send(output(), latch.tripped() ? FaceSignals.ON : 0L);
        BlockFeedback.setPowered(this, EmergencyStopBlock.POWERED, latch.tripped());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TRIPPED, latch.tripped());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        latch.restore(tag.getBoolean(TRIPPED));
    }
}
