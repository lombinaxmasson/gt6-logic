package com.lombinaxmasson.gt6logic.client;

import org.joml.Quaternionf;

import net.minecraft.core.Direction;

/**
 * The same turn the blockstate applies to a model whose front is its south face
 * and whose plate is on its north side. {@code facing} is the wall.
 * Local +Z then points into the room, which is the free face.
 */
public final class PlateTurn {
    private PlateTurn() {}

    public static Quaternionf of(Direction facing) {
        int x = switch (facing) {
            case DOWN -> 90;
            case UP -> 270;
            default -> 0;
        };
        int y = switch (facing) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
        float rad = (float) (Math.PI / 180.0);
        return new Quaternionf().rotationYXZ(-y * rad, -x * rad, 0f);
    }
}
