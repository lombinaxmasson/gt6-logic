package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** Facing sticks to the container. The free face carries the fill, from empty to full. */
public final class LevelGaugeBlock extends LogicPlateBlock {
    public static final DeferredBlock<LevelGaugeBlock> BLOCK = Gt6Logic.BLOCKS.register("level_gauge",
            () -> new LevelGaugeBlock(properties(MapColor.COLOR_LIGHT_BLUE)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("level_gauge",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LevelGaugeBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("level_gauge", () ->
                    BlockEntityType.Builder.of(LevelGaugeBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public LevelGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    /** A neighbor on the free side looks back along {@code FACING}. */
    @Override
    protected Direction redstoneQuery(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LevelGaugeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LevelGaugeBlockEntity gauge) {
            BlockFeedback.tell(player, gauge.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LevelGaugeBlockEntity gauge) {
            BlockFeedback.tell(player, gauge.status());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
