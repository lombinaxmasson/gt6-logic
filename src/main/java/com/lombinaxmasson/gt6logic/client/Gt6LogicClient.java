package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlock;
import com.lombinaxmasson.gt6logic.block.CounterBlock;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlock;
import com.lombinaxmasson.gt6logic.block.DigitDisplayBlock;
import com.lombinaxmasson.gt6logic.block.PointerGaugeBlock;
import com.lombinaxmasson.gt6logic.block.SelectorSwitchBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only renderers. The dedicated server never loads this class. */
@EventBusSubscriber(modid = Gt6Logic.MODID, value = Dist.CLIENT)
public final class Gt6LogicClient {
    private Gt6LogicClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SignalMeterBlock.TYPE.get(), SignalMeterRenderer::new);
        event.registerBlockEntityRenderer(DigitDisplayBlock.TYPE.get(), DigitDisplayRenderer::new);
        event.registerBlockEntityRenderer(PointerGaugeBlock.TYPE.get(), PointerGaugeRenderer::new);
        event.registerBlockEntityRenderer(ChartRecorderBlock.TYPE.get(), ChartRecorderRenderer::new);
        event.registerBlockEntityRenderer(DelayRelayBlock.TYPE.get(), DelayRelayRenderer::new);
        event.registerBlockEntityRenderer(CounterBlock.TYPE.get(), CounterRenderer::new);
        event.registerBlockEntityRenderer(SelectorSwitchBlock.TYPE.get(), SelectorSwitchRenderer::new);
    }
}
