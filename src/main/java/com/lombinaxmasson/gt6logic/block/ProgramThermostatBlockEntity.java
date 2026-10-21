package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.HysteresisSwitch.Mode;
import com.lombinaxmasson.gt6logic.control.ProportionalControl;
import com.lombinaxmasson.gt6logic.control.TemperatureSchedule;
import com.lombinaxmasson.gt6logic.control.Temperatures;
import com.lombinaxmasson.gt6logic.heat.TemperatureInputs;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Left reads temperature. The front carries the opening. The mash curve sets the target. */
public final class ProgramThermostatBlockEntity extends SignalBlockEntity {
    private static final String MODE = "Mode";
    private static final String ELAPSED = "Elapsed";

    private Mode mode = Mode.HEAT;
    private int elapsed;

    public ProgramThermostatBlockEntity(BlockPos pos, BlockState state) {
        super(ProgramThermostatBlock.TYPE.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public int elapsed() {
        return elapsed;
    }

    public int setpointMilli() {
        return TemperatureSchedule.setpointMilli(elapsed);
    }

    public Component status() {
        String key = mode == Mode.HEAT ? "message.gt6logic.program.heat" : "message.gt6logic.program.cool";
        int step = TemperatureSchedule.segment(elapsed) + 1;
        return Component.translatable(key, step, Temperatures.format(setpointMilli()));
    }

    public void adjust(Player player) {
        if (player.isShiftKeyDown()) {
            elapsed = 0;
        } else {
            mode = mode == Mode.HEAT ? Mode.COOL : Mode.HEAT;
        }
        setChanged();
    }

    @Override
    protected void onTick() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(AnalogPlate.SUPPORT)) {
            return;
        }
        GatePorts.Sides ports = AnalogPlate.ports(state);
        long measured = Temperatures.toSignal(TemperatureInputs.milli(level, worldPosition, ports.left()));
        long target = Temperatures.toSignal(setpointMilli());
        AnalogPlate.publish(this, ProportionalControl.opening(mode, measured, target, 1), ports.front());
        int limit = TemperatureSchedule.SEGMENT_TICKS * TemperatureSchedule.CELSIUS.length;
        if (elapsed < limit) {
            elapsed++;
            if (elapsed % 20 == 0) {
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(MODE, mode.name());
        tag.putInt(ELAPSED, elapsed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = "COOL".equals(tag.getString(MODE)) ? Mode.COOL : Mode.HEAT;
        elapsed = Math.max(0, tag.getInt(ELAPSED));
    }
}
