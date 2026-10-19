package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.block.CounterBlock;
import com.lombinaxmasson.gt6logic.block.CounterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** The counter's hand. It sweeps three quarters of the dial, so empty and full do not look the same. */
public final class CounterRenderer implements BlockEntityRenderer<CounterBlockEntity> {
    public CounterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            CounterBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        DialRenderer.draw(
                entity.getBlockState().getValue(CounterBlock.FACING),
                entity.needleAngle(),
                pose,
                buffer);
    }
}
