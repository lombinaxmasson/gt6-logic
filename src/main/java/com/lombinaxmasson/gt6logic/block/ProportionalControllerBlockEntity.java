package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.control.ProportionalControl;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Left is the measurement, right is the setpoint. The front carries the opening. Cool is the default. */
public final class ProportionalControllerBlockEntity extends SignalBlockEntity {
    private static final String MODE = "Mode";
    private static final String GAIN = "Gain";

    private Mode mode = Mode.COOL;
    private int gain = ProportionalControl.GAINS[0];

    public ProportionalControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ProportionalControllerBlock.TYPE.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public int gain() {
        return gain;
    }

    public Component status() {
        String key = mode == Mode.COOL ? "message.gt6logic.proportional.cool" : "message.gt6logic.proportional.heat";
        return Component.translatable(key, gain);
    }

    public void adjust(Player player) {
        if (player.isShiftKeyDown()) {
            mode = mode == Mode.COOL ? Mode.HEAT : Mode.COOL;
        } else {
            gain = ProportionalControl.nextGain(gain);
        }
        setChanged();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.hasProperty(AnalogPlate.SUPPORT)) {
            return;
        }
        GatePorts.Sides ports = AnalogPlate.ports(state);
        long opening = ProportionalControl.opening(mode, read(ports.left()), read(ports.right()), gain);
        AnalogPlate.publish(this, opening, ports.front());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE, mode.name());
        tag.putInt(GAIN, gain);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = "HEAT".equals(tag.getString(MODE)) ? Mode.HEAT : Mode.COOL;
        int saved = tag.getInt(GAIN);
        gain = ProportionalControl.allowed(saved) ? saved : ProportionalControl.GAINS[0];
    }
}
