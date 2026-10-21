package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.block.PointerGaugeBlockEntity;
import com.lombinaxmasson.gt6logic.block.ReceiverPlate;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** The hand is swept by the signal on the free face. Facing is the wall. */
public final class PointerGaugeRenderer implements BlockEntityRenderer<PointerGaugeBlockEntity> {
    public PointerGaugeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PointerGaugeBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Direction facing = entity.getBlockState().getValue(ReceiverPlate.FACING);
        DialRenderer.draw(facing, entity.angle(), pose, buffer);
    }
}
