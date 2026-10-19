package com.lombinaxmasson.gt6logic.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla redstone sees the published signal, rounded up to a whole level.
 * The fractional value stays on {@link SignalBlockEntity#gtSignal(int)} for signal wires.
 */
public abstract class SignalBlock extends Block implements EntityBlock {
    protected SignalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return vanillaLevel(level, pos, direction);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return vanillaLevel(level, pos, direction);
    }

    /**
     * Server ticker for a signal block entity. Returns null on the client and for other block entity types.
     */
    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker(
            Level level, BlockEntityType<T> actual, BlockEntityType<?> expected) {
        if (level.isClientSide || actual != expected) {
            return null;
        }
        return (tickLevel, pos, state, entity) -> ((SignalBlockEntity) entity).finishTick();
    }

    private static int vanillaLevel(BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SignalBlockEntity signal) {
            return SignalLevels.ceilingLevel(signal.gtSignal(direction.ordinal()));
        }
        return 0;
    }
}
