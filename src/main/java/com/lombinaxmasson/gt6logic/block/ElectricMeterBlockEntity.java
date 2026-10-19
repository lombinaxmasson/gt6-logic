package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.TenThousandths;
import com.lombinaxmasson.gt6logic.energy.EuPort;
import com.lombinaxmasson.gt6logic.energy.EuStore;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** A small EU buffer in the line. Every face publishes how full that buffer is. */
public final class ElectricMeterBlockEntity extends SignalBlockEntity implements EuPort {
    public static final long CAPACITY = 10_240L;
    private static final String ENERGY = "Energy";

    private final EuStore power = new EuStore(CAPACITY);

    public ElectricMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ElectricMeterBlock.TYPE.get(), pos, state);
    }

    @Override
    public EuStore eu() {
        return power;
    }

    @Override
    public boolean acceptsEu() {
        return true;
    }

    @Override
    public boolean emitsEu() {
        return true;
    }

    @Override
    public void onEnergyChanged() {
        setChanged();
    }

    public Component status() {
        int part = TenThousandths.of(power.stored(), power.capacity());
        return Component.translatable("message.gt6logic.meter.status", TenThousandths.percent(part));
    }

    @Override
    protected void onTick() {
        long signal = TenThousandths.toSignal(TenThousandths.of(power.stored(), power.capacity()));
        for (int side = 0; side < 6; side++) {
            output().set(side, signal);
        }
        BlockFeedback.setPowered(this, ElectricMeterBlock.POWERED, SignalLevels.ceilingLevel(signal) >= 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(ENERGY, power.stored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power.restore(tag.getLong(ENERGY));
    }
}
