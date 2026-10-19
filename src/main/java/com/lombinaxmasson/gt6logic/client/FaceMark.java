package com.lombinaxmasson.gt6logic.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A flat mark drawn on the room side of a plate. Coordinates are relative to the block center. */
final class FaceMark {
    private FaceMark() {}

    static void quad(
            PoseStack pose,
            MultiBufferSource buffer,
            ResourceLocation texture,
            float x0,
            float y0,
            float x1,
            float y1,
            float z,
            int color) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f matrix = pose.last().pose();
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        if (alpha == 0) {
            alpha = 0xFF;
        }
        vertex(consumer, matrix, x0, y0, z, 0f, 1f, red, green, blue, alpha);
        vertex(consumer, matrix, x1, y0, z, 1f, 1f, red, green, blue, alpha);
        vertex(consumer, matrix, x1, y1, z, 1f, 0f, red, green, blue, alpha);
        vertex(consumer, matrix, x0, y1, z, 0f, 0f, red, green, blue, alpha);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0f, 0f, 1f);
    }
}
