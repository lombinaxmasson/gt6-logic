package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** A named lamp for a status wall. Right click changes the name, not the lamp. */
public final class ConsolePanelBlock extends ReceiverPlate {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final DeferredBlock<ConsolePanelBlock> BLOCK = Gt6Logic.BLOCKS.register("console_panel",
            () -> new ConsolePanelBlock(properties(MapColor.COLOR_BLUE)
                    .lightLevel(state -> state.getValue(POWERED) ? 7 : 0)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("console_panel",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConsolePanelBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("console_panel", () ->
                    BlockEntityType.Builder.of(ConsolePanelBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public ConsolePanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsolePanelBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity panel) {
            panel.cycle();
            BlockFeedback.tell(player, panel.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity panel) {
            return BlockFeedback.glass(stack, level, player, panel.status());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
