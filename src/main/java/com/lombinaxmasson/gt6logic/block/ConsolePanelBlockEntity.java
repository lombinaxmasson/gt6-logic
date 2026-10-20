package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.control.PanelName;
import com.lombinaxmasson.gt6logic.signal.SignalInputs;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The name is only a label. The lamp follows the facing neighbor. */
public final class ConsolePanelBlockEntity extends BlockEntity implements GtSignalReceiver, ServerTick {
    private static final String NAME = "Name";

    private PanelName name = PanelName.POWER;

    public ConsolePanelBlockEntity(BlockPos pos, BlockState state) {
        super(ConsolePanelBlock.TYPE.get(), pos, state);
    }

    public PanelName name() {
        return name;
    }

    public void cycle() {
        name = name.next();
        setChanged();
    }

    public Component status() {
        boolean on = getBlockState().getValue(ConsolePanelBlock.POWERED);
        String key = on ? "message.gt6logic.panel.on" : "message.gt6logic.panel.off";
        return Component.translatable(key, Component.translatable(name.messageKey()));
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide || isRemoved()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ConsolePanelBlock.BLOCK.get())) {
            return;
        }
        Direction facing = state.getValue(ReceiverPlate.FACING);
        long signal = SignalInputs.read(level, worldPosition.relative(facing), facing);
        boolean on = SignalLevels.ceilingLevel(signal) >= 1;
        BlockFeedback.setPowered(this, ConsolePanelBlock.POWERED, on);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(NAME, name.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        name = PanelName.byName(tag.getString(NAME));
    }
}
