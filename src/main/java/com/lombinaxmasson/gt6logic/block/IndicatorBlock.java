package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A lamp the player can place. It does not override {@code getSignal} and it is not a
 * {@link com.gregtech.gregtech.api.signal.GtSignalSource}. The block entity is only a receiver,
 * so a signal wire skips the lamp instead of treating conducted strong power as a new source.
 */
public final class IndicatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Conducts strong power, like any solid block. The block entity is a receiver, so a wire still skips it. */
    public static final DeferredBlock<IndicatorBlock> BLOCK = Gt6Logic.BLOCKS.register("indicator",
            () -> new IndicatorBlock(Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(LIT) ? 15 : 0)
                    .isRedstoneConductor((state, getter, pos) -> true)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("indicator",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndicatorBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("indicator", () ->
                    BlockEntityType.Builder.of(IndicatorBlockEntity::new, BLOCK.get()).build(null));

    /** Loads this class so the deferred entries exist before registry events fire. */
    public static void bootstrap() {}

    public IndicatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndicatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getBlock() != this || type != TYPE.get()) {
            return null;
        }
        return (tickLevel, pos, blockState, entity) -> ((IndicatorBlockEntity) entity).tickServer();
    }
}
