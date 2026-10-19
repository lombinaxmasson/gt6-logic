package com.lombinaxmasson.gt6logic.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gregtech.gregtech.api.addon.platform.EnergyBlock;

import net.minecraft.core.Direction;

/**
 * EU in, and optionally EU, heat, or cold out.
 * {@code doEnergyInjection} stays on the interface default so the voltage gate runs before {@link #doInject}.
 */
public interface EuPort extends EnergyBlock {
    EuStore eu();

    boolean acceptsEu();

    boolean emitsEu();

    default boolean emitsHeat() {
        return false;
    }

    default boolean emitsCold() {
        return false;
    }

    default long heat() {
        return 0L;
    }

    default void setHeat(long heat) {}

    default long cold() {
        return 0L;
    }

    default void setCold(long cold) {}

    /** A packet moved. The block entity marks itself dirty here. */
    void onEnergyChanged();

    @Override
    default boolean isEnergyType(String type, Direction side, boolean emitting) {
        if (EuStore.isEu(type)) {
            return emitting ? emitsEu() : acceptsEu();
        }
        if (!emitting) {
            return false;
        }
        if (EuStore.isHeat(type)) {
            return emitsHeat();
        }
        return EuStore.isCold(type) && emitsCold();
    }

    @Override
    default Collection<String> getEnergyTypes(Direction side) {
        List<String> types = new ArrayList<>(3);
        if (acceptsEu() || emitsEu()) {
            types.add(EuStore.EU);
        }
        if (emitsHeat()) {
            types.add(EuStore.HEAT);
        }
        if (emitsCold()) {
            types.add(EuStore.COLD);
        }
        return List.copyOf(types);
    }

    @Override
    default long getEnergyDemanded(String type, Direction side, long voltage) {
        if (!acceptsEu() || !EuStore.isEu(type)) {
            return 0L;
        }
        return eu().demanded(voltage);
    }

    @Override
    default long getEnergyOffered(String type, Direction side, long voltage) {
        if (EuStore.isEu(type) && emitsEu()) {
            return eu().offered(voltage);
        }
        if (EuStore.isHeat(type) && emitsHeat()) {
            return EuStore.packets(heat(), voltage);
        }
        if (EuStore.isCold(type) && emitsCold()) {
            return EuStore.packets(cold(), voltage);
        }
        return 0L;
    }

    @Override
    default long doInject(String type, Direction side, long voltage, long amperage, boolean simulate) {
        if (!acceptsEu() || !EuStore.isEu(type)) {
            return 0L;
        }
        long taken = eu().inject(voltage, amperage, simulate);
        if (!simulate && taken > 0L) {
            onEnergyChanged();
        }
        return taken;
    }

    @Override
    default long doExtract(String type, Direction side, long voltage, long amperage, boolean simulate) {
        if (EuStore.isEu(type) && emitsEu()) {
            long taken = eu().extract(voltage, amperage, simulate);
            if (!simulate && taken > 0L) {
                onEnergyChanged();
            }
            return taken;
        }
        if (EuStore.isHeat(type) && emitsHeat()) {
            return draw(heat(), voltage, amperage, simulate, true);
        }
        if (EuStore.isCold(type) && emitsCold()) {
            return draw(cold(), voltage, amperage, simulate, false);
        }
        return 0L;
    }

    @Override
    default long getEnergySizeInputRecommended(String type, Direction side) {
        return acceptsEu() && EuStore.isEu(type) ? EuStore.LV : 0L;
    }

    @Override
    default long getEnergySizeOutputRecommended(String type, Direction side) {
        if (emitsEu() && EuStore.isEu(type)) {
            return EuStore.LV;
        }
        if (emitsHeat() && EuStore.isHeat(type)) {
            return 1L;
        }
        if (emitsCold() && EuStore.isCold(type)) {
            return 1L;
        }
        return 0L;
    }

    @Override
    default boolean isEnergyCapacitorType(String type, Direction side) {
        return EuStore.isEu(type) && (acceptsEu() || emitsEu());
    }

    @Override
    default long getEnergyStored(String type, Direction side) {
        return EuStore.isEu(type) ? eu().stored() : 0L;
    }

    @Override
    default long getEnergyCapacity(String type, Direction side) {
        return EuStore.isEu(type) ? eu().capacity() : 0L;
    }

    @Override
    default Collection<String> getEnergyCapacitorTypes(Direction side) {
        return isEnergyCapacitorType(EuStore.EU, side) ? List.of(EuStore.EU) : List.of();
    }

    private long draw(long available, long voltage, long amperage, boolean simulate, boolean heat) {
        long taken = Math.min(Math.max(amperage, 0L), EuStore.packets(available, voltage));
        if (!simulate && taken > 0L) {
            long left = available - Math.abs(voltage) * taken;
            if (heat) {
                setHeat(left);
            } else {
                setCold(left);
            }
            onEnergyChanged();
        }
        return taken;
    }
}
