package com.lombinaxmasson.gt6logic.block;

import com.lombinaxmasson.gt6logic.Gt6Logic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** The tank is a fluid handler on every face, including a side-less query. */
@EventBusSubscriber(modid = Gt6Logic.MODID)
public final class FluidCapabilities {
    private FluidCapabilities() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TankBlock.TYPE.get(), (tank, side) -> tank.fluids());
    }
}
