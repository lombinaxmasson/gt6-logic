package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** A lever that publishes a full signal on every face while it is on. */
public final class BigLeverBlock extends LogicPlateBlock {
    public static final DeferredBlock<BigLeverBlock> BLOCK = Gt6Logic.BLOCKS.register("big_lever",
            () -> new BigLeverBlock(properties(MapColor.COLOR_ORANGE)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("big_lever",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigLeverBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("big_lever", () ->
                    BlockEntityType.Builder.of(BigLeverBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public BigLeverBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigLeverBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean on = !state.getValue(POWERED);
            level.setBlock(pos, state.setValue(POWERED, on), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, on ? 0.6F : 0.5F);
            BlockFeedback.tell(player, status(on));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockFeedback.glass(stack, level, player, status(state.getValue(POWERED)));
    }

    private static Component status(boolean on) {
        return Component.translatable(on ? "message.gt6logic.lever.on" : "message.gt6logic.lever.off");
    }
}
