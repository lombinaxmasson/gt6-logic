package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Pulls from the inlet neighbor and pushes into the outlet neighbor. Neither side is stored here. */
public final class FluidMove {
    private FluidMove() {}

    public static int transfer(Level level, BlockPos pos, Direction inlet, Direction outlet, int millibuckets) {
        if (millibuckets <= 0) {
            return 0;
        }
        IFluidHandler from = face(level, pos.relative(inlet), inlet.getOpposite());
        IFluidHandler to = face(level, pos.relative(outlet), outlet.getOpposite());
        if (from == null || to == null) {
            return 0;
        }
        FluidStack simulated = from.drain(millibuckets, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return 0;
        }
        int room = to.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (room <= 0) {
            return 0;
        }
        FluidStack drained = from.drain(room, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }
        int filled = to.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        int leftover = drained.getAmount() - filled;
        if (leftover > 0) {
            FluidStack back = drained.copy();
            back.setAmount(leftover);
            from.fill(back, IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    static IFluidHandler face(Level level, BlockPos pos, Direction touched) {
        return FluidUtil.getFluidHandler(level, pos, touched).orElse(null);
    }
}
