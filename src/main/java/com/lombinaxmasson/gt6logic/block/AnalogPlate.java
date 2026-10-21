package com.lombinaxmasson.gt6logic.block;

import org.jetbrains.annotations.Nullable;

import com.lombinaxmasson.gt6logic.signal.SignalBlock;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A floor plate. Support is the face it sits on. Rotation picks the four edges.
 * The first version only survives on the floor. The result leaves through the front.
 */
public abstract class AnalogPlate extends SignalBlock {
    public static final DirectionProperty SUPPORT = DirectionProperty.create("support");
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
    public static final BooleanProperty INPUT_LEFT = BooleanProperty.create("input_left");
    public static final BooleanProperty INPUT_RIGHT = BooleanProperty.create("input_right");
    public static final BooleanProperty INPUT_BACK = BooleanProperty.create("input_back");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected AnalogPlate(Properties properties, boolean left, boolean right, boolean back) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(SUPPORT, Direction.DOWN)
                .setValue(ROTATION, 0)
                .setValue(INPUT_LEFT, left)
                .setValue(INPUT_RIGHT, right)
                .setValue(INPUT_BACK, back)
                .setValue(POWERED, false));
    }

    public static Properties properties(MapColor color) {
        return Properties.of()
                .mapColor(color)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(POWERED) ? 7 : 0)
                .isRedstoneConductor((state, getter, pos) -> false);
    }

    public static GatePorts.Sides ports(BlockState state) {
        return GatePorts.of(state.getValue(SUPPORT), state.getValue(ROTATION));
    }

    /** The result leaves through {@code front}. The lamp follows a vanilla level of at least 1. */
    static void publish(SignalBlockEntity entity, long signal, Direction front) {
        FaceSignals.onlyToward(entity.output(), signal, front);
        BlockFeedback.setPowered(entity, POWERED, SignalLevels.ceilingLevel(signal) >= 1);
    }

    protected abstract BlockEntityType<?> entityType();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUPPORT, ROTATION, INPUT_LEFT, INPUT_RIGHT, INPUT_BACK, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return null;
        }
        BlockPos floor = context.getClickedPos().below();
        if (!context.getLevel().getBlockState(floor).isFaceSturdy(context.getLevel(), floor, Direction.UP)) {
            return null;
        }
        int rotation = GatePorts.rotationWithFront(Direction.DOWN, context.getHorizontalDirection());
        return defaultBlockState().setValue(SUPPORT, Direction.DOWN).setValue(ROTATION, rotation);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(SUPPORT) != Direction.DOWN) {
            return false;
        }
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PlateShapes.plate(state.getValue(SUPPORT));
    }

    /** Dust standing on the front passes the opposite direction. The other five faces stay disconnected. */
    @Override
    protected Direction redstoneQuery(BlockState state) {
        return ports(state).front().getOpposite();
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && direction.equals(redstoneQuery(state));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return SignalBlock.serverTicker(level, type, entityType());
    }
}
