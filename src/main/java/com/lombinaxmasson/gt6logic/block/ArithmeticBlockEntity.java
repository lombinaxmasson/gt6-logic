package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.Arithmetic;
import com.lombinaxmasson.gt6logic.control.Arithmetic.Op;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Left is the first number, right is the second. The front carries the result. */
public final class ArithmeticBlockEntity extends SignalBlockEntity {
    private static final String OP = "Op";

    private Op op = Op.ADD;

    public ArithmeticBlockEntity(BlockPos pos, BlockState state) {
        super(ArithmeticBlock.TYPE.get(), pos, state);
    }

    public Op op() {
        return op;
    }

    public Component status() {
        return Component.translatable("message.gt6logic.arithmetic." + op.name().toLowerCase(java.util.Locale.ROOT));
    }

    public void adjust(Player player) {
        op = op.next();
        setChanged();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.hasProperty(AnalogPlate.SUPPORT)) {
            return;
        }
        GatePorts.Sides ports = AnalogPlate.ports(state);
        AnalogPlate.publish(this, Arithmetic.apply(op, read(ports.left()), read(ports.right())), ports.front());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(OP, op.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String name = tag.getString(OP);
        op = Op.byName(name).name().equals(name) ? Op.byName(name) : Op.ADD;
    }
}
