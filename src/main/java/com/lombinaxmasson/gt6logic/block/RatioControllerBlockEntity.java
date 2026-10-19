package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.Presets;
import com.lombinaxmasson.gt6logic.control.RatioControl;
import com.lombinaxmasson.gt6logic.control.RatioControl.Ratio;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Front is the reference. The result is what the second line should carry. */
public final class RatioControllerBlockEntity extends SignalBlockEntity {
    private static final String PRESET = "Preset";

    private int preset;

    public RatioControllerBlockEntity(BlockPos pos, BlockState state) {
        super(RatioControllerBlock.TYPE.get(), pos, state);
    }

    public Ratio ratio() {
        return RatioControl.RATIOS[preset];
    }

    public Component status() {
        return Component.translatable("message.gt6logic.ratio.status", ratio().text());
    }

    public void adjust(Player player) {
        preset = Presets.next(preset, RatioControl.RATIOS.length);
        setChanged();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.hasProperty(LogicPlateBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(LogicPlateBlock.FACING);
        long value = RatioControl.desired(read(facing), ratio());
        PlateOutput.value(this, value, facing, facing.getOpposite());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(PRESET, preset);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int saved = tag.getInt(PRESET);
        preset = saved >= 0 && saved < RatioControl.RATIOS.length ? saved : 0;
    }
}
