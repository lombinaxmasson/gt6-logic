package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.block.AlarmBellBlock.Timbre;
import com.lombinaxmasson.gt6logic.control.AlarmBell;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Listens to the neighbor in front of the bell. The ring count is only there so a game test
 * can see the cadence; it is not saved and the player never sees it.
 */
public final class AlarmBellBlockEntity extends BlockEntity implements GtSignalReceiver {
    private static final String THRESHOLD = "Threshold";
    private static final String TIMBRE = "Timbre";

    private int threshold;
    private Timbre timbre = Timbre.BELL;
    private int cooldown;
    private int played;

    public AlarmBellBlockEntity(BlockPos pos, BlockState state) {
        super(AlarmBellBlock.TYPE.get(), pos, state);
    }

    public int threshold() {
        return threshold;
    }

    public Timbre timbre() {
        return timbre;
    }

    public int ringsPlayed() {
        return played;
    }

    public Component status() {
        return Component.translatable("message.gt6logic.alarm_bell.status", threshold, timbre.label());
    }

    public void cycleThreshold() {
        threshold = AlarmBell.nextLevel(threshold);
        setChanged();
    }

    public void cycleTimbre() {
        timbre = timbre.next();
        setChanged();
    }

    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(AlarmBellBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(AlarmBellBlock.FACING);
        long signal = SignalInputs.read(level, worldPosition.relative(facing), facing);
        boolean ringing = AlarmBell.shouldRing(signal, threshold);
        if (state.getValue(AlarmBellBlock.RINGING) != ringing) {
            state = state.setValue(AlarmBellBlock.RINGING, ringing);
            level.setBlock(worldPosition, state, Block.UPDATE_CLIENTS);
        }
        if (!ringing) {
            cooldown = 0;
            return;
        }
        if (cooldown == 0) {
            level.playSound(null, worldPosition, timbre.soundEvent(), SoundSource.BLOCKS, 1.0F, 1.0F);
            played++;
            cooldown = AlarmBell.PERIOD_TICKS;
        }
        cooldown--;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(THRESHOLD, threshold);
        tag.putString(TIMBRE, timbre.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int loaded = tag.getInt(THRESHOLD);
        if (loaded < AlarmBell.MIN_LEVEL) {
            loaded = AlarmBell.MIN_LEVEL;
        } else if (loaded > AlarmBell.MAX_LEVEL) {
            loaded = AlarmBell.MAX_LEVEL;
        }
        threshold = loaded;
        timbre = Timbre.byName(tag.getString(TIMBRE));
    }
}
