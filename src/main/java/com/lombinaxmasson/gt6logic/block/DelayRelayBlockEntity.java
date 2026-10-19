package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.DelayRelay;
import com.lombinaxmasson.gt6logic.control.DelayRelay.Mode;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** One input, in front. The wait is saved so a reload does not start the second over. */
public final class DelayRelayBlockEntity extends SignalBlockEntity {
    private static final String MODE = "Mode";
    private static final String SECONDS = "Seconds";
    private static final String WAITED = "Waited";
    private static final String OUTPUT = "Output";

    private Mode mode = Mode.ON_DELAY;
    private int seconds = DelayRelay.SECONDS[0];
    private DelayRelay relay = new DelayRelay(mode, ticks());

    public DelayRelayBlockEntity(BlockPos pos, BlockState state) {
        super(DelayRelayBlock.TYPE.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public int seconds() {
        return seconds;
    }

    public int waited() {
        return relay.waited();
    }

    public boolean latched() {
        return relay.output();
    }

    public Component status() {
        String key = mode == Mode.ON_DELAY
                ? "message.gt6logic.delay_relay.on_delay"
                : "message.gt6logic.delay_relay.off_delay";
        return Component.translatable(key, seconds);
    }

    public void cycleSeconds() {
        seconds = DelayRelay.nextSeconds(seconds);
        relay = new DelayRelay(mode, ticks());
        setChanged();
    }

    public void cycleMode() {
        mode = mode.next();
        relay = new DelayRelay(mode, ticks());
        setChanged();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(DelayRelayBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(DelayRelayBlock.FACING);
        boolean input = SignalLevels.ceilingLevel(read(facing)) >= 1;
        boolean on = relay.update(input);
        FaceSignals.send(output(), on ? FaceSignals.ON : 0L, facing);
        BlockFeedback.setPowered(this, DelayRelayBlock.POWERED, on);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE, mode.name());
        tag.putInt(SECONDS, seconds);
        tag.putInt(WAITED, relay.waited());
        tag.putBoolean(OUTPUT, relay.output());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = Mode.byName(tag.getString(MODE));
        int loaded = tag.getInt(SECONDS);
        seconds = DelayRelay.allowedSeconds(loaded) ? loaded : DelayRelay.SECONDS[0];
        relay = new DelayRelay(mode, ticks());
        if (DelayRelay.allowedSeconds(loaded) && Mode.byName(tag.getString(MODE)).name().equals(tag.getString(MODE))) {
            relay.restore(tag.getInt(WAITED), tag.getBoolean(OUTPUT));
        }
    }

    private int ticks() {
        return seconds * DelayRelay.TICKS_PER_SECOND;
    }
}
