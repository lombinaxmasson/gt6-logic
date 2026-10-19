package com.lombinaxmasson.gt6logic.client;

import org.joml.Quaternionf;

import com.lombinaxmasson.gt6logic.block.SignalMeterBlock;
import com.lombinaxmasson.gt6logic.block.SignalMeterBlockEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.PoseStack;

/** Draws the four-decimal level on the face the meter is listening through. */
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
        pose.mulPose(facingOut(facing));
        pose.translate(0.0, 0.0, 0.501);
        float scale = 0.015F;
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

    /** Local +Z points out of the listened face, so the glyphs face the player standing there. */
    private static Quaternionf facingOut(Direction facing) {
        return switch (facing) {
            case SOUTH -> new Quaternionf();
            case NORTH -> new Quaternionf().rotateY((float) Math.PI);
            case EAST -> new Quaternionf().rotateY((float) (-Math.PI / 2.0));
            case WEST -> new Quaternionf().rotateY((float) (Math.PI / 2.0));
            case UP -> new Quaternionf().rotateX((float) (-Math.PI / 2.0));
            case DOWN -> new Quaternionf().rotateX((float) (Math.PI / 2.0));
        };
    }
}
