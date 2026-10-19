package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.addon.platform.AddonTools;
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

/** Status on an empty click or the magnifying glass. Aim the block when placing it. */
public final class AdjustableCoolerBlock extends ActuatorBlock {
    public static final DeferredBlock<AdjustableCoolerBlock> BLOCK = Gt6Logic.BLOCKS.register("adjustable_cooler",
            () -> new AdjustableCoolerBlock(properties(MapColor.COLOR_LIGHT_BLUE)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("adjustable_cooler",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdjustableCoolerBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("adjustable_cooler", () ->
                    BlockEntityType.Builder.of(AdjustableCoolerBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public AdjustableCoolerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdjustableCoolerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        tell(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        tell(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void tell(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdjustableCoolerBlockEntity machine) {
            BlockFeedback.tell(player, machine.status());
        }
    }
}
