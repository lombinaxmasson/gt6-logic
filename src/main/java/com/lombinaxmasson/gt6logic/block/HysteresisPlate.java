package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.Band;
import com.lombinaxmasson.gt6logic.control.HysteresisSwitch;
import com.lombinaxmasson.gt6logic.control.Presets;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Faces the thing it measures. Only the opposite face is on or off. Changing preset keeps the remembered bit. */
public abstract class HysteresisPlate extends SignalBlockEntity {
    private static final String PRESET = "Preset";
    private static final String ON = "On";

    private final Band[] bands;
    private final boolean celsius;
    private int preset;
    private HysteresisSwitch hysteresis;

    HysteresisPlate(BlockEntityType<?> type, BlockPos pos, BlockState state, Band[] bands, boolean celsius) {
        super(type, pos, state);
        this.bands = bands;
        this.celsius = celsius;
        this.hysteresis = bands[0].open(false);
    }

    public Band band() {
        return bands[preset];
    }

    public boolean latched() {
        return hysteresis.output();
    }

    public Component status() {
        return celsius ? BandText.celsius(band()) : BandText.percent(band());
    }

    public void adjust(Player player) {
        boolean was = hysteresis.output();
        preset = Presets.next(preset, bands.length);
        hysteresis = bands[preset].open(was);
        setChanged();
    }

    protected abstract long readSample(Direction facing);

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (level == null || !state.hasProperty(LogicPlateBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        boolean before = hysteresis.output();
        boolean now = hysteresis.update(readSample(facing));
        if (before != now) {
            setChanged();
        }
        PlateOutput.away(this, now, facing);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(PRESET, preset);
        tag.putBoolean(ON, hysteresis.output());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int saved = tag.getInt(PRESET);
        preset = saved >= 0 && saved < bands.length ? saved : 0;
        hysteresis = bands[preset].open(tag.getBoolean(ON));
    }
}
