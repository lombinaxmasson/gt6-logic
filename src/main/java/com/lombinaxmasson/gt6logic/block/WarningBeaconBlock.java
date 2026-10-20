package com.lombinaxmasson.gt6logic.block;

import java.util.Locale;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.control.BeaconBand;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A lamp the size of a full block. It conducts, and the entity is only a receiver,
 * so a signal wire skips the glow.
 */
public final class WarningBeaconBlock extends Block implements EntityBlock {
    public static final EnumProperty<Color> BAND = EnumProperty.create("band", Color.class);

    /** Block-state names. The bands themselves stay on {@link BeaconBand}. */
    public enum Color implements StringRepresentable {
        OFF,
        GREEN,
        YELLOW,
        RED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Color of(BeaconBand band) {
            return switch (band) {
                case GREEN -> GREEN;
                case YELLOW -> YELLOW;
                case RED -> RED;
                case OFF -> OFF;
            };
        }

        public String messageKey() {
            return "message.gt6logic.beacon." + getSerializedName();
        }

        public int light() {
            return switch (this) {
                case GREEN -> 8;
                case YELLOW -> 12;
                case RED -> 15;
                case OFF -> 0;
            };
        }
    }

    public static final DeferredBlock<WarningBeaconBlock> BLOCK = Gt6Logic.BLOCKS.register("warning_beacon",
            () -> new WarningBeaconBlock(Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(BAND).light())
                    .isRedstoneConductor((state, getter, pos) -> true)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("warning_beacon",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarningBeaconBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("warning_beacon", () ->
                    BlockEntityType.Builder.of(WarningBeaconBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public WarningBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BAND, Color.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BAND);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarningBeaconBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != TYPE.get()) {
            return null;
        }
        return (tickLevel, pos, blockState, entity) -> ((ServerTick) entity).tickServer();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockFeedback.tell(player, status(state));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockFeedback.glass(stack, level, player, status(state));
    }

    private static Component status(BlockState state) {
        return Component.translatable(state.getValue(BAND).messageKey());
    }
}
