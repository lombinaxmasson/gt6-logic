package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.block.DigitDisplayBlockEntity;
import com.lombinaxmasson.gt6logic.block.ReceiverPlate;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Draws the selected unit on the free face. Facing is the wall. */
public final class DigitDisplayRenderer implements BlockEntityRenderer<DigitDisplayBlockEntity> {
    private final Font font;

    public DigitDisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(
            DigitDisplayBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Direction facing = entity.getBlockState().getValue(ReceiverPlate.FACING);
        String text = entity.face();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(PlateTurn.of(facing));
        pose.translate(0.0, 0.0, -0.5 + 2.7 / 16.0);
        float scale = 0.012F;
        pose.scale(scale, -scale, scale);
        float width = font.width(text);
        font.drawInBatch(
                text,
                -width / 2.0F,
                -4.0F,
                0xFFFFE8C8,
                false,
                pose.last().pose(),
                buffer,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
                LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
