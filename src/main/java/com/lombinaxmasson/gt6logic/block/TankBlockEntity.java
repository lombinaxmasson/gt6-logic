package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.TenThousandths;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** One fluid, sixteen buckets. Every face is the same tank. Breaking it voids what was inside. */
public final class TankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private static final String TANK = "Tank";

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            TankBlockEntity.this.setChanged();
        }
    };

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(TankBlock.TYPE.get(), pos, state);
    }

    public FluidTank fluids() {
        return tank;
    }

    public Component status() {
        int part = TenThousandths.of(tank.getFluidAmount(), tank.getCapacity());
        if (tank.isEmpty()) {
            return Component.translatable("message.gt6logic.tank.empty");
        }
        return Component.translatable("message.gt6logic.tank.level", TenThousandths.percent(part), tank.getFluid().getHoverName());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TANK, tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TANK)) {
            tank.readFromNBT(registries, tag.getCompound(TANK));
        }
    }
}
