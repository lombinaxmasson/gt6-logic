package com.lombinaxmasson.gt6logic.block;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
