package com.lombinaxmasson.gt6logic.client;

import org.joml.Quaternionf;

import com.lombinaxmasson.gt6logic.Gt6Logic;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Clock hand on the room side of a plate. Angle zero points up; positive sweeps clockwise. */
final class DialRenderer {
    private static final ResourceLocation NEEDLE =
            ResourceLocation.fromNamespaceAndPath(Gt6Logic.MODID, "textures/block/needle.png");
    /** Just in front of the 2px plate, which ends at z = 2/16 in the north-facing model. */
    private static final float Z = -0.5F + 2.35F / 16F;

    private DialRenderer() {}

    static void draw(Direction facing, float angle, PoseStack pose, MultiBufferSource buffer) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(PlateTurn.of(facing));
        pose.mulPose(new Quaternionf().rotateZ(-angle));
        FaceMark.quad(pose, buffer, NEEDLE, -0.018F, 0.02F, 0.018F, 0.26F, Z, 0xFFF3E2C4);
        FaceMark.quad(pose, buffer, NEEDLE, -0.03F, -0.03F, 0.03F, 0.03F, Z, 0xFFF3E2C4);
        pose.popPose();
    }
}
