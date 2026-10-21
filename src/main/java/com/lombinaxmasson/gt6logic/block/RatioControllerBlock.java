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
public final class RatioControllerBlock extends AnalogPlate {
    public static final DeferredBlock<RatioControllerBlock> BLOCK = Gt6Logic.BLOCKS.register("ratio_controller",
            () -> new RatioControllerBlock(properties(MapColor.COLOR_GREEN)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("ratio_controller",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RatioControllerBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("ratio_controller", () ->
                    BlockEntityType.Builder.of(RatioControllerBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public RatioControllerBlock(Properties properties) {
        super(properties, false, false, true);
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return TYPE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RatioControllerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RatioControllerBlockEntity plate) {
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RatioControllerBlockEntity plate) {
            BlockFeedback.tell(player, plate.status());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
