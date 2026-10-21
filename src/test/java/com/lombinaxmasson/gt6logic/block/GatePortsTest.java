package com.lombinaxmasson.gt6logic.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.lombinaxmasson.gt6logic.block.GatePorts.Sides;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class GatePortsTest {
    @Test
    void floorZeroFacesNorthAndOneTurnMovesRightToTheFront() {
        Sides zero = GatePorts.of(Direction.DOWN, 0);
        assertEquals(Direction.NORTH, zero.front());
        assertEquals(Direction.EAST, zero.right());
        assertEquals(Direction.SOUTH, zero.back());
        assertEquals(Direction.WEST, zero.left());

        Sides turned = GatePorts.of(Direction.DOWN, 1);
        assertEquals(zero.right(), turned.front());
        assertEquals(Direction.SOUTH, turned.right());
        assertEquals(0, GatePorts.rotationWithFront(Direction.DOWN, Direction.NORTH));
        assertEquals(1, GatePorts.rotationWithFront(Direction.DOWN, Direction.EAST));
        assertEquals(3, GatePorts.rotationWithFront(Direction.DOWN, Direction.WEST));
    }

    @Test
    void everySupportKeepsFourDistinctEdges() {
        for (Direction support : Direction.values()) {
            Direction previous = null;
            for (int rotation = 0; rotation < 4; rotation++) {
                Sides sides = GatePorts.of(support, rotation);
                assertNotEquals(support.getAxis(), sides.front().getAxis());
                assertNotEquals(support.getAxis(), sides.right().getAxis());
                assertEquals(sides.front().getOpposite(), sides.back());
                assertEquals(sides.right().getOpposite(), sides.left());
                assertNotEquals(sides.front(), sides.right());
                assertNotEquals(sides.front(), sides.back());
                if (previous != null) {
                    assertEquals(previous, sides.front());
                }
                previous = sides.right();
                assertEquals(rotation, GatePorts.rotationWithFront(support, sides.front()));
            }
        }
        Sides ceiling = GatePorts.of(Direction.UP, 0);
        assertEquals(Direction.NORTH, ceiling.front());
        assertEquals(Direction.WEST, ceiling.right());
        Sides wall = GatePorts.of(Direction.NORTH, 0);
        assertEquals(Direction.UP, wall.front());
        assertEquals(Direction.EAST, wall.right());
    }
}
