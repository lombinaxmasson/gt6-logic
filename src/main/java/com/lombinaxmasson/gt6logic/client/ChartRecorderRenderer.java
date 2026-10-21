package com.lombinaxmasson.gt6logic.client;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.lombinaxmasson.gt6logic.block.ChartRecorderBlockEntity;
import com.lombinaxmasson.gt6logic.block.ReceiverPlate;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** One bar per stored sample, oldest at the left. The samples come from the free face. */
public final class ChartRecorderRenderer implements BlockEntityRenderer<ChartRecorderBlockEntity> {
    private static final ResourceLocation MARK =
            ResourceLocation.fromNamespaceAndPath(Gt6Logic.MODID, "textures/block/needle.png");

    public ChartRecorderRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            ChartRecorderBlockEntity entity,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        int count = entity.bars();
        if (count <= 0) {
            return;
        }
        Direction facing = entity.getBlockState().getValue(ReceiverPlate.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(PlateTurn.of(facing));
        float z = -0.5F + 2.5F / 16F;
        float span = 0.72F;
        float left = -span / 2F;
        float slot = span / count;
        for (int i = 0; i < count; i++) {
            float fraction = entity.bar(i);
            if (fraction <= 0F) {
                continue;
            }
            float x0 = left + i * slot;
            float y0 = -0.28F;
            FaceMark.quad(pose, buffer, MARK, x0, y0, x0 + slot * 0.65F, y0 + fraction * 0.56F, z, 0xFFB7F0C2);
        }
        pose.popPose();
    }
}
