package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.signal.GtSignalReceiver;
import com.lombinaxmasson.gt6logic.energy.EuPort;
import com.lombinaxmasson.gt6logic.energy.EuStore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** An actuator with an EU buffer. Subclasses say whether that buffer accepts or emits. */
public abstract class PoweredEntity extends BlockEntity implements EuPort, ServerTick, GtSignalReceiver {
    private static final String ENERGY = "Energy";

    private final EuStore power;

    protected PoweredEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        super(type, pos, state);
        this.power = new EuStore(capacity);
    }

    @Override
    public final EuStore eu() {
        return power;
    }

    @Override
    public boolean acceptsEu() {
        return true;
    }

    @Override
    public boolean emitsEu() {
        return false;
    }

    @Override
    public void onEnergyChanged() {
        setChanged();
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
