package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.control.TenThousandths;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Fill of the fluid handler touching {@code toward}, in ten-thousandths. No handler reads as empty. */
public final class FluidLevel {
    private FluidLevel() {}

    public static int basisPoints(Level level, BlockPos pos, Direction toward) {
        IFluidHandler handler = FluidMove.face(level, pos.relative(toward), toward.getOpposite());
        return basisPoints(handler);
    }

    public static int basisPoints(IFluidHandler handler) {
        if (handler == null || handler.getTanks() <= 0) {
            return 0;
        }
        long amount = 0L;
        long capacity = 0L;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += handler.getFluidInTank(tank).getAmount();
            capacity += handler.getTankCapacity(tank);
        }
        return TenThousandths.of(amount, capacity);
    }
}
