package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.PulseCounter;
import com.lombinaxmasson.gt6logic.control.PulseCounter.Mode;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** Front counts pulses. Back resets. The other four faces carry the latched output. */
public final class CounterBlockEntity extends SignalBlockEntity {
    private static final String MODE = "Mode";
    private static final String TARGET = "Target";
    private static final String COUNT = "Count";
    private static final String OUTPUT = "Output";
    private static final String PULSE = "PulseWas";
    private static final String RESET = "ResetWas";

    private Mode mode = Mode.OPEN;
    private int target = PulseCounter.DEFAULT_TARGET;
    private PulseCounter counter = new PulseCounter(mode, target);
    private int sentCount = -1;

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(CounterBlock.TYPE.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public int target() {
        return target;
    }

    public int count() {
        return counter.count();
    }

    public boolean latched() {
        return counter.output();
    }

    /** Three quarters of a turn at the target, so a full count does not land on the empty mark. */
    public float needleAngle() {
        float fraction = counter.count() / (float) Math.max(1, target);
        return fraction * ((float) Math.PI * 1.5F);
    }

    public Component status() {
        String key = mode == Mode.OPEN
                ? "message.gt6logic.counter.open"
                : "message.gt6logic.counter.shut";
        return Component.translatable(key, target, counter.count());
    }

    public void cycleTarget() {
        target = PulseCounter.nextTarget(target);
        counter = new PulseCounter(mode, target);
        setChanged();
        publishDial();
    }

    public void cycleMode() {
        mode = mode.next();
        counter = new PulseCounter(mode, target);
        setChanged();
        publishDial();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(CounterBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(CounterBlock.FACING);
        boolean pulse = SignalLevels.ceilingLevel(read(facing)) >= 1;
        boolean reset = SignalLevels.ceilingLevel(read(facing.getOpposite())) >= 1;
        boolean on = counter.update(pulse, reset);
        FaceSignals.send(output(), on ? FaceSignals.ON : 0L, facing, facing.getOpposite());
        BlockFeedback.setPowered(this, CounterBlock.POWERED, on);
        if (counter.count() != sentCount) {
            publishDial();
        }
    }

    private void publishDial() {
        sentCount = counter.count();
        BlockFeedback.syncClient(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE, mode.name());
        tag.putInt(TARGET, target);
        tag.putInt(COUNT, counter.count());
        tag.putBoolean(OUTPUT, counter.output());
        tag.putBoolean(PULSE, counter.pulseWas());
        tag.putBoolean(RESET, counter.resetWas());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String modeName = tag.getString(MODE);
        int loadedTarget = tag.getInt(TARGET);
        boolean known = Mode.byName(modeName).name().equals(modeName) && PulseCounter.allowedTarget(loadedTarget);
        mode = known ? Mode.byName(modeName) : Mode.OPEN;
        target = known ? loadedTarget : PulseCounter.DEFAULT_TARGET;
        counter = new PulseCounter(mode, target);
        if (known) {
            counter.restore(tag.getInt(COUNT), tag.getBoolean(OUTPUT), tag.getBoolean(PULSE), tag.getBoolean(RESET));
        }
    }
}
