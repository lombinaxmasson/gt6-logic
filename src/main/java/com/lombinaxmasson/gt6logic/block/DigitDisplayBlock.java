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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Right click swaps the unit. The face keeps showing the neighbor it is mounted on. */
public final class DigitDisplayBlock extends ReceiverPlate {
    public static final DeferredBlock<DigitDisplayBlock> BLOCK = Gt6Logic.BLOCKS.register("digit_display",
            () -> new DigitDisplayBlock(properties(MapColor.COLOR_BLACK).lightLevel(state -> 4)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("digit_display",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DigitDisplayBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("digit_display", () ->
                    BlockEntityType.Builder.of(DigitDisplayBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public DigitDisplayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitDisplayBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DigitDisplayBlockEntity display) {
            display.cycle();
            BlockFeedback.tell(player, display.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DigitDisplayBlockEntity display) {
            return BlockFeedback.glass(stack, level, player, display.status());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
