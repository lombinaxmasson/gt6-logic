package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/** Right click pulses for one second. Sneak changes the painted name and does not pulse. */
public final class LabeledButtonBlock extends LogicPlateBlock {
    public static final DeferredBlock<LabeledButtonBlock> BLOCK = Gt6Logic.BLOCKS.register("labeled_button",
            () -> new LabeledButtonBlock(properties(MapColor.COLOR_LIGHT_BLUE)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("labeled_button",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LabeledButtonBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("labeled_button", () ->
                    BlockEntityType.Builder.of(LabeledButtonBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public LabeledButtonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LabeledButtonBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LabeledButtonBlockEntity button) {
            if (player.isShiftKeyDown()) {
                button.cycleLabel();
            } else {
                button.press();
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, 1.0F);
            }
            BlockFeedback.tell(player, button.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LabeledButtonBlockEntity button) {
            return BlockFeedback.glass(stack, level, player, button.status());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
