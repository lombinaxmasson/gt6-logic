package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thin shapes for logic parts. The plate sits on the face the block listens through.
 * The instrument faces the room, on the near side of that plate.
 */
public final class PlateShapes {
    /** Centered lamp. It has no facing, so the shape is the same on every side. */
    public static final VoxelShape LAMP = Block.box(4, 4, 4, 12, 12, 12);

    private static final VoxelShape[] PLATES = {
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 16, 2),
            Block.box(0, 0, 14, 16, 16, 16),
            Block.box(0, 0, 0, 2, 16, 16),
            Block.box(14, 0, 0, 16, 16, 16)
    };

    private static final VoxelShape[] BELLS = {
            Shapes.or(PLATES[0], Block.box(4, 2, 4, 12, 9, 12)),
            Shapes.or(PLATES[1], Block.box(4, 7, 4, 12, 14, 12)),
            Shapes.or(PLATES[2], Block.box(4, 4, 2, 12, 14, 9)),
            Shapes.or(PLATES[3], Block.box(4, 4, 7, 12, 14, 14)),
            Shapes.or(PLATES[4], Block.box(2, 4, 4, 9, 14, 12)),
            Shapes.or(PLATES[5], Block.box(7, 4, 4, 14, 14, 12))
    };

    private PlateShapes() {}

    public static VoxelShape plate(Direction facing) {
        return PLATES[facing.ordinal()];
    }

    public static VoxelShape bell(Direction facing) {
        return BELLS[facing.ordinal()];
    }
}
