package com.lombinaxmasson.gt6logic.block;

import com.gregtech.gregtech.api.addon.platform.AddonTools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shared block-entity chores that are too small for their own card. */
final class BlockFeedback {
    private BlockFeedback() {}

    static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.displayClientMessage(message, true);
        }
    }

    /** Magnifying glass reads. Any other item keeps its own use action. */
    static ItemInteractionResult glass(ItemStack stack, Level level, Player player, Component message) {
        if (!AddonTools.isMagnifyingGlass(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            tell(player, message);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sends the block entity's update tag. The chunk packet is built from {@code getUpdateTag}. */
    static void syncClient(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = entity.getBlockState();
        level.sendBlockUpdated(entity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    static void setPowered(BlockEntity entity, BooleanProperty property, boolean on) {
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = entity.getBlockState();
        if (!state.hasProperty(property) || state.getValue(property) == on) {
            return;
        }
        level.setBlock(entity.getBlockPos(), state.setValue(property, on), Block.UPDATE_CLIENTS);
    }
}
