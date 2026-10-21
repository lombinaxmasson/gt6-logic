package com.lombinaxmasson.gt6logic.block;

import net.minecraft.core.Direction;

/**
 * The four edges of a plate. Rotation 0 points the front away from a player who is looking north
 * when the plate is on the floor. One step clockwise moves the old right edge to the front.
 * Clockwise is taken while looking toward the support face.
 */
public final class GatePorts {
    public record Sides(Direction front, Direction right, Direction back, Direction left) {}

    private GatePorts() {}

    public static Sides of(Direction support, int rotation) {
        if (support == null) {
            throw new IllegalArgumentException("support face is missing");
        }
        Direction front = zeroFront(support);
        Direction right = zeroRight(support);
        int turns = Math.floorMod(rotation, 4);
        for (int i = 0; i < turns; i++) {
            Direction oldFront = front;
            front = right;
            right = oldFront.getOpposite();
        }
        return new Sides(front, right, front.getOpposite(), right.getOpposite());
    }

    /** Rotation whose front is {@code front}, or 0 when {@code front} is not an edge of {@code support}. */
    public static int rotationWithFront(Direction support, Direction front) {
        for (int rotation = 0; rotation < 4; rotation++) {
            if (of(support, rotation).front() == front) {
                return rotation;
            }
        }
        return 0;
    }

    private static Direction zeroFront(Direction support) {
        return support.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
    }

    private static Direction zeroRight(Direction support) {
        return switch (support) {
            case UP -> Direction.WEST;
            case DOWN -> Direction.EAST;
            default -> support.getClockWise();
        };
    }
}
