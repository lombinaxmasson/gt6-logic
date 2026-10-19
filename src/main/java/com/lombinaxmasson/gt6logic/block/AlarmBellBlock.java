package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A bell the player can place. It listens to the neighbor it faces and does not override
 * {@code getSignal}. The block entity is only a receiver, so a signal wire skips the bell.
 */
public final class AlarmBellBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");

    /** Conducts strong power, like any solid block. The block entity is a receiver, so a wire still skips it. */
    public static final DeferredBlock<AlarmBellBlock> BLOCK = Gt6Logic.BLOCKS.register("alarm_bell",
            () -> new AlarmBellBlock(Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isRedstoneConductor((state, getter, pos) -> true)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("alarm_bell",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmBellBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("alarm_bell", () ->
                    BlockEntityType.Builder.of(AlarmBellBlockEntity::new, BLOCK.get()).build(null));

    /** Loads this class so the deferred entries exist before registry events fire. */
    public static void bootstrap() {}

    public AlarmBellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RINGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PlateShapes.bell(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlarmBellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getBlock() != this || type != TYPE.get()) {
            return null;
        }
        return (tickLevel, pos, blockState, entity) -> ((AlarmBellBlockEntity) entity).tickServer();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlarmBellBlockEntity bell) {
            if (player.isShiftKeyDown()) {
                bell.cycleTimbre();
            } else {
                bell.cycleThreshold();
            }
            tell(player, bell);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlarmBellBlockEntity bell) {
            tell(player, bell);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void tell(Player player, AlarmBellBlockEntity bell) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.displayClientMessage(bell.status(), true);
        }
    }

    /** The three vanilla sounds a bell can use. Worlds store the enum constant name, so the order can change. */
    public enum Timbre {
        BELL(Holder.direct(SoundEvents.BELL_BLOCK), "message.gt6logic.alarm_bell.timbre.bell"),
        CHIME(SoundEvents.NOTE_BLOCK_CHIME, "message.gt6logic.alarm_bell.timbre.chime"),
        PLING(SoundEvents.NOTE_BLOCK_PLING, "message.gt6logic.alarm_bell.timbre.pling");

        private final Holder<SoundEvent> sound;
        private final String translationKey;

        Timbre(Holder<SoundEvent> sound, String translationKey) {
            this.sound = sound;
            this.translationKey = translationKey;
        }

        public SoundEvent soundEvent() {
            return sound.value();
        }

        public Component label() {
            return Component.translatable(translationKey);
        }

        public Timbre next() {
            Timbre[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static Timbre byName(String name) {
            for (Timbre timbre : values()) {
                if (timbre.name().equals(name)) {
                    return timbre;
                }
            }
            return BELL;
        }
    }
}
