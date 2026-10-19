package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.block.DelayRelayBlock;
import com.lombinaxmasson.gt6logic.block.DelayRelayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** The delay relay's hand. The lamp on the plate is the output; the hand is the wait. */
public final class DelayRelayRenderer implements BlockEntityRenderer<DelayRelayBlockEntity> {
    public DelayRelayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            DelayRelayBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        DialRenderer.draw(
                entity.getBlockState().getValue(DelayRelayBlock.FACING),
                entity.needleAngle(),
                pose,
                buffer);
    }
}
