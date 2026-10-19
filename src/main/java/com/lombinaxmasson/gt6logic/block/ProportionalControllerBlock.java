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

/** Right click changes the setting. The magnifying glass reads it back. */
public final class ProportionalControllerBlock extends LogicPlateBlock {
    public static final DeferredBlock<ProportionalControllerBlock> BLOCK = Gt6Logic.BLOCKS.register("proportional_controller",
            () -> new ProportionalControllerBlock(properties(MapColor.COLOR_RED)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("proportional_controller",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProportionalControllerBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("proportional_controller", () ->
                    BlockEntityType.Builder.of(ProportionalControllerBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public ProportionalControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProportionalControllerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProportionalControllerBlockEntity plate) {
            plate.adjust(player);
            BlockFeedback.tell(player, plate.status());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProportionalControllerBlockEntity plate) {
            BlockFeedback.tell(player, plate.status());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
