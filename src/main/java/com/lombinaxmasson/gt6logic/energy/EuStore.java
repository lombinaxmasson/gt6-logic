package com.lombinaxmasson.gt6logic.energy;

import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.api.machine.BasicMachineEnergy;

/**
 * A small EU buffer. Packets follow the addon energy helper:
 * one amp at a given voltage moves that many EU, and a full cover of amps fills the buffer.
 */
public final class EuStore {
    /** Short name GT cables pass for electric energy. */
    public static final String EU = "EU";
    public static final String HEAT = "HU";
    public static final String COLD = "CU";
    /** LV, the second GT voltage tier. ULV is 8. */
    public static final long LV = GTVoltageTiers.VOLTAGES[1];

    private final long capacity;
    private long stored;

    public EuStore(long capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("energy capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    public void restore(long stored) {
        if (stored < 0L) {
            stored = 0L;
        }
        this.stored = Math.min(stored, capacity);
    }

    public long demanded(long voltage) {
        return BasicMachineEnergy.demanded(stored, capacity, voltage);
    }

    /** Amps accepted. Simulation leaves the buffer alone. */
    public long inject(long voltage, long amperage, boolean simulate) {
        long taken = BasicMachineEnergy.accepted(stored, capacity, voltage, amperage);
        if (!simulate && taken > 0L) {
            stored = BasicMachineEnergy.add(stored, capacity, voltage, taken);
        }
        return taken;
    }

    public long offered(long voltage) {
        return packets(stored, voltage);
    }

    /** Amps removed. Simulation leaves the buffer alone. */
    public long extract(long voltage, long amperage, boolean simulate) {
        long taken = Math.min(Math.max(amperage, 0L), offered(voltage));
        if (!simulate && taken > 0L) {
            stored = BasicMachineEnergy.drain(stored, Math.abs(voltage) * taken);
        }
        return taken;
    }

    /** Spends up to {@code eu} and returns what was actually spent. */
    public long consumeUpTo(long eu) {
        if (eu <= 0L || stored <= 0L) {
            return 0L;
        }
        long spend = Math.min(stored, eu);
        stored = BasicMachineEnergy.drain(stored, spend);
        return spend;
    }

    public static boolean isEu(String type) {
        return EU.equalsIgnoreCase(type);
    }

    public static boolean isHeat(String type) {
        return HEAT.equalsIgnoreCase(type);
    }

    public static boolean isCold(String type) {
        return COLD.equalsIgnoreCase(type);
    }

    /** How many packets of {@code voltage} fit into {@code energy}. */
    public static long packets(long energy, long voltage) {
        if (energy <= 0L || voltage == 0L || voltage == Long.MIN_VALUE) {
            return 0L;
        }
        return energy / Math.abs(voltage);
    }
}
