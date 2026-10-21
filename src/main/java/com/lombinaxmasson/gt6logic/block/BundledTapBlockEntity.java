package com.lombinaxmasson.gt6logic.block;

import java.util.Arrays;

import com.lombinaxmasson.gt6logic.signal.BundledScale;
import com.lombinaxmasson.gt6logic.signal.SignalBlockEntity;
import com.lombinaxmasson.gt6logic.signal.SignalLevels;

import mrtjp.projectred.api.IBundledTile;
import mrtjp.projectred.api.ProjectRedAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One colour of a bundled cable, in either direction.
 * A new tap listens: cable toward GT, colour white.
 */
public final class BundledTapBlockEntity extends SignalBlockEntity implements IBundledTile {
    private static final String COLOR = "Color";
    private static final String TO_CABLE = "ToCable";
    private static final int COLOURS = 16;

    private int color;
    private boolean toCable;
    private byte[] emitted = new byte[COLOURS];

    public BundledTapBlockEntity(BlockPos pos, BlockState state) {
        super(BundledTapBlock.TYPE.get(), pos, state);
    }

    public int color() {
        return color;
    }

    public boolean toCable() {
        return toCable;
    }

    public Component status() {
        String direction = toCable ? "message.gt6logic.bundled_tap.to_cable" : "message.gt6logic.bundled_tap.to_gt";
        Component colour = Component.translatable("color.minecraft." + DyeColor.byId(color).getName());
        return Component.translatable("message.gt6logic.bundled_tap.status", colour, Component.translatable(direction));
    }

    /** Sneak steps the colour. A plain click flips which way the value travels. */
    public void adjust(Player player) {
        if (player.isShiftKeyDown()) {
            color = (color + 1) % COLOURS;
        } else {
            toCable = !toCable;
        }
        setChanged();
    }

    @Override
    public boolean canConnectBundled(int side) {
        Direction cable = cableFace();
        return cable != null && side == cable.ordinal();
    }

    @Override
    public byte[] getBundledSignal(int side) {
        if (!toCable || !canConnectBundled(side)) {
            return null;
        }
        return emitted.clone();
    }

    @Override
    protected void onTick() {
        BlockState state = getBlockState();
        if (!state.is(BundledTapBlock.BLOCK.get()) || level == null) {
            return;
        }
        Direction facing = state.getValue(BundledTapBlock.FACING);
        Direction cable = facing.getOpposite();
        if (toCable) {
            int bundled = BundledScale.toBundled(read(facing));
            byte[] next = new byte[COLOURS];
            next[color] = (byte) bundled;
            FaceSignals.onlyToward(output(), 0L, facing);
            publishBytes(next);
            BlockFeedback.setPowered(this, BundledTapBlock.POWERED, bundled > 0);
            return;
        }
        int bundled = readCable(cable);
        FaceSignals.onlyToward(output(), BundledScale.toSignal(bundled), facing);
        publishBytes(new byte[COLOURS]);
        BlockFeedback.setPowered(this, BundledTapBlock.POWERED, SignalLevels.ceilingLevel(BundledScale.toSignal(bundled)) >= 1);
    }

    private void publishBytes(byte[] next) {
        if (Arrays.equals(emitted, next)) {
            return;
        }
        emitted = next;
        setChanged();
        Level world = getLevel();
        if (world != null) {
            world.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    private int readCable(Direction cable) {
        if (level == null || ProjectRedAPI.transmissionAPI == null) {
            return 0;
        }
        byte[] input = ProjectRedAPI.transmissionAPI.getBundledInput(level, worldPosition, cable);
        if (input == null || color < 0 || color >= input.length) {
            return 0;
        }
        return input[color] & 0xFF;
    }

    private Direction cableFace() {
        BlockState state = getBlockState();
        if (!state.hasProperty(BundledTapBlock.FACING)) {
            return null;
        }
        return state.getValue(BundledTapBlock.FACING).getOpposite();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(COLOR, color);
        tag.putBoolean(TO_CABLE, toCable);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int saved = tag.getInt(COLOR);
        color = saved >= 0 && saved < COLOURS ? saved : 0;
        toCable = tag.getBoolean(TO_CABLE);
    }
}
