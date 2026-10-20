package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.ButtonLabel;
import com.lombinaxmasson.gt6logic.control.PulseHold;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** The label is stored. The pulse is what the neighbors see. */
public final class LabeledButtonBlockEntity extends SignalBlockEntity {
    private static final String LABEL = "Label";
    private static final String LEFT = "Left";

    private ButtonLabel label = ButtonLabel.RUN;
    private final PulseHold pulse = new PulseHold();

    public LabeledButtonBlockEntity(BlockPos pos, BlockState state) {
        super(LabeledButtonBlock.TYPE.get(), pos, state);
    }

    public ButtonLabel label() {
        return label;
    }

    public int remaining() {
        return pulse.remaining();
    }

    public void cycleLabel() {
        label = label.next();
        setChanged();
    }

    public void press() {
        pulse.press();
        BlockFeedback.setPowered(this, LabeledButtonBlock.POWERED, true);
        setChanged();
    }

    public Component status() {
        String key = pulse.on() ? "message.gt6logic.button.holding" : "message.gt6logic.button.idle";
        return Component.translatable(key, Component.translatable(label.messageKey()));
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(LabeledButtonBlock.BLOCK.get())) {
            return;
        }
        boolean on = pulse.on();
        FaceSignals.send(output(), on ? FaceSignals.ON : 0L);
        BlockFeedback.setPowered(this, LabeledButtonBlock.POWERED, on);
        if (on) {
            pulse.tick();
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(LABEL, label.name());
        tag.putInt(LEFT, pulse.remaining());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        label = ButtonLabel.byName(tag.getString(LABEL));
        pulse.restore(tag.getInt(LEFT));
    }
}
