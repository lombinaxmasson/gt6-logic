package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlockEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.PoseStack;

/** Draws the four-decimal level on the free face. Facing is the wall. */
public final class SignalMeterRenderer implements BlockEntityRenderer<SignalMeterBlockEntity> {
    private final Font font;

    public SignalMeterRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(
            SignalMeterBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Direction facing = entity.getBlockState().getValue(SignalMeterBlock.FACING);
        String text = entity.formatted();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(PlateTurn.of(facing));
        // Room side of the plate. The dark window ends just behind this.
        pose.translate(0.0, 0.0, -0.5 + 2.65 / 16.0);
        float scale = 0.010F;
        pose.scale(scale, -scale, scale);
        float width = font.width(text);
        font.drawInBatch(
                text,
                -width / 2.0F,
                -4.0F,
                0xFF70F5B8,
                false,
                pose.last().pose(),
                buffer,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
                LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
