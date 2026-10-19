package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.KitchenTimer;
import com.lombinaxmasson.gt6logic.control.MachineBands;
import com.lombinaxmasson.gt6logic.control.Presets;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** On for a few seconds, then off. Every face carries the same on or off. */
public final class KitchenTimerBlockEntity extends SignalBlockEntity {
    private static final String ON = "OnSeconds";
    private static final String OFF = "OffSeconds";
    private static final String PHASE = "Phase";
    private static final int LIMIT = 3600;

    private int onSeconds = MachineBands.KITCHEN[0][0];
    private int offSeconds = MachineBands.KITCHEN[0][1];
    private KitchenTimer timer = clock(0);

    public KitchenTimerBlockEntity(BlockPos pos, BlockState state) {
        super(KitchenTimerBlock.TYPE.get(), pos, state);
    }

    public int onSeconds() {
        return onSeconds;
    }

    public int offSeconds() {
        return offSeconds;
    }

    public int phase() {
        return timer.phase();
    }

    public Component status() {
        return Component.translatable("message.gt6logic.kitchen_timer.status", onSeconds, offSeconds);
    }

    public void adjust(Player player) {
        int index = -1;
        for (int i = 0; i < MachineBands.KITCHEN.length; i++) {
            if (MachineBands.KITCHEN[i][0] == onSeconds && MachineBands.KITCHEN[i][1] == offSeconds) {
                index = i;
            }
        }
        int next = Presets.next(index, MachineBands.KITCHEN.length);
        onSeconds = MachineBands.KITCHEN[next][0];
        offSeconds = MachineBands.KITCHEN[next][1];
        timer = clock(0);
        setChanged();
    }

    @Override
    protected void onTick() {
        boolean on = timer.output();
        timer.tick();
        PlateOutput.digital(this, on);
        if (timer.phase() % KitchenTimer.TICKS_PER_SECOND == 0) {
            setChanged();
        }
    }

    private KitchenTimer clock(int phase) {
        int period = (onSeconds + offSeconds) * KitchenTimer.TICKS_PER_SECOND;
        int duration = onSeconds * KitchenTimer.TICKS_PER_SECOND;
        return KitchenTimer.resumed(period, duration, phase);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(ON, onSeconds);
        tag.putInt(OFF, offSeconds);
        tag.putInt(PHASE, timer.phase());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int on = tag.getInt(ON);
        int off = tag.getInt(OFF);
        if (on < 1 || off < 1 || on > LIMIT || off > LIMIT) {
            on = MachineBands.KITCHEN[0][0];
            off = MachineBands.KITCHEN[0][1];
        }
        onSeconds = on;
        offSeconds = off;
        timer = clock(tag.getInt(PHASE));
    }
}
