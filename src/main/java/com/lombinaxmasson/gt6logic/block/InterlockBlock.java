package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.control.Interlock;
import com.lombinaxmasson.gt6logic.signal.SignalBlock;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Front is input A. The opposite face is input B. The other four faces carry the result. */
public final class InterlockBlock extends SignalBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Gate> GATE = EnumProperty.create("mode", Gate.class);

    /** Block-state names for the three symbols on the plate. Logic stays on {@link Interlock.Mode}. */
    public enum Gate implements StringRepresentable {
        AND,
        OR,
        NOT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Gate of(Interlock.Mode mode) {
            return switch (mode) {
                case OR -> OR;
                case NOT -> NOT;
                case AND -> AND;
            };
        }
    }

    public static final DeferredBlock<InterlockBlock> BLOCK = Gt6Logic.BLOCKS.register("interlock",
            () -> new InterlockBlock(Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(POWERED) ? 7 : 0)
                    .isRedstoneConductor((state, getter, pos) -> false)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("interlock",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InterlockBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("interlock", () ->
                    BlockEntityType.Builder.of(InterlockBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public InterlockBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(GATE, Gate.AND));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED, GATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PlateShapes.plate(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InterlockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return SignalBlock.serverTicker(level, type, TYPE.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InterlockBlockEntity interlock) {
            interlock.cycleMode();
            BlockFeedback.tell(player, interlock.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InterlockBlockEntity interlock) {
            BlockFeedback.tell(player, interlock.status());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
