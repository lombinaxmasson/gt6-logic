package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalCarrier;
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

/** The selected face is the only input. Every other face repeats that signal, fraction included. */
public final class SelectorSwitchBlockEntity extends SignalBlockEntity {
    private static final String INPUT = "Input";

    private Direction input;

    public SelectorSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(SelectorSwitchBlock.TYPE.get(), pos, state);
        input = state.getValue(SelectorSwitchBlock.FACING);
    }

    public Direction input() {
        return input == null ? Direction.NORTH : input;
    }

    public Component status() {
        return Component.translatable("message.gt6logic.selector_switch.status", directionLabel(input()));
    }

    public static Component directionLabel(Direction direction) {
        return Component.translatable("message.gt6logic.direction." + direction.getSerializedName());
    }

    public void cycleInput() {
        Direction[] values = Direction.values();
        input = values[(input().ordinal() + 1) % values.length];
        setChanged();
        BlockFeedback.syncClient(this);
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(SelectorSwitchBlock.BLOCK.get())) {
            return;
        }
        long value = GtSignalCarrier.clamp(read(input()));
        FaceSignals.send(output(), value, input());
        BlockFeedback.setPowered(this, SelectorSwitchBlock.POWERED, SignalLevels.ceilingLevel(value) >= 1);
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
        tag.putString(INPUT, input().getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Direction fallback = getBlockState().getValue(SelectorSwitchBlock.FACING);
        Direction parsed = Direction.byName(tag.getString(INPUT));
        if (parsed == null) {
            input = fallback;
        } else {
            input = parsed;
        }
    }
}
