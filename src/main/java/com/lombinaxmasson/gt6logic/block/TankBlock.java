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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Sixteen buckets of one fluid. A bucket in hand fills or drains it. */
public final class TankBlock extends Block implements EntityBlock {
    public static final DeferredBlock<TankBlock> BLOCK = Gt6Logic.BLOCKS.register("tank",
            () -> new TankBlock(Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .isRedstoneConductor((state, getter, pos) -> false)));

    public static final DeferredItem<BlockItem> ITEM = Gt6Logic.ITEMS.register("tank",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TYPE =
            Gt6Logic.BLOCK_ENTITIES.register("tank", () ->
                    BlockEntityType.Builder.of(TankBlockEntity::new, BLOCK.get()).build(null));

    public static void bootstrap() {}

    public TankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        tell(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (AddonTools.isMagnifyingGlass(stack)) {
            tell(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && FluidUtil.interactWithFluidHandler(player, hand, tank.fluids())) {
            return ItemInteractionResult.sidedSuccess(false);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void tell(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            BlockFeedback.tell(player, tank.status());
        }
    }
}
